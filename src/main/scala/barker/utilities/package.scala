package barker.utilities

import cats.MonadThrow
import cats.syntax.all.*

/** Going from F[Option[A]] to F[A] or failed effect is an extremely common pattern in any code base
  */
extension [F[_]: MonadThrow, A](m: F[Option[A]])
  def getOrFailWith(t: Throwable): F[A] =
    m.flatMap:
      case Some(v) => v.pure[F]
      case None    => MonadThrow[F].raiseError(t)

/** This is a testing utility that recovers from specific error turning F with that error back into successful F[Unit].
  * This is useful for testing failure scenarios.
  */
extension [F[_]: MonadThrow, A](m: F[A])
  def expectError(t: Throwable): F[Unit] =
    m.void.recover:
      case `t` => ()
