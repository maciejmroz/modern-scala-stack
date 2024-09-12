package barker

import barker.infrastructure.DBConfig
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import doobie.Transactor
import org.scalatest.Suite
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.ConfigSource

/** Test spec that gives access to Doobie transactor, and nothing else
  */
trait DbOnlySpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with doobie.scalatest.IOChecker:
  self: Suite =>
  lazy val dbConfig: DBConfig = ConfigSource.default.loadOrThrow[AppConfig].db
  lazy val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = dbConfig.jdbcDriver,
    url = dbConfig.jdbcUrl,
    user = dbConfig.username,
    password = dbConfig.password,
    logHandler = None
  )
