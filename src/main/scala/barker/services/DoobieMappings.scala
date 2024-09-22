package barker.services

import barker.entities.{AccessToken, BarkId, Likes, Name, Rebarks, UserId}
import doobie.*
import doobie.postgres.implicits.*

import java.util.UUID

/** Type mappings for basic entity types that allow using these directly in Doobie queries. While it might be tempting
  * to put these in OpaqueCompanion traits, we do not want DB library dependency (or JSON library etc.) at the core of
  * the system. It is slightly more ceremony to add support in the separate place, but we can keep things clean.
  */
object DoobieMappings:
  given Meta[UserId] = Meta[UUID].timap(UserId.apply)(_.value)
  given Meta[Name] = Meta[String].timap(Name.apply)(_.value)
  given Meta[AccessToken] = Meta[String].timap(AccessToken.apply)(_.value)
  given Meta[BarkId] = Meta[UUID].timap(BarkId.apply)(_.value)
  given Meta[Likes] = Meta[Long].timap(Likes.apply)(_.value)
  given Meta[Rebarks] = Meta[Long].timap(Rebarks.apply)(_.value)

  /** export is another new nice thing in Scala 3 that allows us to have something like "all._" imports from Scala 2 but
    * without using object that mixes in bunch of traits.
    */
  export doobie.postgres.implicits.*
