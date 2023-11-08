ThisBuild / scalaVersion := "3.3.0"
ThisBuild / organization := "com.maciejmroz"

val http4sVersion = "0.23.19" //"1.0.0-M40"

lazy val barkerDependencies = Seq(
  "org.typelevel" %% "cats-effect" % "3.5.1",
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
  "org.slf4j" % "slf4j-simple" % "1.7.36",
  "com.github.ghostdogpr" %% "caliban" % "2.3.1",
  "com.github.ghostdogpr" %% "caliban-cats" % "2.3.1",
  "com.github.ghostdogpr" %% "caliban-http4s" % "2.3.1",
  "com.github.ghostdogpr" %% "caliban-tapir" % "2.3.1",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.2.11",
  "io.scalaland" %% "chimney" % "0.8.0-RC1",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
)

lazy val barker = project
  .in(file("."))
  .settings(
    name := "Hello",
    libraryDependencies ++= barkerDependencies,
    scalacOptions ++= Seq(
      "-Wunused:all",
      "-Wvalue-discard"
    )
  )

//TODO: longer term switch to separate set of dependencies for integration tests
lazy val integration = (project in file("integration"))
  .dependsOn(barker)
  .settings(
    publish / skip := true,
    libraryDependencies ++= barkerDependencies
  )
