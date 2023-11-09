package barker

import cats.effect.*
import doobie.implicits.*
import org.scalatest.freespec.AsyncFreeSpec

class SampleIntegration extends DbOnlySpec:
  "SampleIntegration" - {

    "should run" in {
      for result <- sql"select 1"
          .query[String]
          .unique
          .transact(xa)
      yield result shouldBe "1"
    }
  }
