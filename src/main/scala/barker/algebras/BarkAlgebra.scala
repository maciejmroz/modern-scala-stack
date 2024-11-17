package barker.algebras

import barker.entities.{Bark, BarkId, UserId}
import cats.effect.IO

/** Algebra definitions
  *
  * Why fix everything on IO effect and inject dependencies via constructor parameters? Because it is extremely simple
  * to grasp.
  *
  * While I get that having tagless final backed by something like Kleisli has several advantages it also has
  * significant barrier to entry, so what I am trying here is answering a question: what if we just don't do it?
  */

trait BarkAlgebra:
  def post(author: UserId, content: String): IO[Bark]
  def rebark(author: UserId, sourceBarkId: BarkId, addedContent: String): IO[Bark]
  def list(author: UserId): IO[List[Bark]]
