package com.github.leonhardtdavid.migrations

import java.io.File

import sbt.Keys._
import sbt._
import sbt.util.Logger

/**
  * SBT auto plugin to handle database migrations.
  */
object MigrationsPlugin extends AutoPlugin {

  private val default = "default"
  private val up      = "UP_"
  private val down    = "DOWN_"

  /**
    * Database configuration.
    *
    * Examples: {{{
    * new DatabaseConfig(url = "jdbc:postgresql://admin:admin@localhost/someSchema")
    *
    * new DatabaseConfig(
    *   id = "users-database",
    *   url = "jdbc:mysql://localhost:3306/users",
    *   user = Some("admin"),
    *   password = Some("1234")
    * )
    * }}}
    *
    * @param id       Database identifier in configurations, default is "default".
    * @param url      Database url, usually starting with jdbc:{{driver}}:...
    * @param user     Optional database user.
    * @param password Optional database user password.
    */
  final class DatabaseConfig(
      val id: String = default,
      val url: String,
      val user: Option[String] = None,
      val password: Option[String] = None) {

    require(
      user.isDefined && password.isDefined || user.isEmpty && password.isEmpty,
      "You must set user and password or leave both empty"
    )

  }

  /**
    * auto import definitions.
    */
  object autoImport {
    val migrationsPath    = settingKey[String]("Path to migrations files")
    val migrationsTable   = settingKey[String]("Table name for migrations")
    val migrationsConfigs = settingKey[Seq[DatabaseConfig]]("Database configurations")

    val migratedb = taskKey[Unit]("Apply migrations in database")
  }

  import autoImport._

  override def requires: Plugins = sbt.plugins.JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  override val projectSettings = Seq(
    migrationsPath := ((Compile / resourceDirectory).value / "migrations").getAbsolutePath,
    migrationsTable := "app_migrations",
    migratedb := migratedbTask.value
  )

  lazy val migratedbTask = Def.task[Unit] {
    implicit val logger: Logger = streams.value.log

    logger.info("Starting migration...")
    logger.info(s"Taking files from ${migrationsPath.value}")

    this.createConnectionAndListFiles(migrationsConfigs.value, migrationsTable.value, migrationsPath.value) foreach {
      case (dbHandler, migrations) =>
        val dbMigrations = dbHandler.retrieveMigrations

        logger.info(s"Migrations for ${migrations.headOption.map(_.id).getOrElse("- Nothing to apply -")}")
        logger.info(s"Migrations on disk: ${migrations.length} / Migrations on database: ${dbMigrations.length}")

        val migrationsToRun =
          if (migrations.length >= dbMigrations.length) {
            this.listSQLsToApply(migrations, dbMigrations)
          } else {
            val (sameLength, remaining) = dbMigrations.splitAt(migrations.length)
            remaining.map(_.down) ++ this.listSQLsToApply(migrations, sameLength)
          }

        dbHandler.applyMigrations(migrationsToRun, migrations)
    }

    logger.info("Migration ended")
  }

  private def createConnectionAndListFiles(
      migrationsConfigs: Seq[DatabaseConfig],
      migrationsTable: String,
      migrationsPath: String
    )(implicit logger: Logger
    ): Seq[(DatabaseHandler, Seq[Migration])] =
    migrationsConfigs.zipWithIndex.map {
      case (dbConfig, index) =>
        val maybeCredentials = for {
          user     <- dbConfig.user
          password <- dbConfig.password
        } yield user -> password

        val handler = new DatabaseHandler(dbConfig.url, maybeCredentials, migrationsTable)
        handler.initializeDatabase()

        val migrationsDirectory = new File(migrationsPath + File.separator + dbConfig.id)

        logger.info(s"Migration $index: ${migrationsDirectory.getAbsolutePath}")

        if (!migrationsDirectory.isDirectory) throw new MigrationException(s"$migrationsDirectory is not a directory")

        val migrations = this.findMigrationsFiles(migrationsDirectory)

        handler -> migrations
    }

  private def findMigrationsFiles(migrationsDirectory: File)(implicit logger: Logger): Seq[Migration] = {
    val (ups, downs) = migrationsDirectory
      .listFiles()
      .filter { file =>
        val name = file.getName.toUpperCase

        logger.info(s"Found: $name | Is a file? ${file.isFile}")

        file.isFile && (name.startsWith(up) || name.startsWith(down))
      }
      .partition(_.name.startsWith(up))

    if (ups.length != downs.length) throw new MigrationException("The number of UP files and DOWN files is different")

    sort(ups, up).zipWithIndex zip sort(downs, down) map {
      case (((indexUp, up), index), (indexDown, down)) =>
        val expectedIndex = index + 1

        if (expectedIndex != indexUp) {
          throw new MigrationException(s"Expected index $expectedIndex not found")
        } else if (indexUp != indexDown) {
          throw new MigrationException(s"Some UP or DOWN file is missing, trying to compare $indexUp and $indexDown")
        }

        new Migration(indexUp, this.file2String(up), this.file2String(down))
    }
  }

  private def sort(array: Array[File], prefix: String) =
    array
      .map { file =>
        val name  = file.name
        val index = name.substring(prefix.length, name.indexOf('.')).toInt

        index -> file
      }
      .sortBy(_._1)

  private def file2String(file: File): String = {
    val source  = scala.io.Source.fromFile(file)
    val content = source.getLines().mkString("\n").trim

    source.close()

    content
  }

  private def listSQLsToApply(migrations: Seq[Migration], dbMigrations: Seq[Migration]): Seq[String] =
    this
      .findMigrationIdFromWhereToApply(migrations, dbMigrations)
      .map { id =>
        val downs =
          if (id <= dbMigrations.length) {
            dbMigrations.drop(id - 1).map(_.down)
          } else {
            Nil
          }

        downs ++ migrations.drop(id - 1).map(_.up)
      }
      .getOrElse(Nil)
      .flatMap(_.split(';'))

  private def findMigrationIdFromWhereToApply(migrations: Seq[Migration], dbMigrations: Seq[Migration]): Option[Int] = {
    val maybeId = migrations
      .zip(dbMigrations)
      .find {
        case (migration, dbMigration) => migration.hash != dbMigration.hash
      }
      .map(_._1.id)

    maybeId match {
      case None if migrations.length > dbMigrations.length => Some(dbMigrations.length + 1)
      case option                                          => option
    }
  }

}
