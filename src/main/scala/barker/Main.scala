package barker

import barker.infrastructure.{DB, DBConfig}
import caliban.interop.cats.{CatsInterop, InjectEnv}
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
import caliban.schema.Schema.auto.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.ci.CIStringSyntax
import zio.{Runtime, ZEnvironment}
import pureconfig.*
import pureconfig.generic.derivation.default.*
import barker.schema.*
import barker.services.Services

final case class AppConfig(db: DBConfig) derives ConfigReader

object Main extends IOApp:
  given fxLogger: Logger[Fx] = Slf4jLogger.getLogger[Fx]
  given ioLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val TokenHeader = ci"X-Token"
  private val appConfig = ConfigSource.default.loadOrThrow[AppConfig]

  /** This is middleware that takes token from request, constructs RequestContext, and injects it into RequestIO (using
    * Kleisli.local) Original Caliban sample is using cats-mtl Local typeclass to achieve simpler notation, below it is
    * implemented in terms of pure http4s/cats types and without using final tagless style, which is still very compact.
    */
  private def accessTokenMiddleware(routes: HttpRoutes[Fx]): HttpRoutes[Fx] =
    Kleisli { (req: Request[Fx]) =>
      val at = req.headers.get(TokenHeader).map(_.head.value)
      OptionT(routes.run(req).value.local[AppContext](_.copy(accessToken = at)))
    }

  private def makeHttpApp(
      graphQLRoutes: HttpRoutes[Fx]
  ): HttpApp[Fx] =
    Router("/api/graphql" -> accessTokenMiddleware(graphQLRoutes)).orNotFound

  private def initServices(): Resource[IO, Services] =
    for
      transactor <- DB.transactor[IO](appConfig.db)
      services <- Resource.eval {
        for
          _ <- ioLogger.info(s"Running db migrations ...")
          _ <- DB.runMigrations(appConfig.db)
          _ <- ioLogger.info(s"Wiring services ...")
          services <- Services(transactor)
        yield services
      }
    yield services

  private def initGraphQL(services: Services, dispatcher: Dispatcher[Fx]): Fx[HttpRoutes[Fx]] =
    // needed for Caliban-CE interop (to instantiate CatsInterop.Contextual below)
    // TODO: are our IOs running on ZIO runtime now? How is this configured?
    //   finally, we run on runtime provided by IOApp, right?
    given Runtime[AppContext] = Runtime.default.withEnvironment(ZEnvironment(AppContext(None)))
    given InjectEnv[Fx, AppContext] = InjectEnv.kleisli
    // CatsInterop.Contextual is FromEffect and ToEffect in one given instance.
    given CatsInterop.Contextual[Fx, AppContext] = CatsInterop.contextual[Fx, AppContext](dispatcher)
    for
      _ <- fxLogger.info(s"Building GraphQL schema ...")
      interpreter <- CalibanInit.makeInterpreter(services)
    yield CalibanInit.makeGraphQLRoutes(interpreter)

  def run(args: List[String]): IO[ExitCode] =
    val serverResource = for
      dispatcher <- Dispatcher.parallel[Fx]
      services <- initServices().mapK(Fx.liftK)
      graphQLRoutes <- Resource.eval(initGraphQL(services, dispatcher))
      server <- EmberServerBuilder
        .default[Fx]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8090")
        .withHttpApp(makeHttpApp(graphQLRoutes))
        .build
    yield server

    serverResource.useForever
      .run(AppContext(None))
