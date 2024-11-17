package barker.app

import cats.*
import cats.effect.*
import com.zaxxer.hikari.HikariConfig
import doobie.*
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location

object DB:
  def transactor[F[_]: Async](dbConfig: DBConfig): Resource[F, HikariTransactor[F]] =
    for
      hikariConfig <- Resource.pure:
        val config = new HikariConfig()
        config.setDriverClassName(dbConfig.jdbcDriver)
        config.setJdbcUrl(dbConfig.jdbcUrl)
        config.setUsername(dbConfig.username)
        config.setPassword(dbConfig.password)
        config.setMaximumPoolSize(dbConfig.maxConnections)
        config
      xa <- HikariTransactor.fromHikariConfig[F](hikariConfig)
    yield xa

  def runMigrations(dbConfig: DBConfig): IO[Unit] =
    // inspired by https://alexn.org/blog/2020/11/15/managing-database-migrations-scala/
    IO:
      Flyway.configure
        .dataSource(
          dbConfig.jdbcUrl,
          dbConfig.username,
          dbConfig.password
        )
        .group(true)
        .outOfOrder(false)
        .table(dbConfig.migrationsTable)
        .locations(
          dbConfig.migrationsLocations.map(new Location(_))*
        )
        .baselineOnMigrate(true)
        .load()
        .migrate()
    .void
