package barker.interpreters

import cats.effect.IO
import org.scalatest.freespec.AsyncFreeSpec
import barker.DbSpec
import barker.entities.{AccessToken, Name, User, UserId}

class UserAlgebraDBInterpreterTest extends DbSpec with UserAlgebraBehavior:
  "UserAlgebra" - {
    val userAlgebraIO = UserAlgebraDbInterpreter(transactor)

    behave like userAlgebraBehavior(userAlgebraIO)
  }

  // We are using Cats Effect support trait so we need to wrap Doobie query checks in IOs
  "UserAlgebra queries type checks" - {
    val userAlgebra = new UserAlgebraDbInterpreter(transactor)

    "select user by name" in {
      IO(check(userAlgebra.selectUserByNameQuery(Name("Joe"))))
    }

    "select user by id" in {
      IO(check(userAlgebra.selectUserByIdQuery(UserId.random())))
    }

    "select user by access token" in {
      IO(check(userAlgebra.selectUserByAccessTokenQuery(AccessToken.random())))
    }

    "insert user" in {
      IO(check(userAlgebra.insertUserQuery(User(UserId.random(), Name("Joe")))))
    }

    "invalidate access token" in {
      IO(check(userAlgebra.invalidateAccessTokenQuery(UserId.random())))
    }

    "insert access token" in {
      IO(check(userAlgebra.insertAccessTokenQuery(AccessToken.random(), UserId.random())))
    }

  }
