package barker.interpreters

import cats.syntax.all.*
import cats.effect.IO
import doobie.{ConnectionIO, Query0, Transactor}
import doobie.implicits.*
import barker.entities.*
import doobie.util.update.Update0
import DoobieMappings.given
import barker.algebras.UserAlgebra

private[interpreters] class UserAlgebraDbInterpreter(xa: Transactor[IO]) extends UserAlgebra:
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

object UserAlgebraDbInterpreter:
  def apply(xa: Transactor[IO]): IO[UserAlgebra] =
    IO(new UserAlgebraDbInterpreter(xa))
