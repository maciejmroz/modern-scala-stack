ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "com.maciejmroz"

val http4sVersion = "0.23.27" //"1.0.0-M40"
val doobieVersion = "1.0.0-RC5"
val circeVersion = "0.15.0-M1"

lazy val barkerDependencies = Seq(
  "org.typelevel" %% "cats-effect" % "3.5.4",
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.typelevel" %% "log4cats-core" % "2.7.0",
  "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "com.github.ghostdogpr" %% "caliban" % "2.8.1",
  "com.github.ghostdogpr" %% "caliban-cats" % "2.8.1",
  "com.github.ghostdogpr" %% "caliban-http4s" % "2.8.1",
  "com.github.ghostdogpr" %% "caliban-tapir" % "2.8.1",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.10.15",
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-scalatest" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,
  "com.github.pureconfig" %% "pureconfig-core" % "0.17.7",
  "org.flywaydb" % "flyway-core" % "10.15.2",
  "org.flywaydb" % "flyway-database-postgresql" % "10.14.0",
  "io.scalaland" %% "chimney" % "1.3.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-optics" % "0.15.0",
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
)

lazy val runMigrations = taskKey[Unit]("Migrates the database schema.")

lazy val barker = project
  .in(file("."))
  .settings(
    name := "Barker",
    libraryDependencies ++= barkerDependencies,
    Compile / run / fork := true,
    fullRunTask(runMigrations, Compile, "barker.app.DBMigrationsCommand"),
    runMigrations / fork := true,
    scalacOptions ++= Seq(
      "-Wunused:all",
      "-Wvalue-discard"
    )
  )

//TODO: longer term switch to separate set of dependencies for integration tests
lazy val integration = (project in file("integration"))
  .dependsOn(barker % "compile->compile;test->test")
  .settings(
    publish / skip := true,
    libraryDependencies ++= barkerDependencies
  )
