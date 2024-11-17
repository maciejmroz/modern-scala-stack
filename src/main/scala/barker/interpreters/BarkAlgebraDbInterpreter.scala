package barker.interpreters

import java.time.Instant
import cats.MonadThrow
import cats.syntax.all.*
import cats.effect.IO
import doobie.{ConnectionIO, Query0, Transactor}
import doobie.implicits.*
import barker.entities.*
import doobie.util.update.Update0
import DoobieMappings.given
import barker.algebras.BarkAlgebra
import barker.utilities.*

import java.time.temporal.ChronoUnit

private[interpreters] class BarkAlgebraDbInterpreter(xa: Transactor[IO]) extends BarkAlgebra:
  def insertBarkQuery(bark: Bark): Update0 =
    sql"""INSERT INTO bark_bark(bark_id, author_id, content, rebark_from_id, created_at) 
     VALUES(${bark.id}, ${bark.authorId}, ${bark.content}, ${bark.rebarkFromId}, ${bark.createdAt})""".update

  private def insertBark(bark: Bark): ConnectionIO[Bark] =
    insertBarkQuery(bark).run.as(bark)

  def selectBarkByIdQuery(barkId: BarkId): Query0[Bark] =
    sql"SELECT bark_id, author_id, content, rebark_from_id, created_at, likes, rebarks FROM bark_bark WHERE bark_id=$barkId"
      .query[Bark]

  private def selectBarkById(barkId: BarkId): ConnectionIO[Bark] =
    selectBarkByIdQuery(barkId).option.getOrFailWith(BarkNotFound)

  def updateBarkRebarksQuery(barkId: BarkId, rebarks: Rebarks): Update0 =
    sql"UPDATE bark_bark SET rebarks=$rebarks WHERE bark_id=$barkId".update

  private def updateBarkRebarks(barkId: BarkId, rebarks: Rebarks): ConnectionIO[Unit] =
    updateBarkRebarksQuery(barkId, rebarks).run.void

  def selectBarksByUserIdQuery(userId: UserId): Query0[Bark] =
    sql"SELECT bark_id, author_id, content, rebark_from_id, created_at, likes, rebarks FROM bark_bark WHERE author_id=$userId"
      .query[Bark]

  private def selectBarksByUserId(userId: UserId): ConnectionIO[List[Bark]] =
    selectBarksByUserIdQuery(userId).to[List]

  override def post(author: UserId, content: String): IO[Bark] =
    val newBark =
      Bark(BarkId.random(), author, content, None, Instant.now().truncatedTo(ChronoUnit.MICROS), Likes(0), Rebarks(0))
    insertBark(newBark).transact(xa)

  override def rebark(author: UserId, sourceBarkId: BarkId, addedContent: String): IO[Bark] =
    (for
      sourceBark <- selectBarkById(sourceBarkId)
      newBark = Bark(
        BarkId.random(),
        author,
        addedContent,
        sourceBarkId.some,
        Instant.now().truncatedTo(ChronoUnit.MICROS),
        Likes(0),
        Rebarks(0)
      )
      _ <- insertBark(newBark)
      _ <- updateBarkRebarks(sourceBarkId, Rebarks(sourceBark.rebarks.value + 1))
    yield newBark).transact(xa)

  override def list(author: UserId): IO[List[Bark]] =
    selectBarksByUserId(author).transact(xa)

object BarkAlgebraDbInterpreter:
  def apply(xa: Transactor[IO]): IO[BarkAlgebra] =
    IO(new BarkAlgebraDbInterpreter(xa))
