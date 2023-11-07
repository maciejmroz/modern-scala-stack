package barker.schema

import cats.syntax.all.*
import cats.effect.IO
import caliban.schema.Schema
import caliban.schema.ArgBuilder
import caliban.Value.StringValue
import io.scalaland.chimney.dsl.*
import barker.entities.{BarkId, UserId}
import barker.services.{BarkService, Services}
import cats.data.Kleisli

/** Request context is needed to pass information from HTTP request to the resolver. It forces us to use
  * [[cats.data.Kleisli]] in the resolvers, and lifting [[cats.effect.IO]] returned from services into Kleisli. This
  * might be somewhat unintuitive to Scala newcomers, but is not really a huge deal, and hopefully we can contain this
  * complexity to HTTP/GraphQL wiring only.
  */
final case class RequestContext(accessToken: Option[String])
type RequestIO[A] = Kleisli[IO, RequestContext, A]

object RequestIO:
  def ctx: RequestIO[RequestContext] =
    Kleisli { (ctx: RequestContext) => IO.pure(ctx) }

  def liftIO[A](io: IO[A]): RequestIO[A] = Kleisli { _ => io }

/** Support for custom types
  *
  * Interesting problem: should you expose your core domain types in the API? While I've seen strict "no" from some,
  * exposing ID types directly is just a pragmatic choice - it is _very_ unlikely to ever cause problems.
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

/** Actual GraphQL schema definition starts here
  *
  * This project tries to use automatic schema derivation from case classes if possible, just to try how far can you
  * push minimal boilerplate approach without running into limitations. Having said that, for some types/schema setups
  * automatic derivation just doesn't work, and for commonly used types semi-auto derivation is preferred due to
  * compilation time improvements. See Caliban docs for more info.
  */
final case class Bark(id: BarkId, authorId: UserId, content: String)

final case class Query(barks: UserId => RequestIO[List[Bark]], token: RequestIO[String])

/** Schema object contains queries, mutations, and subscriptions for the API, as well as resolvers (these should really
  * just map domain model to API types)
  */
class BarkerSchema(services: Services):
  // transformInto comes from chimney library which allows easy mapping between similar types
  // Quite useful and intuitive, even if using macro magic, I believe it improves readability.
  private def listBarks(authorId: UserId): RequestIO[List[Bark]] =
    RequestIO.liftIO(services.barkService.list(authorId).map(_.map(_.transformInto[Bark])))

  private def token: RequestIO[String] =
    for ctx <- RequestIO.ctx
    yield ctx.accessToken.getOrElse("whatever")

  lazy val query: Query =
    Query(barks = listBarks, token = token)
