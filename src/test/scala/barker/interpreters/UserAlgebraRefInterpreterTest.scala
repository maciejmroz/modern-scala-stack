package barker.interpreters

import barker.BasicSpec
import org.scalatest.freespec.AsyncFreeSpec

class UserAlgebraRefInterpreterTest extends BasicSpec with UserAlgebraBehavior:
  "UserService" - {
    val userAlgebraIO = UserAlgebraRefInterpreter()

    behave like userAlgebraBehavior(userAlgebraIO)
  }
