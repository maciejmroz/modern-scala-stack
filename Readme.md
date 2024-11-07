# Modern Scala Stack

This project is a humble attempt to reimagine Scala based GraphQL API backend if I was starting it now
(so late 2024). It does not solve every possible problem, and is not something that's production ready, it only
is a reflection of what my choices for generic backend might be (given no special requirements) as well as a playground
for ideas I would not be able to test otherwise. I am not trying to convince anyone to use Scala, specific
libraries or approaches - do whatever works for you.

Overarching theme here is functional programming with effects, while also making code as simple and
as approachable as possible.

I've put multiple comments in the code where I believe it makes sense to do more explaining. Also,
there are quite a few TODOs - which are not necessarily defects but point to things that may require
more work, especially if the project grows. I tried to avoid dead code but can't guarantee that you'll not find
case class or method that's unused.

## Example app

Welcome to Barker, which is like Twitter, except it's for dogs, not for birds! We are addressing
completely new and unexplored niche for revolutionary messaging app! On serious note, I just didn't want to do
another TODO app.

## The stack

Highlights:

- Scala 3: I do not believe there is an excuse to start new project on 2.13 today. Yes, Scala 3 tooling leaves
  a lot to be desired and is still much worse than for Scala 2, but I believe it reached a point of being usable, at
  least for small projects.
- Cats Effect: Today's Scala is functional Scala with effects running on async runtime, either in
  Typelevel or ZIO flavour. This is what really makes Scala programming a unique experience that
  brings many "wow" moments ;) Why Cats Effect and not ZIO? I have way more familiarity with CE,
  that really is the only reason
- http4s: We want to do functional Scala here ;)
- Caliban: again, in the spirit of functional programming, I went with GraphQL library that
  fully integrates with effect systems. Caliban is ZIO focused, and it seems that for more advanced use cases
  it might be easier to use with ZIO. This would also avoid having two runtimes backed by separate thread pools,
  which is a downside of this project (not something that really matters unless you really want to squeeze every
  last bit of performance from your app).
- Doobie: maybe not the most fashionable database access library but integrates well with FP
  ecosystem and gets the job done through what is mostly just plain SQL.
- DB used is just Postgres - again, nothing fancy, only tried and robust. I also used it because I use MySQL
  daily and wanted to play with something slightly different :) I did not squash DB migrations, so you can see
  all the silly things I've done :)

This obviously does not list every single library used, only the most major ones.

## Project structure

The project is onion-ish architecture, with dependencies looking more or less like this (marked by indentation):

```
app                <- http4s (and everything else)
  schema           <- Fx (custom Kleisli-based effect), Caliban
  interpreters     <- IO, Doobie
    programs       
      algebras     <- Cats Effect (IO effect)
        entities   <- minimal library dependencies, domain only
```

`programs`, `algebras` and `entities` together create core domain model. While having `IO` dependency at this level is
somewhat dangerous it is also quite pragmatic, especially for a small project. Even in simple real world app, you will
need to model some of the capabilities provided by `IO` (one way or another) and for simple project well ... just use
`IO` directly without any abstractions and don't worry too much.

`interpreters` provide actual implementation of algebras for programs to use.

`schema` contains Caliban support code, and GraphQL schema itself

`app` is pretty much a catch-all for infrastructure code, and wires everything to http4s (which obviously resides at the
very edge of the system)

## Building

Build system used is sbt, which may not be the coolest thing out there, but it's been around forever.
Even if you don't know how to do something, there's always Google/SO (or even official docs!!! ;) )
available to help out.

There is custom `runMigrations` command available in sbt. Migrations are also run automatically on app start
(as you would expect). They _do not_ automatically run when running tests.

Technically it is a multi-project build but only because there's a separate project for integration
tests. Standard `test` task contains only small tests, and `integration/test` should run things like
database tests (or whatever you need to have as dependency) - in here it is expected to have Postgres running in
Docker container for these tests to complete (Docker Compose file included). You could use testcontainers library
to control that programmatically.

## Testing

A practical approach to testing pyramid I stumbled upon recently is small/medium/large approach where:

- small is everything that happens in-process (that means no I/O)
- medium is anything that happens on single machine/vm but involves multiple processes
- large is anything that's requiring multiple dedicated machines/vms - not really a concern here

Advantage of this taxonomy is focus on speed of execution and potential for flakiness. While "small" is pretty big
superset of unit testing, it is expected that these tests will be mostly CPU bound - so very fast and easy to run
in parallel fashion. So, while `BasicSpec` expects tests to run in `IO`, it does not expect them to actually _do_ I/O.
Tests in this spec might for example use test doubles based on `Ref` from Cats Effect, or use other CE features. Of
course, you can/should still do unit testing where applicable. `GraphQLSpec` allows testing GraphQL queries, which is
useful to flag schema breakage and maybe some simple wiring problems without requiring us to spin up web server.

"Medium" is when we need to talk to DB on the same host (or in general communicate with some other
process) - these may still be relatively fast but are way, way slower than "small" tests. To avoid accidentally
mixing different size classes, `DbSpec` is defined only in the `integration` subproject.

You can find chapter on this approach
in [Software Engineering at Google](https://www.oreilly.com/library/view/software-engineering-at/9781492082781/).
While this book is about large engineering organization so not directly applicable to small companies or people
doing hobby projects, it is still very well written and insightful.

## Style choices

I believe in simplicity, and try to reflect that in how code is structured and written. With simplicity, I do not
necessarily focus on initial "hello world" setup but rather on how simple it is to add something to the project
(so in case of GraphQL API this means adding queries, mutations, db migrations, tests etc.).
I still try to be as straightforward as possible in code, and try _not_ to use complex constructs too
much. So:

- I do not use tagless final pattern, and just use concrete effect types where possible. This might
  not scale well to 1M LOC project developed by army of engineers, but I believe it is a pragmatic
  choice for small projects, where having "too much power" does not inhibit ability to reason about the code.
- I do not use `Kleisli` for dependency injection unless I have to (like for http4s/Caliban
  integrations), and I do not use any dependency injection framework, only traditional constructor parameter
  passing. This is an experiment I am not 100% sure of - might backtrack on this going forward.
- Going with the theme of concrete effect types, services are fixed on `IO` effect, as this is the "common ground"
  for all CE apps and requires no explaining to anyone. Should anyone ever want to extend this layer of the system,
  they can just do so based on whatever they _already_ know, with zero surprises. And for testing, lifting pure values
  to `IO` isn't really much different from using `Id` type parameter in testing abstract `F[_]` algebras.
  At infrastructure/app level there's `Fx` effect type, which really is a `Kleisli` - I guess not something that
  can easily be avoided. This creates natural separation of system layers - maybe that's good? Alternate design
  would be to just use tagless final all the way, extend environment type to include services, and privide
  typeclasses to access the environment (maybe even just use `Ask` from cats-mtl). This might be cleaner if you
  look through functional programming lens (and maybe even objectively better) but I believe it would also be
  harder to get into, unless you come to scala from Haskell.
- I absolutely _do_ use macro-based capabilities of libraries like Caliban and intend to use it
  in cases where I believe it to be improvement to productivity and/or readability. Other example (although a smaller
  one) is excellent chimney library for transforming data types.
- The syntax used is new Python-esque Scala syntax, with scalafmt set up to rewrite into this syntax. This is an
  experiment at reducing visual noise, which is an aspect of Python I really like (this also applies to Haskell, which
  is likely a better analogy in FP context any way;) ).

Again, look for comments in the code for more specifics.
