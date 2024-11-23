package barker.app

import cats.effect.{ExitCode, IO, IOApp}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

// inspired by https://alexn.org/blog/2020/11/15/managing-database-migrations-scala/

/** This is sbt command implementation that allows you to run DB migrations by "sbt runMigrations"
  */
object DBMigrationsCommand extends IOApp:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  private val dbConfig = ConfigSource.default.at("db").loadOrThrow[DBConfig]

  def run(args: List[String]): IO[ExitCode] =
    for
      _ <- logger.info(s"Migrating database configuration")
      _ <- DB.runMigrations(dbConfig)
    yield ExitCode.Success
