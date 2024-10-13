package barker.algebras

import barker.entities.{AccessToken, Name, User, UserId}
import cats.effect.IO

/** This is not real auth in any way, we just generate access token that can be used in subsequent requests and provide
  * a way to fetch user based on access token or user id. Just bare minimum to wire into larger system.
  */
trait UserAlgebra:
  /** Creates new user if one doesn't exist. Invalidates any previous access token for the user.
    * @return
    *   access token
    */
  def login(userName: Name): IO[AccessToken]
  def byId(userId: UserId): IO[Option[User]]
  def byAccessToken(accessToken: AccessToken): IO[Option[User]]
