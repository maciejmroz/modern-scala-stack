package barker

import org.http4s.*
import caliban.*
import caliban.interop.cats.implicits.*
import caliban.interop.cats.{CatsInterop, ToEffect}
import caliban.interop.tapir.HttpInterpreter
import caliban.schema.Schema.auto.*
import sttp.tapir.json.circe.*

import barker.schema.{*, given}
import barker.services.Services

object CalibanInit:
  def makeInterpreter(services: Services)(using
      interop: CatsInterop.Contextual[Fx, AppContext]
  ): Fx[GraphQLInterpreter[AppContext, CalibanError]] =
    val schema = new BarkerSchema(services)
    val api = graphQL[AppContext, barker.schema.Query, Unit, Unit](RootResolver(schema.query))
    interop.toEffect(api.interpreter)

  def makeGraphQLRoutes(interpreter: GraphQLInterpreter[AppContext, CalibanError])(using
      ev: ToEffect[Fx, AppContext]
  ): HttpRoutes[Fx] =
    Http4sAdapter.makeHttpServiceF[Fx, AppContext, CalibanError](HttpInterpreter(interpreter))
