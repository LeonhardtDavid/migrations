package com.github.leonhardtdavid.migrations

import sbt.Keys._
import sbt._
import sbt.util.Logger

/**
  * SBT auto plugin to handle database migrations.
  */
object MigrationsPlugin extends AutoPlugin {

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

    val handler = new MigrationHandler

    handler.execute(migrationsPath.value, migrationsConfigs.value, migrationsTable.value)
  }

}
