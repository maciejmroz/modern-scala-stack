package barker.services

import cats.effect.IO
import org.scalatest.freespec.AsyncFreeSpec
import barker.DbSpec
import barker.entities.{AccessToken, Name, User, UserId}

class BarkServiceDBTest extends DbSpec with BarkServiceBehavior:
  "BarkService" - {
    val userServiceIO = UserService(transactor)
    val barkServiceIO = BarkService(transactor)

    behave like barkServiceBehavior(userServiceIO, barkServiceIO)
  }

  // We are using Cats Effect support trait so we need to wrap Doobie query checks in IOs
  "BarkService queries type checks" - {
    val barkService = new BarkServiceDbImpl(transactor)

    // TODO
  }
