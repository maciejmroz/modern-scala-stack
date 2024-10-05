package barker.services

import cats.effect.IO
import org.scalatest.freespec.AsyncFreeSpec
import barker.DbSpec
import barker.entities.{AccessToken, Bark, BarkId, Likes, Name, Rebarks, User, UserId}

import java.time.Instant
import java.time.temporal.ChronoUnit

class BarkServiceDBTest extends DbSpec with BarkServiceBehavior:
  "BarkService" - {
    val userServiceIO = UserService(transactor)
    val barkServiceIO = BarkService(transactor)

    behave like barkServiceBehavior(userServiceIO, barkServiceIO)
  }

  // We are using Cats Effect support trait so we need to wrap Doobie query checks in IOs
  "BarkService queries type checks" - {
    val barkService = new BarkServiceDbImpl(transactor)

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
      IO(check(barkService.insertBarkQuery(bark)))
    }

    "select bark" in {
      IO(check(barkService.selectBarkByIdQuery(BarkId.random())))
    }

    "update bark rebarks" in {
      IO(check(barkService.updateBarkRebarksQuery(BarkId.random(), Rebarks(5))))
    }

    "select barks by user id" in {
      IO(check(barkService.selectBarksByUserIdQuery(UserId.random())))
    }

  }
