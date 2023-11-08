# Modern Scala Stack

This project is a humble attempt to reimagine Scala based GraphQL API backend if I was starting it now
(late 2023). It does not solve every possible problem, and is not something that's really production
ready, it only is reflection of what my choices for generic backend might be (given no special
requirements).

Overarching theme here is functional programming with effects, while also making code as simple and
as approachable as possible.

I've put several comments in the code where I believe it makes sense to do more explaining. Also,
there are quite a few TODOs - which are not necessarily defects but point to things that may require
more work, especially as project grows.

## Example app

Welcome to Barker, which is like Twitter, except it's for dogs, not for birds! We are addressing
completely new and unexplored niche for revolutionary messaging app!

## The stack

Highlights:

- Scala 3: I do not believe there is an excuse to start new project on 2.13 today.
- Cats Effect: Today's Scala is functional Scala with effects running on async runtime, either in
  Typelevel or ZIO flavour. This is what really makes Scala programming a unique experience that
  brings many "wow" moments ;) Why Cats Effect and not ZIO? I have way more familiarity with CE,
  that really is the only reason
- http4s: We want to do functional Scala here ;)
- Caliban: again, in the spirit of functional programming, I went with GraphQL library that
  fully integrates with effect systems.

This obviously does not list every single library used, only the most major ones. Also, I stand
firmly behind an opinion that what you do with the tech stack (both on product and process axes)
matters a lot more than what your tech stack actually is.

## Style choices

I believe in simplicity, and try to reflect that in how code is structured. I generally try to be
as straightforward as possible in code, and try _not_ to use complex constructs too
much. So:

- I do not use tagless final pattern, and just use concrete effect types where possible. This might
  not scale well to 1M LOC project developed by army of engineers, but I believe it is a pragmatic
  choice for small projects.
- I generally do not use `Kleisli` for dependency injection (unless I have to), and I do not use
  any dependency injection framework, only traditional constructor parameter passing.
- Services are fixed on `IO` effect. I know some people will disagree,
  but I believe lifting pure values to `IO` isn't really much different from using `Id` type
  parameter in testing abstract `F[_]` services.
- I absolutely _do_ use macro-based capabilities of libraries like Caliban and intend to use it
  in cases where I believe it to be improvement to productivity and/or readability.
- The syntax used is new Python-esque Scala syntax, with scalafmt set up to rewrite into
  this syntax. This is an experiment at reducing visual noise, which is an aspect of
  Python (and Haskell ;) ) I really like.

Again, look for comments in the code for more specifics.

## Project structure

Build system used is sbt, which may not be the coolest thing out there, but it's been around forever.
Even if you don't know how to do something, there's always Google/SO (or even official docs!!! ;) )
available to help out.

Technically it is a multi-project build but only because there's separate project for integration
tests. Standard `test` task contains only unit tests, and `integration/test` should run things like
database tests (or whatever you need to have as dependency).