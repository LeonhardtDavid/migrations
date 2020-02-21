package com.github.leonhardtdavid.migrations

import java.io.File

import sbt._
import sbt.util.Logger

/**
  * Migrations handler.
  *
  * @param logger A sbt Logger instance.
  */
class MigrationHandler(implicit logger: Logger) {

  private val up   = "UP_"
  private val down = "DOWN_"

  /**
    * Run the migrations with the given parameters.
    *
    * @param migrationsPath    Path to migrations files.
    * @param migrationsConfigs Database configurations.
    * @param migrationsTable   Table name for migrations.
    */
  def execute(migrationsPath: String, migrationsConfigs: Seq[DatabaseConfig], migrationsTable: String): Unit = {
    logger.info("Starting migration...")
    logger.info(s"Taking files from $migrationsPath")

    val ids = migrationsConfigs.map(_.id)
    if (ids.toSet.size != ids.size) {
      throw new MigrationException("Configurations ids must be unique")
    }

    this.createConnectionAndListFiles(migrationsConfigs, migrationsTable, migrationsPath) foreach {
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

  private def findMigrationsFiles(migrationsDirectory: File): Seq[Migration] = {
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
