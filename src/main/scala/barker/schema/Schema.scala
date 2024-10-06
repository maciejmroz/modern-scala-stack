package barker.schema

import cats.syntax.all.*
import cats.effect.IO
import caliban.schema.Schema
import caliban.schema.ArgBuilder
import caliban.Value.StringValue
import io.scalaland.chimney.dsl.*
import barker.entities.{AccessToken, BarkId, InvalidAccessToken, User, UserId, UserNotFound}
import barker.services.Services

/** Support for domain types
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

final case class Query(barks: UserId => Fx[List[Bark]], token: Fx[AccessToken])

final case class Mutation(post: String => Fx[Bark])

/** Schema object contains queries, mutations, and subscriptions for the API, together with resolvers. [[Services]]
  * speak [[IO]] so we want to stay inside it as long as possible. Having too much logic in here is likely a code smell
  * any way, we only want to wire existing business logic to GraphQL here.
  */
class BarkerSchema(services: Services):
  // transformInto comes from chimney library which allows easy mapping between similar types
  // Quite useful and intuitive, even if using macro magic, I believe it improves readability.
  private def listBarks(authorId: UserId): Fx[List[Bark]] =
    Fx.liftIO(services.barkService.list(authorId).map(_.map(_.transformInto[Bark])))

  private def token: Fx[AccessToken] =
    for ctx <- Fx.ctx
    yield ctx.accessToken.getOrElse(AccessToken("whatever"))

  // utility method, doesn't really belong here
  private def requireUser(accessTokenOpt: Option[AccessToken]): IO[User] =
    for
      userOpt <- accessTokenOpt match
        case Some(accessToken) => services.userService.byAccessToken(accessToken)
        case None              => IO.raiseError(InvalidAccessToken)
      user <- userOpt match
        case Some(u) => u.pure[IO]
        case None    => IO.raiseError(UserNotFound)
    yield user

  private def postBark(content: String): Fx[Bark] =
    for
      ctx <- Fx.ctx
      user <- Fx.liftIO(requireUser(ctx.accessToken))
      newBark <- Fx.liftIO(services.barkService.post(user.id, content))
    yield newBark.transformInto[Bark]

  lazy val query: Query =
    Query(barks = listBarks, token = token)

  lazy val mutation: Mutation =
    Mutation(post = postBark)
