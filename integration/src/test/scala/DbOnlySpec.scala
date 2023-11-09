package barker

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import doobie.Transactor
import org.scalatest.Suite
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec
import pureconfig.ConfigSource

trait DbOnlySpec extends AsyncFreeSpec with AsyncIOSpec with Matchers:
  self: Suite =>
  lazy val dbConfig: DBConfig = ConfigSource.default.loadOrThrow[AppConfig].db
  lazy val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = dbConfig.jdbcDriver,
    url = dbConfig.jdbcUrl,
    user = dbConfig.username,
    password = dbConfig.password,
    logHandler = None
  )
