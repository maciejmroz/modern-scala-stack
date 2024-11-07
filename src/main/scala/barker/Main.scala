package barker

import barker.app.{AppConfig, DB, GraphQLRoutes}
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
import org.typelevel.ci.CIString
import pureconfig.*
import barker.schema.*
import barker.interpreters.Interpreters
import barker.entities.AccessToken

object Main extends IOApp:
  private val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val appConfig = ConfigSource.default.loadOrThrow[AppConfig]
  private val TokenHeader = CIString(appConfig.http.header)

  /** This is middleware that takes token from request, constructs RequestContext, and injects it into RequestIO (using
    * Kleisli.local) Original Caliban sample is using cats-mtl Local typeclass to achieve simpler notation, below it is
    * implemented in terms of pure http4s/cats types and without using final tagless style, which is still very compact.
    */
  private def accessTokenMiddleware(routes: HttpRoutes[Fx]): HttpRoutes[Fx] =
    Kleisli { (req: Request[Fx]) =>
      val at = req.headers.get(TokenHeader).map(_.head.value)
      OptionT(routes.run(req).value.local[AppContext](_.copy(accessToken = at.map(AccessToken.apply))))
    }

  private def makeHttpApp(
      graphQLRoutes: HttpRoutes[Fx]
  ): HttpApp[Fx] =
    Router("/api/graphql" -> accessTokenMiddleware(graphQLRoutes)).orNotFound

  private def initInterpreters(): Resource[IO, Interpreters] =
    for
      transactor <- DB.transactor[IO](appConfig.db)
      interpreters <- Resource.eval {
        for
          _ <- logger.info(s"Running db migrations ...")
          _ <- DB.runMigrations(appConfig.db)
          _ <- logger.info(s"Wiring services ...")
          interpreters <- Interpreters(transactor)
        yield interpreters
      }
    yield interpreters

  def run(args: List[String]): IO[ExitCode] =
    val serverResource = for
      dispatcher <- Dispatcher.parallel[Fx]
      interpreters <- initInterpreters().mapK(Fx.liftK)
      graphQLRoutes <- Resource.eval(GraphQLRoutes.make(interpreters, dispatcher))
      server <- EmberServerBuilder
        .default[Fx]
        .withHost(Host.fromString(appConfig.http.host).get)
        .withPort(Port.fromInt(appConfig.http.port).get)
        .withHttpApp(makeHttpApp(graphQLRoutes))
        .build
    yield server

    serverResource.useForever
      .run(AppContext(None))
