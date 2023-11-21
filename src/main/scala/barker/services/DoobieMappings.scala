package barker.services

import barker.entities.{AccessToken, Name, UserId}
import doobie.*
import doobie.postgres.implicits.*

import java.util.UUID

/** Type mappings for basic entity types that allow using these directly in Doobie queries. While it might be tempting
  * to put these in OpaqueCompanion traits, we do not want DB library dependency (or JSON library etc) at the core of
  * the system. It is slightly more ceremony to add support in the separate place but we can keep things clean.
  */
object DoobieMappings:
  given Meta[UserId] = Meta[UUID].timap(UserId.apply)(_.value)
  given Meta[Name] = Meta[String].timap(Name.apply)(_.value)
  given Meta[AccessToken] = Meta[String].timap(AccessToken.apply)(_.value)
