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
  * performance due to avoiding boxing on the JVM. With type companion template traits (UUIDCompanion etc) it's quite
  * convenient to use.
  */
opaque type Name = String
object Name:
  def apply(value: String): Name = value
  extension (id: Name) def value: String = id

opaque type AccessToken = String
object AccessToken extends RandomTokenCompanion[AccessToken]

opaque type Likes = Long
object Likes:
  def apply(likes: Long): Likes = likes
  extension (likes: Likes) def value: Long = likes

opaque type Rebarks = Long
object Rebarks:
  def apply(rebarks: Long): Rebarks = rebarks
  extension (rebarks: Rebarks) def value: Long = rebarks

opaque type BarkId = UUID
object BarkId extends UUIDCompanion[BarkId]

opaque type UserId = UUID
object UserId extends UUIDCompanion[UserId]

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
  */
enum BarkAction:
  case Post(barkId: BarkId, createdAt: ZonedDateTime)
  case Like(barkId: BarkId, likedBy: Name, createdAt: ZonedDateTime)
  case Rebark(rebarkFrom: BarkId, rebarkTo: BarkId, createdAt: ZonedDateTime)
