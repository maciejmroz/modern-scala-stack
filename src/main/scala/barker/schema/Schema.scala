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
given ArgBuilder[BarkId] = ArgBuilder.uuid.map(BarkId.apply)

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

final case class BarksQuery(list: ListBarksInput => Fx[List[Bark]])

/** Main query object. 'token' query is not related to anything in the domain, it is just meant to validate that access
  * token is read correctly and available to GraphQL resolvers (which is useful as a sanity check)
  */
final case class Query(barks: BarksQuery, token: Fx[AccessToken])

final case class PostBarkInput(content: String) derives ArgBuilder
final case class RebarkInput(sourceBarkId: BarkId, addedContent: Option[String]) derives ArgBuilder

final case class BarksMutation(post: PostBarkInput => Fx[Bark], rebark: RebarkInput => Fx[Bark])

/** Main mutation object for the schema
  */
final case class Mutation(barks: BarksMutation)

/** Schema object contains queries, mutations, and subscriptions for the API, together with resolvers. [[Interpreters]]
  * speak [[IO]] so we want to stay inside it as long as possible. Having too much logic in here is likely a code smell
  * any way, we only want to wire existing business logic to GraphQL here. As things scale, you'll probably want to move
  * resolvers out of the schema definition.
  *
  * TODO: user queries and mutations
  */
class BarkerSchema(interpreters: Interpreters):
  // this is resolver for token query which is not really related to anything
  private def token: Fx[AccessToken] =
    for ctx <- Fx.ctx
    yield ctx.accessToken.getOrElse(AccessToken("whatever"))

  // transformInto comes from chimney library which allows easy mapping between similar types
  // Even if using macro magic, I believe it improves readability.
  private def listBarks(input: ListBarksInput): Fx[List[Bark]] =
    Fx.liftIO(interpreters.bark.list(input.authorId).map(_.map(_.transformInto[Bark])))

  private def postBark(postBarkInput: PostBarkInput): Fx[Bark] =
    Fx: ctx =>
      for
        user <- UserProgram.authenticate(interpreters.user, ctx.accessToken)
        bark <- interpreters.bark.post(user.id, postBarkInput.content)
      yield bark.transformInto[Bark]

  private def rebarkBark(rebarkInput: RebarkInput): Fx[Bark] =
    Fx: ctx =>
      for
        user <- UserProgram.authenticate(interpreters.user, ctx.accessToken)
        bark <- interpreters.bark.rebark(user.id, rebarkInput.sourceBarkId, rebarkInput.addedContent.getOrElse(""))
      yield bark.transformInto[Bark]

  lazy val query: Query =
    Query(barks = BarksQuery(listBarks), token = token)

  lazy val mutation: Mutation =
    Mutation(barks = BarksMutation(post = postBark, rebark = rebarkBark))
