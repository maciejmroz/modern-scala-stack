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
import caliban.interop.tapir.HttpInterpreter
import caliban.schema.Schema.auto.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.ci.CIStringSyntax
import zio.{Runtime, ZEnvironment}
import sttp.tapir.json.circe.*
import pureconfig.*
import pureconfig.generic.derivation.default.*
import barker.schema.*
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

  private def init(using
      interop: CatsInterop.Contextual[RequestIO, RequestContext]
  ): RequestIO[GraphQLInterpreter[RequestContext, CalibanError]] =
    for
      _ <- logger.info(s"Starting Barker, running db migrations ...")
      _ <- RequestIO.liftIO(DB.runMigrations(appConfig.db))
      _ <- logger.info(s"Wiring services ...")
      services <- RequestIO.liftIO(Services())
      _ <- logger.info(s"Building GraphQL schema ...")
      interpreter <- GraphQLInit.makeInterpreter(services)
    yield interpreter

  def run(args: List[String]): IO[ExitCode] =
    // needed for Caliban-CE interop (to instantiate CatsInterop.Contextual later)
    // TODO: are our IOs running on ZIO runtime now? How is this configured?
    // finally, we run on runtime provided by IOApp, right?
    given runtime: Runtime[RequestContext] = Runtime.default.withEnvironment(ZEnvironment(RequestContext(None)))
    given injector: InjectEnv[RequestIO, RequestContext] = InjectEnv.kleisli

    val serverResource = for
      dispatcher <- Dispatcher.parallel[RequestIO]
      // CatsInterop.Contextual is FromEffect and ToEffect in one given instance. We need to explicitly pass type parameters
      // to contextual as compiler type inference has trouble here
      given CatsInterop.Contextual[RequestIO, RequestContext] = CatsInterop.contextual[RequestIO, RequestContext](
        dispatcher
      )
      interpreter <- Resource.eval(init)
      server <- EmberServerBuilder
        .default[RequestIO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8090")
        .withHttpApp(makeHttpApp(interpreter))
        .build
    yield server

    serverResource.useForever
      .run(RequestContext(None))
