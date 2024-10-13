package barker.interpreters

import barker.algebras.{BarkAlgebra, UserAlgebra}
import cats.effect.IO
import doobie.Transactor

/** Just group all services in single place - enough for simple case
  */
final case class AllInterpreters(bark: BarkAlgebra, user: UserAlgebra)

object AllInterpreters:
  def apply(): IO[AllInterpreters] =
    for
      user <- UserAlgebraRefInterpreter()
      bark <- BarkAlgebraRefInterpreter()
    yield AllInterpreters(bark, user)

  def apply(xa: Transactor[IO]): IO[AllInterpreters] =
    for
      user <- UserAlgebraDbInterpreter(xa)
      bark <- BarkAlgebraDbInterpreter(xa)
    yield AllInterpreters(bark, user)
