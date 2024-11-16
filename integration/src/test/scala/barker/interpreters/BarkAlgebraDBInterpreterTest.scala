package barker.interpreters

import cats.effect.IO
import org.scalatest.freespec.AsyncFreeSpec
import barker.DbSpec
import barker.entities.{AccessToken, Bark, BarkId, Likes, Name, Rebarks, User, UserId}

import java.time.Instant
import java.time.temporal.ChronoUnit

class BarkAlgebraDBInterpreterTest extends DbSpec with BarkAlgebraBehavior:
  "BarkAlgebra" - {
    val userAlgebraIO = UserAlgebraDbInterpreter(transactor)
    val barkAlgebraIO = BarkAlgebraDbInterpreter(transactor)

    behave like barkAlgebraBehavior(userAlgebraIO, barkAlgebraIO)
  }

  // We are using Cats Effect support trait so we need to wrap Doobie query checks in IOs
  "BarkAlgebraDbInterpreter queries type checks" - {
    val barkAlgebra = new BarkAlgebraDbInterpreter(transactor)

    "insert bark" in {
      val bark = Bark(
        BarkId.random(),
        UserId.random(),
        "content",
        None,
        Instant.now(),
        Likes(0),
        Rebarks(0)
      )
      IO(check(barkAlgebra.insertBarkQuery(bark)))
    }

    "select bark" in {
      IO(check(barkAlgebra.selectBarkByIdQuery(BarkId.random())))
    }

    "update bark rebarks" in {
      IO(check(barkAlgebra.updateBarkRebarksQuery(BarkId.random(), Rebarks(5))))
    }

    "select barks by user id" in {
      IO(check(barkAlgebra.selectBarksByUserIdQuery(UserId.random())))
    }

  }
