package barker.schema

import cats.syntax.all.*
import cats.effect.IO
import caliban.schema.Schema
import caliban.schema.ArgBuilder
import caliban.Value.StringValue
import io.scalaland.chimney.dsl.*
import barker.entities.{AccessToken, BarkId, UserId}
import barker.interpreters.Interpreters
import barker.programs.UserProgram

/** Support for using domain types in GraphQL
  */

//use scalarSchema to have unique type name and comment
given Schema[Any, UserId] =
  Schema
    .scalarSchema[UserId]("AuthorId", "Unique ID".some, None, None, x => StringValue(x.value.toString))
//for parameters we can reuse ArgBuilder for UUID
given ArgBuilder[UserId] = ArgBuilder.uuid.map(UserId.apply)

given Schema[Any, BarkId] =
  Schema
    .scalarSchema[BarkId]("BarkId", "Unique ID".some, None, None, x => StringValue(x.value.toString))

given Schema[Any, AccessToken] =
  Schema
    .scalarSchema[AccessToken]("AccessToken", "Unique ID".some, None, None, x => StringValue(x.value))

/** Actual GraphQL schema definition starts here
  *
  * This project tries to use automatic schema derivation from case classes if possible, just to try how far can you
  * push minimal boilerplate approach without running into limitations. Having said that, for some types/schema setups
  * automatic derivation just doesn't work, and for commonly used types semi-auto derivation is preferred due to
  * compilation time improvements. See Caliban docs for more info.
  */
final case class Bark(id: BarkId, authorId: UserId, content: String)

/** We always need an input case class for queries/mutations so that parameter names used in schema can be inferred
  */
final case class ListBarksInput(authorId: UserId) derives ArgBuilder

final case class Query(barks: ListBarksInput => Fx[List[Bark]], token: Fx[AccessToken])

final case class Mutation(post: String => Fx[Bark])

/** Schema object contains queries, mutations, and subscriptions for the API, together with resolvers. [[Interpreters]]
  * speak [[IO]] so we want to stay inside it as long as possible. Having too much logic in here is likely a code smell
  * any way, we only want to wire existing business logic to GraphQL here.
  */
class BarkerSchema(interpreters: Interpreters):
  // transformInto comes from chimney library which allows easy mapping between similar types
  // Quite useful and intuitive, even if using macro magic, I believe it improves readability.
  private def listBarks(input: ListBarksInput): Fx[List[Bark]] =
    Fx.liftIO(interpreters.bark.list(input.authorId).map(_.map(_.transformInto[Bark])))

  private def token: Fx[AccessToken] =
    for ctx <- Fx.ctx
    yield ctx.accessToken.getOrElse(AccessToken("whatever"))

  private def postBark(content: String): Fx[Bark] =
    Fx { ctx =>
      for
        user <- UserProgram.authenticate(interpreters.user, ctx.accessToken)
        bark <- interpreters.bark.post(user.id, content)
      yield bark.transformInto[Bark]
    }

  lazy val query: Query =
    Query(barks = listBarks, token = token)

  lazy val mutation: Mutation =
    Mutation(post = postBark)
