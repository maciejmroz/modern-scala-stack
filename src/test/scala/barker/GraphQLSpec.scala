package barker

import barker.app.GraphQLRoutes
import barker.schema.{AppContext, Fx}
import barker.interpreters.Interpreters
import caliban.interop.cats.{CatsInterop, InjectEnv}
import caliban.{CalibanError, GraphQLResponse}
import cats.effect.*
import cats.effect.std.Dispatcher
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.{Json, JsonObject}
import org.scalatest.Suite
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import zio.{Runtime, ZEnvironment}

/** Test spec that can run GraphQL query.
  *
  * This is where we have a bit of a downside to automatic schema derivation as we need to derive it for every single
  * test. Doesn't matter for toy scenarios but won't scale well into large schemas combined with 1000s of tests ...
  */
trait GraphQLSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers:
  self: Suite =>

  def executeGraphQL(
      query: String,
      interpreters: Interpreters,
      variables: JsonObject = JsonObject(),
      ctx: AppContext = AppContext(None)
  ): IO[GraphQLResponse[CalibanError]] =
    given runtime: Runtime[AppContext] = Runtime.default.withEnvironment(ZEnvironment(AppContext(None)))
    given injector: InjectEnv[Fx, AppContext] = InjectEnv.kleisli

    // Use Circe decoder defined in Caliban to map from JsonObject to Map[String, InputValue]
    val circeInputDecoder = caliban.InputValue.circeDecoder
    val calibanVariables = variables.toMap
      .map { case (str, json) => str -> circeInputDecoder.decodeJson(json) }
      .collect { case (str, Right(i)) => str -> i }

    Dispatcher
      .parallel[Fx]
      .use { dispatcher =>
        given interop: CatsInterop.Contextual[Fx, AppContext] =
          CatsInterop.contextual[Fx, AppContext](
            dispatcher
          )
        GraphQLRoutes
          .makeInterpreter(interpreters)
          .flatMap(it => interop.toEffect(it.execute(query, variables = calibanVariables)))
      }
      .run(ctx)
