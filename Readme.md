# Modern Scala Stack

This project is a humble attempt to reimagine Scala based GraphQL API backend if I was starting it now
(late 2023). It does not solve every possible problem, and is not something that's really production
ready, it only is reflection of what my choices for generic backend might be.

Overarching theme here is functional programming with effects, while also making code as simple and
as approachable as possible.

I've put several comments in the code where I believe it makes sense to do more explaining.

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

This obviously does not list every single library used, only the most major ones.

## Style choices

I believe in simplicity, and try to reflect that in how code is structured. I generally try to be
as straightforward as possible in code, and try _not_ to use complex constructs too
much. So:

- I do not use tagless final pattern, and just use concrete effect types where possible. This might
  not scale well to 1M LOC project developed by army of engineers but I believe it is a pragmatic
  choice for small projects.
- I generally do not use `Kleisli` for dependency injection (unless I have to), and I do not use
  any dependency injection framework, only traditional constructor parameter passing.
- with these two in mind, services are generally fixed on `IO`. I know some people will disagree,
  but I believe lifting pure values to `IO` isn't really much different from using `Id` in testing.
- I absolutely _do_ use macro-based capabilities of libraries like Caliban and intend to use it
  in cases where I believe it to be improvement to productivity and/or readability.

Again, look for comments in the code for more specifics.