package barker.interpreters

import barker.BasicSpec
import barker.algebras.{BarkAlgebra, UserAlgebra}
import barker.entities.{BarkId, BarkNotFound, Name, UserId}
import barker.utilities.*
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
        userAlgebra <- userAlgebraIO
        barksAlgebra <- barkAlgebraIO
        authorId <- loginTestUser(userAlgebra, Name("test user"))
        bark <- barksAlgebra.post(authorId, "some content")
        allBarks <- barksAlgebra.list(authorId)
      yield allBarks.find(_.id == bark.id) shouldBe bark.some
    }

    "allows posting and rebarking a bark" in {
      for
        userAlgebra <- userAlgebraIO
        barksAlgebra <- barkAlgebraIO
        authorId <- loginTestUser(userAlgebra, Name("test user"))
        authorId2 <- loginTestUser(userAlgebra, Name("test user2"))
        originalBark <- barksAlgebra.post(authorId, "some content")
        newBark <- barksAlgebra.rebark(authorId2, originalBark.id, "some added content")
        allBarks <- barksAlgebra.list(authorId2)
      yield allBarks.find(_.id == newBark.id) shouldBe newBark.some
    }

    "fails with BarkNotFound when trying to rebark non-existing bark" in {
      for
        userAlgebra <- userAlgebraIO
        barkAlgebra <- barkAlgebraIO
        authorId <- loginTestUser(userAlgebra, Name("test user"))
        _ <- barkAlgebra.rebark(authorId, BarkId.random(), "some added content").expectError(BarkNotFound)
      yield ()
    }
