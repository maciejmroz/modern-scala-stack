package barker.services

import barker.BasicSpec
import cats.syntax.all.*
import org.scalatest.freespec.AsyncFreeSpec
import barker.entities.{Name, UserId}
import cats.effect.IO

trait BarkServiceBehavior extends BasicSpec:
  private def loginTestUser(userService: UserService, name: Name): IO[UserId] =
    for
      accessToken <- userService.login(name)
      userOpt <- userService.byAccessToken(accessToken)
      userId <- userOpt.fold(IO.raiseError(new Exception("User not found")))(_.id.pure[IO])
    yield userId

  def barkServiceBehavior(userServiceIO: IO[UserService], barkServiceIO: IO[BarkService]): Unit =
    "allows posting and retrieving a bark" in {
      for
        userService <- userServiceIO
        barksService <- barkServiceIO
        authorId <- loginTestUser(userService, Name("test user"))
        bark <- barksService.post(authorId, "some content")
        allBarks <- barksService.list(authorId)
      yield allBarks.find(_.id == bark.id) shouldBe bark.some
    }

    "allows posting and rebarking a bark" in {
      for
        userService <- userServiceIO
        barksService <- barkServiceIO
        authorId <- loginTestUser(userService, Name("test user"))
        authorId2 <- loginTestUser(userService, Name("test user2"))
        originalBark <- barksService.post(authorId, "some content")
        newBark <- barksService.rebark(authorId2, originalBark.id, "some added content")
        allBarks <- barksService.list(authorId2)
      yield allBarks.find(_.id == newBark.id) shouldBe newBark.some
    }

class BarkServiceRefTest extends BasicSpec with BarkServiceBehavior:
  "BarkService" - {
    val userServiceIO = UserService()
    val barkServiceIO = BarkService()

    behave like barkServiceBehavior(userServiceIO, barkServiceIO)
  }
