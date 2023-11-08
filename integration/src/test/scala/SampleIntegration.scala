package barker.services

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec

class SampleIntegration extends AsyncFreeSpec with AsyncIOSpec with Matchers:
  "SampleIntegration" - {

    "should run" in {
      IO {
        assert(true)
      }
    }
  }
