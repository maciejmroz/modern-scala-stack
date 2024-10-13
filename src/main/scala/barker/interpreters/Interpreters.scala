package barker.interpreters

import barker.algebras.{BarkAlgebra, UserAlgebra}
import cats.effect.IO
import doobie.Transactor

/** Just group all services in single place - enough for simple case
  */
final case class Interpreters(bark: BarkAlgebra, user: UserAlgebra)

object Interpreters:
  def apply(): IO[Interpreters] =
    for
      user <- UserAlgebraRefInterpreter()
      bark <- BarkAlgebraRefInterpreter()
    yield Interpreters(bark, user)

  def apply(xa: Transactor[IO]): IO[Interpreters] =
    for
      user <- UserAlgebraDbInterpreter(xa)
      bark <- BarkAlgebraDbInterpreter(xa)
    yield Interpreters(bark, user)
