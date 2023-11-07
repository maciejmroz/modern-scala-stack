package barker.services

import cats.syntax.all.*
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec
import barker.entities.UserId
import java.util.UUID

class BarkServiceTest extends AsyncFreeSpec with AsyncIOSpec with Matchers:
  "BarkService" - {
    val barksServiceIO = BarkService()
    val authorId = UserId(UUID.randomUUID())
    val authorId2 = UserId(UUID.randomUUID())

    "allows posting and retrieving a bark" in {
      for
        barksService <- barksServiceIO
        bark <- barksService.post(authorId, "some content")
        allBarks <- barksService.list(authorId)
      yield allBarks.find(_.id == bark.id) shouldBe bark.some
    }

    "allows posting and rebarking a bark" in {
      for
        barksService <- barksServiceIO
        originalBark <- barksService.post(authorId, "some content")
        newBark <- barksService.rebark(authorId2, originalBark.id, "some added content")
        allBarks <- barksService.list(authorId2)
      yield allBarks.find(_.id == newBark.id) shouldBe newBark.some
    }

  }
