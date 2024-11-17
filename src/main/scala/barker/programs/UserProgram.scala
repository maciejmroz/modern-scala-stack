package barker.programs

import cats.effect.IO

import barker.entities.*
import barker.algebras.UserAlgebra
import barker.utilities.*

object UserProgram:
  /** Given optional AccessToken, either yield User or raise error. Obviously raises error if None is provided.
    */
  def authenticate(userAlg: UserAlgebra, accessTokenOpt: Option[AccessToken]): IO[User] =
    accessTokenOpt match
      case Some(accessToken) => userAlg.byAccessToken(accessToken).getOrFailWith(UserNotFound)
      case None              => IO.raiseError(InvalidAccessToken)
