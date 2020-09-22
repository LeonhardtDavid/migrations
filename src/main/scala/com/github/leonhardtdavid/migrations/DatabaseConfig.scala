package com.github.leonhardtdavid.migrations

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
  * @param files    A list of ups and downs files to apply migrations instead of default configuration.
  */
final class DatabaseConfig(
    val id: String = "default",
    val url: String,
    val user: Option[String] = None,
    val password: Option[String] = None,
    val files: Seq[(String, String)] = Nil) {

  require(
    user.isDefined && password.isDefined || user.isEmpty && password.isEmpty,
    "You must set user and password or leave both empty"
  )

}
