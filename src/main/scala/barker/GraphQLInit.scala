package barker

import barker.services.Services
import caliban.*
import caliban.interop.cats.implicits.*
import cats.effect.IO
import caliban.schema.Schema.auto.*
import barker.schema.{*, given}
import caliban.interop.cats.CatsInterop

object GraphQLInit:
  def makeInterpreter(servicesIO: IO[Services])(using
      interop: CatsInterop.Contextual[RequestIO, RequestContext]
  ): RequestIO[GraphQLInterpreter[RequestContext, CalibanError]] =
    for
      services <- RequestIO.liftIO(servicesIO)
      schema = new BarkerSchema(services)
      api = graphQL[RequestContext, barker.schema.Query, Unit, Unit](RootResolver(schema.query))
      interpreter <- interop.toEffect(api.interpreter)
    yield interpreter
