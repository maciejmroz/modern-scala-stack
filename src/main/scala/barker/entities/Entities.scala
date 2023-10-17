package barker.entities

import java.util.UUID
import java.time.ZonedDateTime

/** Pretty much standard pattern for opaque type:
  *   - define the type itself
  *   - define apply in companion object OR use shortened syntax (def Type(v: Underlying): Type = v)
  *   - define extension method to access underlying
  * You always have access toString method of the underlying (it's JVM), so in case of String wrappers, toString isn't
  * strictly necessary. Extension methods have similar resolution rules to Scala 2 implicits, so they can be defined in
  * a companion object of opaque type (there are some minor advantages to doing so).
  *
  * This is a bit more ceremony than "final case class(value: V) extends AnyVal" from Scala 2 but promises more
  * performance due to avoiding boxing on the JVM. Unfortunately, usage is slightly less convenient.
  */
opaque type Name = String
def Name(value: String): Name = value

opaque type AccessToken = String
def AccessToken(value: String): Name = value

opaque type Likes = Long
object Likes:
  def apply(likes: Long): Likes = likes
  extension (likes: Likes) def value: Long = likes

opaque type Rebarks = Long
object Rebarks:
  def apply(rebarks: Long): Rebarks = rebarks
  extension (rebarks: Rebarks) def value: Long = rebarks

opaque type BarkId = UUID
object BarkId:
  def apply(id: UUID): BarkId = id
  extension (id: BarkId) def value: UUID = id

opaque type UserId = UUID
object UserId:
  def apply(id: UUID): UserId = id
  extension (id: UserId) def value: UUID = id

final case class User(id: UserId, name: Name)

final case class Bark(
    id: BarkId,
    authorId: UserId,
    content: String,
    rebarkFromId: Option[BarkId],
    createdAt: ZonedDateTime,
    likes: Likes,
    rebarks: Rebarks
)

/** Enriched version of the Bark, where entities referenced by IDs are already resolved to actual objects
  *
  * TODO: we may or may not need it ..
  */
final case class RichBark(
    id: BarkId,
    author: User,
    content: String,
    rebarkFrom: Option[Bark],
    createdAt: ZonedDateTime,
    likes: Likes,
    rebarks: Rebarks
)

/** Bark action represents a system event, sequence of which can be used as basis to construct more sophisticated data
  * snapshots
  *
  * TODO: can this be converted to Rust-style enum?
  */
sealed trait BarkAction

object BarkAction:
  final case class Post(barkId: BarkId, createdAt: ZonedDateTime) extends BarkAction
  final case class Like(barkId: BarkId, likedBy: Name, createdAt: ZonedDateTime) extends BarkAction
  final case class Rebark(
      rebarkFrom: BarkId,
      rebarkTo: BarkId,
      createdAt: ZonedDateTime
  ) extends BarkAction
