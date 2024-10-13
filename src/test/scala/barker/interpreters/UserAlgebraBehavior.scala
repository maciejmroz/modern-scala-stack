package barker.interpreters

import barker.BasicSpec
import barker.algebras.UserAlgebra
import barker.entities.{AccessToken, Name}
import cats.effect.IO

trait UserAlgebraBehavior extends BasicSpec:
  def userAlgebraBehavior(userAlgebraIO: IO[UserAlgebra]): Unit =
    "does not return user when passed invalid token" in {
      for
        userService <- userAlgebraIO
        optUser <- userService.byAccessToken(AccessToken.random())
      yield optUser shouldBe None
    }

    "allows user to log in" in {
      for
        userService <- userAlgebraIO
        token <- userService.login(Name("Joe"))
        user <- userService.byAccessToken(token)
      yield user should not be empty
    }

    "invalidates old access token on login" in {
      for
        userService <- userAlgebraIO
        token1 <- userService.login(Name("Joe"))
        token2 <- userService.login(Name("Joe"))
        user1 <- userService.byAccessToken(token1)
        user2 <- userService.byAccessToken(token2)
      yield
        user1 shouldBe empty
        user2 shouldBe defined
        token1 should not be token2
    }
