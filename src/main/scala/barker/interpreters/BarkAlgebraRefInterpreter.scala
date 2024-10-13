package barker.interpreters

import barker.algebras.BarkAlgebra
import barker.entities.{Bark, BarkId, Likes, Rebarks, UserId}
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.syntax.all.*

import java.time.Instant
import java.util.UUID

/** Ref-based impl might not be something to use in production but is definitely usable for testing
  */
private[interpreters] class BarkAlgebraRefInterpreter(ref: Ref[IO, Map[BarkId, Bark]]) extends BarkAlgebra:

  override def post(author: UserId, content: String): IO[Bark] =
    val bark = Bark(
      id = BarkId(UUID.randomUUID()),
      authorId = author,
      content = content,
      rebarkFromId = None,
      createdAt = Instant.now(),
      likes = Likes(0),
      rebarks = Rebarks(0)
    )
    ref.update(barksMap => barksMap + (bark.id -> bark)).as(bark)

  override def rebark(author: UserId, sourceBarkId: BarkId, addedContent: String): IO[Bark] =
    for
      barks <- ref.get
      sourceOpt = barks.get(sourceBarkId)
      _ <- sourceOpt.fold(IO.raiseError[Unit](Throwable("Source bark not found")))(_ => IO.unit)
      newBark = Bark(
        id = BarkId(UUID.randomUUID()),
        authorId = author,
        content = addedContent,
        rebarkFromId = sourceBarkId.some,
        createdAt = Instant.now(),
        likes = Likes(0),
        rebarks = Rebarks(0)
      )
      _ <- ref.update { barksMap =>
        // to correctly handle concurrency, we need to fetch source again inside update
        val withSourceUpdated = barksMap.get(sourceBarkId) match
          case Some(v) => barksMap + (v.id -> v.copy(likes = Likes(v.likes.value)))
          // under current contract source is guaranteed to exist so line below is really unreachable
          // if that ever changes it's a bit unfortunate as we cannot raise error from here
          // guess that's ok for test implementation
          case None => barksMap
        withSourceUpdated + (newBark.id -> newBark)
      }
    yield newBark

  override def list(author: UserId): IO[List[Bark]] =
    ref.get.map(_.values.toList.sortBy(_.createdAt))

object BarkAlgebraRefInterpreter:
  def apply(): IO[BarkAlgebra] =
    Ref[IO].of(Map.empty[BarkId, Bark]).map(new BarkAlgebraRefInterpreter(_))
