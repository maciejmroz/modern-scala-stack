package barker.entities

import java.util.UUID
import java.time.Instant

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

opaque type Likes = Int
object Likes:
  def apply(likes: Int): Likes = likes
  extension (likes: Likes) def value: Int = likes

opaque type Rebarks = Int
object Rebarks:
  def apply(rebarks: Int): Rebarks = rebarks
  extension (rebarks: Rebarks) def value: Int = rebarks

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
    createdAt: Instant,
    likes: Likes,
    rebarks: Rebarks
)

trait AppError(val errorCode: Long) extends Throwable with Product
case object InvalidAccessToken extends AppError(1728229358)
case object UserNotFound extends AppError(1728229501)
case object BarkNotFound extends AppError(1731792724)
