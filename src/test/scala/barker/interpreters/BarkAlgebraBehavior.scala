package barker.interpreters

import barker.BasicSpec
import barker.algebras.{BarkAlgebra, UserAlgebra}
import barker.entities.{BarkId, BarkNotFound, Name, UserId}
import cats.effect.IO
import cats.syntax.all.*

trait BarkAlgebraBehavior extends BasicSpec:
  private def loginTestUser(userAlgebra: UserAlgebra, name: Name): IO[UserId] =
    for
      accessToken <- userAlgebra.login(name)
      userOpt <- userAlgebra.byAccessToken(accessToken)
      userId <- userOpt.fold(IO.raiseError(new Exception("User not found")))(_.id.pure[IO])
    yield userId

  def barkAlgebraBehavior(userAlgebraIO: IO[UserAlgebra], barkAlgebraIO: IO[BarkAlgebra]): Unit =
    "allows posting and retrieving a bark" in {
      for
        userService <- userAlgebraIO
        barksService <- barkAlgebraIO
        authorId <- loginTestUser(userService, Name("test user"))
        bark <- barksService.post(authorId, "some content")
        allBarks <- barksService.list(authorId)
      yield allBarks.find(_.id == bark.id) shouldBe bark.some
    }

    "allows posting and rebarking a bark" in {
      for
        userService <- userAlgebraIO
        barksService <- barkAlgebraIO
        authorId <- loginTestUser(userService, Name("test user"))
        authorId2 <- loginTestUser(userService, Name("test user2"))
        originalBark <- barksService.post(authorId, "some content")
        newBark <- barksService.rebark(authorId2, originalBark.id, "some added content")
        allBarks <- barksService.list(authorId2)
      yield allBarks.find(_.id == newBark.id) shouldBe newBark.some
    }

    "fails with BarkNotFound when trying to rebark non-existing bark" in {
      for
        userService <- userAlgebraIO
        barksService <- barkAlgebraIO
        authorId <- loginTestUser(userService, Name("test user"))
        _ <- barksService.rebark(authorId, BarkId.random(), "some added content").recover { case BarkNotFound =>
          ()
        }
      yield ()
    }
