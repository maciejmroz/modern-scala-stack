package barker.graphql

import barker.GraphQLSpec
import barker.entities.{Name, UserId}
import barker.schema.AppContext
import barker.interpreters.Interpreters
import org.scalatest.freespec.AsyncFreeSpec
import io.circe.*
import io.circe.syntax.*
import cats.syntax.all.*

import barker.app.CirceSupport.given

/** For GraphQL testing we mostly focus on schema being what we expect, and perhaps that wiring of schema to business
  * logic makes sense. Actual interpreters should be tested in independently so there isn't much point in indirectly
  * testing them via GraphQL tests.
  */
class BarksTest extends GraphQLSpec:

  val listQuery: String =
    """
      |query ListBarks($authorId: AuthorId!) {
      |  barks {
      |    list(authorId: $authorId) {
      |      id
      |      authorId
      |      content
      |    }
      |  }
      |}
      |""".stripMargin

  val postMutation: String =
    """
      |mutation PostBark($content: String!) {
      |  barks {
      |    post(content: $content) {
      |      id
      |      authorId
      |      content
      |    }
      |  }
      |}
      |""".stripMargin

  val rebarkMutation: String =
    """
      |mutation RebarkBark($sourceBarkId: BarkId!, $addedContent: String!) {
      |  barks {
      |    rebark(sourceBarkId: $sourceBarkId, addedContent: $addedContent) {
      |      id
      |      authorId
      |      content
      |    }
      |  }
      |}
      |""".stripMargin

  "Barks GQL" - {
    "should be possible to call 'list' query" in {
      for
        interpreters <- Interpreters()
        _ <- executeGraphQL(
          listQuery,
          interpreters,
          JsonObject("authorId" -> UserId.random().asJson),
          AppContext(None)
        )
      yield () // we don't really expect a result here
    }

    "should be possible to call 'post' mutation" in {
      for
        interpreters <- Interpreters()
        accessToken <- interpreters.user.login(Name("test user"))
        _ <- executeGraphQL(
          postMutation,
          interpreters,
          JsonObject("content" -> "test content".asJson),
          AppContext(accessToken.some)
        )
      yield ()
    }

    "should be possible to call 'rebark' mutation" in {
      for
        interpreters <- Interpreters()
        accessToken <- interpreters.user.login(Name("test user"))
        user <- interpreters.user.byAccessToken(accessToken)
        sourceBark <- interpreters.bark.post(user.get.id, "test content")
        // ok technically we are reposting our own content which should not be possible, but it's not a real app :)
        _ <- executeGraphQL(
          rebarkMutation,
          interpreters,
          JsonObject("sourceBarkId" -> sourceBark.id.asJson, "addedContent" -> "test added content".asJson),
          AppContext(accessToken.some)
        )
      yield ()
    }

  }
