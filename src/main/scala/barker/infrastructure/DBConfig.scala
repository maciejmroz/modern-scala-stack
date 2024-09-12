package barker.infrastructure

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class DBConfig(
    username: String,
    password: String,
    database: String,
    host: String,
    port: String,
    jdbcUrl: String,
    jdbcDriver: String,
    maxConnections: Int,
    migrationsTable: String,
    migrationsLocations: List[String]
) derives ConfigReader
