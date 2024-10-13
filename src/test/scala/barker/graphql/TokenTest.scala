package barker.graphql

import barker.GraphQLSpec
import barker.entities.AccessToken
import barker.schema.AppContext
import barker.interpreters.Interpreters
import org.scalatest.freespec.AsyncFreeSpec
import cats.syntax.all.*
import io.circe.*
import io.circe.parser.*
import io.circe.optics.JsonPath.*

/** This is a "demo test" with no real purpose (as token query doesn't really do anything) other than verifying that
  * things work correctly at a basic level of HTTP middleware and injecting request data to GraphQL resolvers.
  */
class TokenTest extends GraphQLSpec:
  
  // Minimal valid query can just be written as "{ token }" but let's be a bit more realistic here
  val tokenQuery: String =
    """
      |query GetToken() {
      | token
      |}
      |""".stripMargin
    
  "token" - {
    "should reflect access token to caller" in {
      for
        // Not really used, only needed to instantiate schema
        services <- Interpreters()
        randomToken = AccessToken.random()
        result <- executeGraphQL(tokenQuery, services, AppContext(randomToken.some))
      yield {
        val json = parse(result.toResponseValue.toString).getOrElse(Json.Null)
        root.data.token.string.getOption(json) shouldBe randomToken.some
      }
    }
  }
