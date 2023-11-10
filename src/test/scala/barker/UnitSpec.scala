package barker

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec

trait UnitSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers
