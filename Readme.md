# Modern Scala Stack

This project is a humble attempt to reimagine Scala based GraphQL API backend if I was starting it now
(so late 2024). It does not solve every possible problem, and is not something that's production ready, it only
is a reflection of what my choices for generic backend might be (given no special requirements).

Overarching theme here is functional programming with effects, while also making code as simple and
as approachable as possible.

I've put multiple comments in the code where I believe it makes sense to do more explaining. Also,
there are quite a few TODOs - which are not necessarily defects but point to things that may require
more work, especially as project grows. I tried to avoid dead code but can't guarantee that you'll not find case class
or method that's unused.

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
  fully integrates with effect systems.
- Doobie: maybe not the most fashionable database access library but integrates well with FP
  ecosystem and gets the job done through what is mostly just plain SQL.
- DB used is just Postgres - again, nothing fancy, only tried and robust

This obviously does not list every single library used, only the most major ones. Also, I stand
firmly behind an opinion that what you do with the tech stack (both on product and process axes)
matters a lot more than what your tech stack actually is :)

## Style choices

I believe in simplicity, and try to reflect that in how code is structured. I generally try to be
as straightforward as possible in code, and try _not_ to use complex constructs too
much. So:

- I do not use tagless final pattern, and just use concrete effect types where possible. This might
  not scale well to 1M LOC project developed by army of engineers, but I believe it is a pragmatic
  choice for small projects.
- I generally do not use `Kleisli` for dependency injection (unless I have to, like for http4s/Caliban
  integrations), and I do not use any dependency injection framework, only traditional constructor parameter
  passing. This is an experiment I am not 100% sure of - might backtrack on this going forward.
- Services are fixed on `IO` effect. I know some people will disagree, but I believe lifting pure values
  to `IO` isn't really much different from using `Id` type parameter in testing abstract `F[_]` algebras.
  Accidentally, because at infrastructure level I am forced to use different effect type, this creates natural
  separation of system layers - maybe that's good?
- I absolutely _do_ use macro-based capabilities of libraries like Caliban and intend to use it
  in cases where I believe it to be improvement to productivity and/or readability.
- The syntax used is new Python-esque Scala syntax, with scalafmt set up to rewrite into this syntax. This is an
  experiment at reducing visual noise, which is an aspect of Python I really like (this also applies to Haskell, which
  is likely a better analogy in FP context any way;) ).

Again, look for comments in the code for more specifics.

## Project structure

The project is onion-ish architecture, with dependencies looking more or less like this:

```
app                <- http4s (and everything else)
  infrastructure   
  schema           <- Caliban
    services       <- IO, Doobie
      entities     <- minimal library dependencies, domain only
```

## Building

Build system used is sbt, which may not be the coolest thing out there, but it's been around forever.
Even if you don't know how to do something, there's always Google/SO (or even official docs!!! ;) )
available to help out.

There is custom `runMigrations` command available in sbt. Migrations are also run automatically on app start
(as you would expect). They _do not_ automatically run when running tests.

Technically it is a multi-project build but only because there's separate project for integration
tests. Standard `test` task contains only unit tests, and `integration/test` should run things like
database tests (or whatever you need to have as dependency) - in here it is expected to have Postgres running in
Docker container for these tests to complete.

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