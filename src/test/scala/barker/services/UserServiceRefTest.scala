package barker.services

import barker.UnitSpec
import org.scalatest.freespec.AsyncFreeSpec
import barker.entities.{AccessToken, Name}
import cats.effect.IO

trait UserServiceBehavior extends UnitSpec:
  def userServiceBehavior(userServiceIO: IO[UserService]): Unit =
    "does not return user when passed invalid token" in {
      for
        userService <- userServiceIO
        optUser <- userService.byAccessToken(AccessToken.random())
      yield optUser shouldBe None
    }

    "allows user to log in" in {
      for
        userService <- userServiceIO
        token <- userService.login(Name("Joe"))
        user <- userService.byAccessToken(token)
      yield user should not be empty
    }

    "invalidates old access token on login" in {
      for
        userService <- userServiceIO
        token1 <- userService.login(Name("Joe"))
        token2 <- userService.login(Name("Joe"))
        user1 <- userService.byAccessToken(token1)
        user2 <- userService.byAccessToken(token2)
      yield
        user1 shouldBe empty
        user2 shouldBe defined
        token1 should not be token2
    }

class UserServiceRefTest extends UnitSpec with UserServiceBehavior:
  "UserService" - {
    val userServiceIO = UserService()

    behave like userServiceBehavior(userServiceIO)
  }
