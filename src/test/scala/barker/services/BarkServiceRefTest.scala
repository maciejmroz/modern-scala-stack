package barker.services

import barker.BasicSpec
import cats.syntax.all.*
import org.scalatest.freespec.AsyncFreeSpec
import barker.entities.UserId
import cats.effect.IO

import java.util.UUID

trait BarkServiceBehavior extends BasicSpec:
  def barkServiceBehavior(barkServiceIO: IO[BarkService]): Unit =
    "allows posting and retrieving a bark" in {
      val authorId = UserId(UUID.randomUUID())
      for
        barksService <- barkServiceIO
        bark <- barksService.post(authorId, "some content")
        allBarks <- barksService.list(authorId)
      yield allBarks.find(_.id == bark.id) shouldBe bark.some
    }

    "allows posting and rebarking a bark" in {
      val authorId = UserId(UUID.randomUUID())
      val authorId2 = UserId(UUID.randomUUID())
      for
        barksService <- barkServiceIO
        originalBark <- barksService.post(authorId, "some content")
        newBark <- barksService.rebark(authorId2, originalBark.id, "some added content")
        allBarks <- barksService.list(authorId2)
      yield allBarks.find(_.id == newBark.id) shouldBe newBark.some
    }

class BarkServiceRefTest extends BasicSpec with BarkServiceBehavior:
  "BarkService" - {
    val barkServiceIO = BarkService()

    behave like barkServiceBehavior(barkServiceIO)
  }
