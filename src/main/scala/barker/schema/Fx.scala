package barker.schema

import cats.syntax.all.*
import cats.effect.IO
import cats.data.Kleisli
import cats.~>

/** App context is needed to pass information from HTTP request to the resolver. It forces us to use
 * [[cats.data.Kleisli]] in the resolvers, and lifting [[cats.effect.IO]] returned from services into Kleisli. This
 * might be somewhat unintuitive to Scala newcomers, but is not really a huge deal, and hopefully we can contain this
 * complexity to HTTP/GraphQL wiring only.
 */
final case class AppContext(accessToken: Option[String])
type Fx[A] = Kleisli[IO, AppContext, A]

object Fx:
  def ctx: Fx[AppContext] =
    Kleisli { (ctx: AppContext) => IO.pure(ctx) }

  def liftIO[A](io: IO[A]): Fx[A] = Kleisli { _ => io }

  def liftK: IO ~> Fx = Kleisli.liftK[IO, AppContext]
