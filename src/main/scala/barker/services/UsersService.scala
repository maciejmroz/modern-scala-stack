package barker.services

import java.util.UUID
import java.time.ZonedDateTime
import cats.syntax.all.*
import cats.effect.IO
import cats.effect.kernel.Ref

import barker.entities.*

/** This is not real auth in any way, we just generate access token that can be used in subsequent requests and provide
  * a way to fetch user based on access token or user id. Just bare minimum to wire into larger system.
  */
trait UsersService:
  /** Creates new user if one doesn't exist. Invalidates any previous access token for the user.
    * @return
    *   access token
    */
  def login(userName: Name): IO[AccessToken]
  def byId(userId: UserId): IO[Option[User]]
  def byAccessToken(accessToken: AccessToken): IO[Option[User]]

/** In-memory impl
  */
private[services] class UsersServiceRefImpl(ref: Ref[IO, Map[AccessToken, User]]) extends UsersService:
  override def login(userName: Name): IO[AccessToken] = ref.modify { users =>
    user
  }

  override def byId(userId: UserId): IO[Option[User]] = ???

  override def byAccessToken(accessToken: AccessToken): IO[Option[User]] = ???
