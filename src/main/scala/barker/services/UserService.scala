package barker.services

import java.util.UUID
import scala.util.Try
import cats.syntax.all.*
import cats.effect.IO
import cats.effect.kernel.Ref
import doobie.{ConnectionIO, Transactor}
import doobie.implicits.*
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

private[services] class UserServiceDbImpl(xa: Transactor[IO]) extends UserService:
  private final case class UserDAO(userId: String, name: String)

  // This is pretty ugly - need validation + error logging in real world implementation!
  private def userFromDAO(u: UserDAO): Option[User] =
    Try(UUID.fromString(u.userId)).toOption.map { uuid =>
      User(UserId(uuid), Name(u.name))
    }

  private def selectUserByName(userName: Name): ConnectionIO[Option[User]] =
    sql"SELECT userId, name FROM user_user WHERE name=${userName.value}"
      .query[UserDAO]
      .option
      .map(_.flatMap(userFromDAO))

  private def selectUserById(userId: UserId): ConnectionIO[Option[User]] =
    sql"SELECT userId, name FROM user_user WHERE userId=${userId.value.toString}"
      .query[UserDAO]
      .option
      .map(_.flatMap(userFromDAO))

  private def selectUserByAccessToken(accessToken: AccessToken): ConnectionIO[Option[User]] =
    sql"""SELECT u.userId, u.name FROM user_access_token AS a
         INNER JOIN user_user AS u
         ON u.userId=a.userId
         WHERE a.accesstoken=${accessToken.value.toString}"""
      .query[UserDAO]
      .option
      .map(_.flatMap(userFromDAO))

  private def insertUser(user: User): ConnectionIO[User] =
    sql"INSERT INTO user_user(userId, name) VALUES(${user.id.value.toString}, ${user.name.value})".update.run
      .as(user)

  private def invalidateAccessToken(userId: UserId): ConnectionIO[Unit] =
    sql"DELETE FROM user_access_token WHERE userId=${userId.value.toString}".update.run.void

  private def insertAccessToken(accessToken: AccessToken, userId: UserId): ConnectionIO[AccessToken] =
    sql"INSERT INTO user_access_token(accesstoken, userId) VALUES(${accessToken.value}, ${userId.value.toString})".update.run
      .as(accessToken)

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
