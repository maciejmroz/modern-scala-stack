package barker.interpreters

import barker.BasicSpec
import org.scalatest.freespec.AsyncFreeSpec

class BarkAlgebraRefInterpreterTest extends BasicSpec with BarkAlgebraBehavior:
  "BarkAlgebra" - {
    val userAlgebraIO = UserAlgebraRefInterpreter()
    val barkAlgebraIO = BarkAlgebraRefInterpreter()

    behave like barkAlgebraBehavior(userAlgebraIO, barkAlgebraIO)
  }
