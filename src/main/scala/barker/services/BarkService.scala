package barker.services

import java.util.UUID
import java.time.Instant
import cats.syntax.all.*
import cats.effect.IO
import cats.effect.kernel.Ref
import doobie.{ConnectionIO, Query0, Transactor}
import doobie.implicits.*
import barker.entities.*
import doobie.util.update.Update0
import DoobieMappings.given

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

private[services] class BarkServiceDbImpl(xa: Transactor[IO]) extends BarkService:
  def insertBarkQuery(bark: Bark): Update0 =
    sql"""INSERT INTO bark_bark(bark_id, author_id, content, rebark_from_id, created_at) 
     VALUES(${bark.id}, ${bark.authorId}, ${bark.rebarkFromId}, ${bark.createdAt})""".update

  def insertBark(bark: Bark): ConnectionIO[Bark] =
    insertBarkQuery(bark).run.as(bark)

  def selectBarkByIdQuery(barkId: BarkId): Query0[Bark] =
    sql"SELECT bark_id, author_id, content, rebark_from_id, created_at, likes, rebarks FROM user_user WHERE bark_id=$barkId"
      .query[Bark]

  def selectBarkById(barkId: BarkId): ConnectionIO[Bark] =
    selectBarkByIdQuery(barkId).unique

  def updateBarkRebarksQuery(barkId: BarkId, rebarks: Rebarks): Update0 =
    sql"UPDATE bark_bark SET rebarks=$rebarks WHERE id=$barkId".update

  def updateBarkRebarks(barkId: BarkId, rebarks: Rebarks): ConnectionIO[Unit] =
    updateBarkRebarksQuery(barkId, rebarks).run.void

  def selectBarksByUserIdQuery(userId: UserId): Query0[Bark] =
    sql"SELECT bark_id, author_id, content, rebark_from_id, created_at, likes, rebarks FROM user_user WHERE author_id=$userId"
      .query[Bark]

  def selectBarksByUserId(userId: UserId): ConnectionIO[List[Bark]] =
    selectBarksByUserIdQuery(userId).to[List]

  override def post(author: UserId, content: String): IO[Bark] =
    val newBark = Bark(BarkId.random(), author, content, None, Instant.now(), Likes(0), Rebarks(0))
    insertBark(newBark).transact(xa)

  override def rebark(author: UserId, sourceBarkId: BarkId, addedContent: String): IO[Bark] =
    (for
      sourceBark <- selectBarkById(sourceBarkId)
      newBark = Bark(BarkId.random(), author, addedContent, sourceBarkId.some, Instant.now(), Likes(0), Rebarks(0))
      _ <- insertBark(newBark)
      _ <- updateBarkRebarks(sourceBarkId, Rebarks(sourceBark.rebarks.value + 1))
    yield newBark).transact(xa)

  override def list(author: UserId): IO[List[Bark]] =
    selectBarksByUserId(author).transact(xa)

object BarkService:
  // Let's have Ref based impl for now
  // as constructing a Ref is an IO by itself, we return IO[BarkService] rather than raw value
  def apply(): IO[BarkService] =
    Ref[IO].of(Map.empty[BarkId, Bark]).map(new BarkServiceRefImpl(_))
