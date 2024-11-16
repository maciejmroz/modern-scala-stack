package barker.graphql

import barker.GraphQLSpec
import barker.schema.AppContext
import barker.interpreters.Interpreters
import org.scalatest.freespec.AsyncFreeSpec
import io.circe.*
import io.circe.syntax.*

class UserTest extends GraphQLSpec:

  val loginMutation: String =
    """
      |mutation LoginUser($userName: Name!) {
      |  user {
      |    login(userName: $userName)
      |  }
      |}
      |""".stripMargin

  "User GQL" - {
    "should be possible to call 'login' mutation" in {
      for
        interpreters <- Interpreters()
        _ <- executeGraphQL(
          loginMutation,
          interpreters,
          JsonObject("userName" -> "test user".asJson),
          AppContext(None)
        )
      yield ()
    }
  }
