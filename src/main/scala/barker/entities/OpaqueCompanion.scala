package barker.entities

import java.security.SecureRandom

import java.util.UUID

/** Inspired by Rust From/Into type classes, although not as nice to use.
  */
trait From[U, V]:
  def from(value: U): V

object From:
  def apply[U, V](using ev: From[U, V]): From[U, V] = ev

trait Into[U, V]:
  def into(value: V): U

object Into:
  def apply[U, V](using ev: Into[U, V]): Into[U, V] = ev

/** An experiment in providing common functionality to opaque types - looks like we can define a trait with both upper
  * and lower bounds of type parameter to the type being aliased by opaque type. This wouldn't make sense in Scala 2,
  * but in Scala 3 opaque types actually meet these bounds (as they _are_ the type). All that remains is deriving
  * companion object of the opaque type from one of these traits.
  */
trait RandomTokenCompanion[T >: String <: String]:
  def apply(id: String): T = id
  def random(): T =
    val secureRandom = SecureRandom.getInstance("NativePRNGNonBlocking")
    val tokenBytes = new Array[Byte](16)
    secureRandom.nextBytes(tokenBytes)
    tokenBytes.map("%02x".format(_)).mkString
  extension (id: T) def value: String = id

trait UUIDCompanion[T >: UUID <: UUID]:
  def apply(id: UUID): T = id
  def random(): T = UUID.randomUUID()
  extension (id: T) def value: UUID = id

  given From[UUID, T] with
    def from(v: UUID): T = v

  given Into[UUID, T] with
    def into(v: T): UUID = v.value
