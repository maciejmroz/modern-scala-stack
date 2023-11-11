package barker.services

import cats.effect.IO
import org.scalatest.freespec.AsyncFreeSpec
import barker.DbOnlySpec
import barker.entities.{AccessToken, Name, User, UserId}

class UserServiceDBTest extends DbOnlySpec with UserServiceBehavior:
  "UserService" - {
    val userServiceIO = UserService(transactor)

    behave like userServiceBehavior(userServiceIO)
  }

  // We are using Cats Effect support traits so we need to wrap Doobie query checks in IOs
  "UserService queries type checks" - {
    val userService = new UserServiceDbImpl(transactor)

    "select user by name" in {
      IO(check(userService.selectUserByNameQuery(Name("Joe"))))
    }

    "select user by id" in {
      IO(check(userService.selectUserByIdQuery(UserId.random())))
    }

    "select user by access token" in {
      IO(check(userService.selectUserByAccessTokenQuery(AccessToken.random())))
    }

    "insert user" in {
      IO(check(userService.insertUserQuery(User(UserId.random(), Name("Joe")))))
    }

    "invalidate access token" in {
      IO(check(userService.invalidateAccessTokenQuery(UserId.random())))
    }

    "insert access token" in {
      IO(check(userService.insertAccessTokenQuery(AccessToken.random(), UserId.random())))
    }

  }
