package barker.interpreters

import barker.algebras.UserAlgebra
import barker.entities.{AccessToken, Name, User, UserId}
import cats.effect.IO
import cats.effect.kernel.Ref

import java.util.UUID

/** In-memory impl
  */
private[interpreters] class UserAlgebraRefInterpreter(ref: Ref[IO, Map[AccessToken, User]]) extends UserAlgebra:

  override def login(userName: Name): IO[AccessToken] = ref.modify { users =>
    val newAccessToken = AccessToken(UUID.randomUUID().toString)
    val accessTokenWithUser = users.find((_, u) => u.name == userName)
    val usersWithAccessTokenRemoved = accessTokenWithUser.map((accessToken, _) => accessToken) match
      case Some(at) => users - at
      case None     => users
    val newUser = accessTokenWithUser.map((_, user) => user).getOrElse(User(UserId(UUID.randomUUID()), userName))
    (usersWithAccessTokenRemoved + (newAccessToken -> newUser), newAccessToken)
  }

  override def byId(userId: UserId): IO[Option[User]] =
    ref.get.map(_.map((_, user) => user).find(_.id == userId))

  /** We are optimizing for fetching by access token with other operations being implemented in most naive way possible.
    */
  override def byAccessToken(accessToken: AccessToken): IO[Option[User]] =
    ref.get.map(_.get(accessToken))

object UserAlgebraRefInterpreter:
  def apply(): IO[UserAlgebra] =
    Ref[IO].of(Map.empty[AccessToken, User]).map(new UserAlgebraRefInterpreter(_))
