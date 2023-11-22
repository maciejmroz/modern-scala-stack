package barker.services

import java.util.UUID
import cats.syntax.all.*
import cats.effect.IO
import cats.effect.kernel.Ref
import doobie.{ConnectionIO, Query0, Transactor}
import doobie.implicits.*
import barker.entities.*
import doobie.util.update.Update0

import DoobieMappings.given

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

private[services] class UserServiceDbImpl(xa: Transactor[IO]) extends UserService:
  def selectUserByNameQuery(userName: Name): Query0[User] =
    sql"SELECT user_id, name FROM user_user WHERE name=$userName"
      .query[User]

  private def selectUserByName(userName: Name): ConnectionIO[Option[User]] =
    selectUserByNameQuery(userName).option

  def selectUserByIdQuery(userId: UserId): Query0[User] =
    sql"SELECT user_id, name FROM user_user WHERE user_id=$userId"
      .query[User]

  private def selectUserById(userId: UserId): ConnectionIO[Option[User]] =
    selectUserByIdQuery(userId).option

  def selectUserByAccessTokenQuery(accessToken: AccessToken): Query0[User] =
    sql"""SELECT u.user_id, u.name FROM user_access_token AS a
         INNER JOIN user_user AS u
         ON u.user_id=a.user_id
         WHERE a.access_token=$accessToken"""
      .query[User]

  private def selectUserByAccessToken(accessToken: AccessToken): ConnectionIO[Option[User]] =
    selectUserByAccessTokenQuery(accessToken).option

  def insertUserQuery(user: User): Update0 =
    sql"INSERT INTO user_user(user_id, name) VALUES(${user.id}, ${user.name})".update

  private def insertUser(user: User): ConnectionIO[User] =
    insertUserQuery(user).run.as(user)

  def invalidateAccessTokenQuery(userId: UserId): Update0 =
    sql"DELETE FROM user_access_token WHERE user_id=$userId".update

  private def invalidateAccessToken(userId: UserId): ConnectionIO[Unit] =
    invalidateAccessTokenQuery(userId).run.void

  def insertAccessTokenQuery(accessToken: AccessToken, userId: UserId): Update0 =
    sql"INSERT INTO user_access_token(access_token, user_id) VALUES($accessToken, $userId)".update

  private def insertAccessToken(accessToken: AccessToken, userId: UserId): ConnectionIO[AccessToken] =
    insertAccessTokenQuery(accessToken, userId).run.as(accessToken)

  override def login(userName: Name): IO[AccessToken] =
    (for
      maybeUser <- selectUserByName(userName)
      user <- maybeUser match
        case Some(user) => user.pure[ConnectionIO]
        case None       => insertUser(User(UserId.random(), userName))
      _ <- invalidateAccessToken(user.id)
      accessToken <- insertAccessToken(AccessToken.random(), user.id)
    yield accessToken).transact(xa)

  override def byId(userId: UserId): IO[Option[User]] =
    selectUserById(userId).transact(xa)
  override def byAccessToken(accessToken: AccessToken): IO[Option[User]] =
    selectUserByAccessToken(accessToken).transact(xa)

object UserService:
  def apply(): IO[UserService] =
    Ref[IO].of(Map.empty[AccessToken, User]).map(new UserServiceRefImpl(_))
  def apply(xa: Transactor[IO]): IO[UserService] =
    IO(new UserServiceDbImpl(xa))
