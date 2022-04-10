import com.github.leonhardtdavid.migrations.DatabaseConfig

name := "migrations-example"
organization := "com.github.leonhardtdavid"
scalaVersion := "3.1.1"

lazy val root = project in file(".")

migrationsConfigs := Seq(
  new DatabaseConfig(
    id = "animals_database",
    url = "jdbc:mysql://localhost/animals_schema",
    user = Some("some_user"),
    password = Some("some_password")
  )
)
