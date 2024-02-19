package barker

import barker.schema.{RequestContext, RequestIO}
import barker.services.Services
import caliban.{CalibanError, GraphQLResponse}
import caliban.interop.cats.{CatsInterop, InjectEnv}
import cats.effect.*
import cats.effect.std.Dispatcher
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.Suite
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import zio.{Runtime, ZEnvironment}

/** Test spec that can run GraphQL query.
  *
  * This is where we have a bit of a downside to automatic schema derivation as we need to derive it for every single
  * test. Doesn't matter for toy scenarios but won't scale well.
  */
trait GraphQLSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers:
  self: Suite =>

  // TODO: this API makes it impossible to set up any state
  def executeQuery(
      query: String,
      services: IO[Services] = Services(),
      ctx: RequestContext = RequestContext(None)
  ): IO[GraphQLResponse[CalibanError]] =
    given runtime: Runtime[RequestContext] = Runtime.default.withEnvironment(ZEnvironment(RequestContext(None)))
    given injector: InjectEnv[RequestIO, RequestContext] = InjectEnv.kleisli

    Dispatcher
      .parallel[RequestIO]
      .use { dispatcher =>
        given interop: CatsInterop.Contextual[RequestIO, RequestContext] =
          CatsInterop.contextual[RequestIO, RequestContext](
            dispatcher
          )
        GraphQLInit.makeInterpreter(services).flatMap(it => interop.toEffect(it.execute(query)))
      }
      .run(ctx)
