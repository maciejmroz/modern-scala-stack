package barker.app

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

final case class HTTPConfig(
    host: String,
    port: Int,
    header: String
) derives ConfigReader

final case class AppConfig(db: DBConfig, http: HTTPConfig) derives ConfigReader
