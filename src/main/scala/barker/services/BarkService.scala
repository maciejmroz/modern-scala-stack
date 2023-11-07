package barker.services

import java.util.UUID
import java.time.ZonedDateTime
import cats.syntax.all.*
import cats.effect.IO
import cats.effect.kernel.Ref

import barker.entities.*

/** Service definitions
  *
  * Why fix everything on IO effect and inject dependencies via constructor parameters? Because it is extremely simple
  * to grasp.
  *
  * While I get that having tagless final backed by something like Kleisli has several advantages it also has
  * significant barrier to entry, so what I am trying here is answering a question: what if we just don't do it?
  */

trait BarkService:
  def post(author: UserId, content: String): IO[Bark]
  def rebark(author: UserId, sourceBarkId: BarkId, addedContent: String): IO[Bark]
  def list(author: UserId): IO[List[Bark]]

/** Ref-based impl might not be something to use in production but is definitely usable for testing
  */
private[services] class BarkServiceRefImpl(ref: Ref[IO, Map[BarkId, Bark]]) extends BarkService:

  override def post(author: UserId, content: String): IO[Bark] =
    val bark = Bark(
      id = BarkId(UUID.randomUUID()),
      authorId = author,
      content = content,
      rebarkFromId = None,
      createdAt = ZonedDateTime.now(),
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
        createdAt = ZonedDateTime.now(),
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

object BarkService:
  // Let's have Ref based impl for now
  // as constructing a Ref is an IO by itself, we return IO[BarkService] rather than raw value
  def apply(): IO[BarkService] =
    Ref[IO].of(Map.empty[BarkId, Bark]).map(new BarkServiceRefImpl(_))
