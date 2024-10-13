package barker.app

import barker.schema.{*, given}
import barker.interpreters.AllInterpreters
import caliban.*
import caliban.interop.cats.implicits.*
import caliban.interop.cats.{CatsInterop, InjectEnv}
import caliban.interop.tapir.HttpInterpreter
import caliban.schema.Schema.auto.*
import cats.effect.std.Dispatcher
import org.http4s.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.tapir.json.circe.*
import zio.{Runtime, ZEnvironment}

object GraphQLRoutes:
  private val logger: Logger[Fx] = Slf4jLogger.getLogger[Fx]

  def makeInterpreter(algebras: AllInterpreters)(using
                                                 interop: CatsInterop.Contextual[Fx, AppContext]
  ): Fx[GraphQLInterpreter[AppContext, CalibanError]] =
    val schema = new BarkerSchema(algebras)
    val api = graphQL[AppContext, barker.schema.Query, barker.schema.Mutation, Unit](
      RootResolver(schema.query, schema.mutation)
    )
    interop.toEffect(api.interpreter)

  def make(services: AllInterpreters, dispatcher: Dispatcher[Fx]): Fx[HttpRoutes[Fx]] =
    // ZIO runtime is needed for Caliban-CE interop (to instantiate CatsInterop.Contextual below)
    // I am not really sure what exactly runs on CE runtime and what runs in ZIO. Both runtimes create thread pools
    // each with size equal to logical CPU cores which is not optimal if there's significant work allocated to both.
    given Runtime[AppContext] = Runtime.default.withEnvironment(ZEnvironment(AppContext(None)))
    given InjectEnv[Fx, AppContext] = InjectEnv.kleisli
    // CatsInterop.Contextual is FromEffect and ToEffect in one given instance.
    given CatsInterop.Contextual[Fx, AppContext] = CatsInterop.contextual[Fx, AppContext](dispatcher)

    for
      _ <- logger.info(s"Building GraphQL schema ...")
      interpreter <- GraphQLRoutes.makeInterpreter(services)
    yield Http4sAdapter.makeHttpServiceF[Fx, AppContext, CalibanError](HttpInterpreter(interpreter))
