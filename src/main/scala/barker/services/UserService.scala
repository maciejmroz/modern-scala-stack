package barker.services

import java.util.UUID
import cats.syntax.all.*
import cats.effect.IO
import cats.effect.kernel.Ref

import barker.entities.*

/** This is not real auth in any way, we just generate access token that can be used in subsequent requests and provide
  * a way to fetch user based on access token or user id. Just bare minimum to wire into larger system.
  */
trait UserService:
  /** Creates new user if one doesn't exist. Invalidates any previous access token for the user.
    * @return
    *   access token
    */
  def login(userName: Name): IO[AccessToken]
  def byId(userId: UserId): IO[Option[User]]
  def byAccessToken(accessToken: AccessToken): IO[Option[User]]

/** In-memory impl
  */
private[services] class UserServiceRefImpl(ref: Ref[IO, Map[AccessToken, User]]) extends UserService:

  /** This is private case class that exist only to give names to tuples extracted from Map[AccessToken, User], so we
    * avoid syntax like _._1 which I find less readable.
    */
  private final case class AccessTokenWithUser(accessToken: AccessToken, user: User)

  override def login(userName: Name): IO[AccessToken] = ref.modify { users =>
    val newAccessToken = AccessToken(UUID.randomUUID().toString)
    val accessTokenWithUser = users.find((_, u) => u.name == userName).map(AccessTokenWithUser.apply)
    val usersWithAccessTokenRemoved = accessTokenWithUser.map(_.accessToken) match
      case Some(at) => users - at
      case None     => users
    val newUser = accessTokenWithUser.map(_.user).getOrElse(User(UserId(UUID.randomUUID()), userName))
    (usersWithAccessTokenRemoved + (newAccessToken -> newUser), newAccessToken)
  }

  override def byId(userId: UserId): IO[Option[User]] =
    ref.get.map(_.find(_._2.id == userId).map(_._2))

  /** We are optimizing for fetching by access token with other operations being implemented in most naive way possible.
    */
  override def byAccessToken(accessToken: AccessToken): IO[Option[User]] =
    ref.get.map(_.get(accessToken))


object UserService:
  def apply(): IO[UserService] =
    Ref[IO].of(Map.empty[AccessToken, User]).map(new UserServiceRefImpl(_))
