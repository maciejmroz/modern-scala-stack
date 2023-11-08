package barker

import caliban.interop.cats.{CatsInterop, InjectEnv, ToEffect}
import cats.effect.*
import cats.effect.std.Dispatcher
import cats.data.Kleisli
import cats.data.OptionT
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.ember.server.*
import org.http4s.server.Router
import com.comcast.ip4s.*
import caliban.*
import caliban.interop.cats.implicits.*
import caliban.interop.tapir.HttpInterpreter
import caliban.schema.Schema.auto.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.ci.CIStringSyntax
import zio.{Runtime, ZEnvironment}
import sttp.tapir.json.circe.*
import pureconfig.*
import pureconfig.generic.derivation.default.*
import barker.schema.{*, given}
import barker.services.Services

final case class AppConfig(db: DBConfig) derives ConfigReader

object Main extends IOApp:
  given logger: Logger[RequestIO] = Slf4jLogger.getLogger[RequestIO]

  private val TokenHeader = ci"X-Token"
  private val appConfig = ConfigSource.default.loadOrThrow[AppConfig]

  /** This is middleware that takes token from request, constructs RequestContext, and injects it into RequestIO (using
    * Kleisli.local) Original Caliban sample is using cats-mtl Local typeclass to achieve simpler notation, below it is
    * implemented in terms of pure http4s/cats types and without using final tagless style, which still fits into single
    * line of code.
    */
  private def extractAccessToken(routes: HttpRoutes[RequestIO]): HttpRoutes[RequestIO] =
    Kleisli { (req: Request[RequestIO]) =>
      val ctx = req.headers.get(TokenHeader) match
        case Some(tokenNel) => RequestContext(Some(tokenNel.head.value))
        case None           => RequestContext(None)
      OptionT(routes.run(req).value.local[RequestContext](_ => ctx))
    }

  private def makeGraphQLRoutes(interpreter: GraphQLInterpreter[RequestContext, CalibanError])(using
      ev: ToEffect[RequestIO, RequestContext]
  ): HttpRoutes[RequestIO] =
    extractAccessToken(
      Http4sAdapter.makeHttpServiceF[RequestIO, RequestContext, CalibanError](HttpInterpreter(interpreter))
    )

  private def makeHttpApp(
      interpreter: GraphQLInterpreter[RequestContext, CalibanError]
  )(using
      ev: ToEffect[RequestIO, RequestContext]
  ): HttpApp[RequestIO] =
    Router("/api/graphql" -> makeGraphQLRoutes(interpreter)).orNotFound

  def run(args: List[String]): IO[ExitCode] =
    // needed for Caliban-CE interop
    // TODO: are our IOs running on ZIO runtime now? How is this configured?
    // finally, we run on runtime provided by IOApp, right?
    given Runtime[RequestContext] = Runtime.default.withEnvironment(ZEnvironment(RequestContext(None)))
    given InjectEnv[RequestIO, RequestContext] = InjectEnv.kleisli

    Dispatcher
      .parallel[RequestIO]
      .flatMap { dispatcher =>
        // we want to be consistent with using "given" rather than "implicit"
        given interop: CatsInterop.Contextual[RequestIO, RequestContext] = CatsInterop.contextual(dispatcher)

        val init = for
          _ <- logger.info(s"Starting Barker, running db migrations ...")
          _ <- RequestIO.liftIO(DB.runMigrations(appConfig.db))
          _ <- logger.info(s"Wiring services ...")
          services <- RequestIO.liftIO(Services())
          _ <- logger.info(s"Building GraphQL schema ...")
          schema = new BarkerSchema(services)
          api = graphQL[RequestContext, barker.schema.Query, Unit, Unit](RootResolver(schema.query))
          interpreter <- interop.toEffect(api.interpreter)
        yield interpreter

        for
          interpreter <- Resource.eval(init)
          server <- EmberServerBuilder
            .default[RequestIO]
            .withHost(ipv4"0.0.0.0")
            .withPort(port"8090")
            .withHttpApp(makeHttpApp(interpreter))
            .build
        yield server
      }
      .useForever
      .run(RequestContext(None))
