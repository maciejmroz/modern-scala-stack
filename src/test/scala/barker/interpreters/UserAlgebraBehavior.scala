package barker.interpreters

import barker.BasicSpec
import barker.algebras.UserAlgebra
import barker.entities.{AccessToken, Name}
import cats.effect.IO

trait UserAlgebraBehavior extends BasicSpec:
  def userAlgebraBehavior(userAlgebraIO: IO[UserAlgebra]): Unit =
    "does not return user when passed invalid token" in {
      for
        userAlgebra <- userAlgebraIO
        optUser <- userAlgebra.byAccessToken(AccessToken.random())
      yield optUser shouldBe None
    }

    "allows user to log in" in {
      for
        userAlgebra <- userAlgebraIO
        token <- userAlgebra.login(Name("Joe"))
        user <- userAlgebra.byAccessToken(token)
      yield user should not be empty
    }

    "invalidates old access token on login" in {
      for
        userAlgebra <- userAlgebraIO
        token1 <- userAlgebra.login(Name("Joe"))
        token2 <- userAlgebra.login(Name("Joe"))
        user1 <- userAlgebra.byAccessToken(token1)
        user2 <- userAlgebra.byAccessToken(token2)
      yield
        user1 shouldBe empty
        user2 shouldBe defined
        token1 should not be token2
    }
