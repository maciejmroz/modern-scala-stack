package barker.programs

import cats.effect.IO
import cats.syntax.all.*

import barker.entities.*
import barker.algebras.UserAlgebra

object UserProgram:
  /** Given optional AccessToken, either yield User or raise error. Obviously raises error if None is provided.
    */
  def authenticate(userAlg: UserAlgebra, accessTokenOpt: Option[AccessToken]): IO[User] =
    for
      userOpt <- accessTokenOpt match
        case Some(accessToken) => userAlg.byAccessToken(accessToken)
        case None              => IO.raiseError(InvalidAccessToken)
      user <- userOpt match
        case Some(u) => u.pure[IO]
        case None    => IO.raiseError(UserNotFound)
    yield user
