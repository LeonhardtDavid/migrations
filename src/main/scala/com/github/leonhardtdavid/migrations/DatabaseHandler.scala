package com.github.leonhardtdavid.migrations

import java.sql.DriverManager

import sbt.util.Logger

import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
  * Database connections handler for migrations.
  *
  * @param url         Database url.
  * @param credentials Optional user and password.
  * @param table       Table name to persist migrations.
  * @param logger      A sbt Logger instance.
  */
class DatabaseHandler(url: String, credentials: Option[(String, String)], table: String)(implicit logger: Logger) {

  List("com.mysql.cj.jdbc.Driver", "org.postgresql.Driver") foreach { driver =>
    Try(Class.forName(driver))
  }

  private val connection = credentials.fold(
    DriverManager.getConnection(url)
  ) {
    case (user, password) => DriverManager.getConnection(url, user, password)
  }

  /**
    * Initialize the database creating the migrations table if it doesn't exists.
    */
  def initializeDatabase(): Unit =
    this.connection
      .prepareStatement(
        s"""CREATE TABLE IF NOT EXISTS $table
        |(
        |  id INTEGER,
        |  up VARCHAR(4000),
        |  down VARCHAR(4000),
        |  hash VARCHAR(50),
        |  PRIMARY KEY (id)
        |);""".stripMargin
      )
      .execute()

  /**
    * Retrieve migrations from database.
    *
    * @return A sequence of migrations.
    */
  def retrieveMigrations: Seq[Migration] = {
    val buffer    = ListBuffer.empty[Migration]
    val resultSet = this.connection.prepareStatement(s"SELECT * FROM $table ORDER BY id ASC;").executeQuery()

    while (resultSet.next()) {
      buffer += new Migration(
        resultSet.getInt("id"),
        resultSet.getString("up"),
        resultSet.getString("down"),
        resultSet.getString("hash")
      )
    }

    buffer
  }

  /**
    * Updates the database running the migrations.
    *
    * @param migrations        The migrations to apply (ups and down in the correct order).
    * @param updatedMigrations The current migrations to update the database table.
    */
  def applyMigrations(migrations: Seq[String], updatedMigrations: Seq[Migration]): Unit = {
    migrations foreach { migration =>
      logger.info(s"Applying migration:\n$migration")
      this.connection.prepareStatement(migration).execute()
    }

    logger.info("Updating migrations table")

    this.connection.prepareStatement(s"TRUNCATE TABLE $table;").execute()

    val statement = this.connection.prepareStatement(s"INSERT INTO $table(id, up, down, hash) VALUES (?, ?, ?, ?)")

    updatedMigrations foreach { migration =>
      statement.setInt(1, migration.id)
      statement.setString(2, migration.up)
      statement.setString(3, migration.down)
      statement.setString(4, migration.hash)
      statement.addBatch()
    }

    statement.executeBatch()
  }

}
