package com.github.leonhardtdavid.migrations

/**
  * Database configuration.
  *
  * Examples: {{{ DatabaseConfig(url = "jdbc:postgresql://admin:admin@localhost/someSchema")
  *
  * DatabaseConfig( id = "users-database", url = "jdbc:mysql://localhost:3306/users", user = Some("admin"), password = Some("1234") ) }}}
  *
  * @param id
  *   Database identifier in configurations, default is "default".
  * @param url
  *   Database url, usually starting with jdbc:{{driver}}:...
  * @param user
  *   Optional database user.
  * @param password
  *   Optional database user password.
  * @param files
  *   A list of ups and downs files to apply migrations instead of default configuration.
  */
final case class DatabaseConfig(
    id: String = "default",
    url: String,
    user: Option[String] = None,
    password: Option[String] = None,
    files: Seq[(String, String)] = Nil
)
