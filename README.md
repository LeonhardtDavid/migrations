# sbt-migrations

[![Open Source Love svg1](https://badges.frapsoft.com/os/v1/open-source.svg?v=103)](https://github.com/ellerbrock/open-source-badges/)
[![MIT license](https://img.shields.io/badge/License-MIT-blue.svg)](https://david-leonhardt.mit-license.org/)
[![Download](https://api.bintray.com/packages/leonhardtdavid/sbt-plugins/sbt-migrations/images/download.svg)](https://bintray.com/leonhardtdavid/sbt-plugins/sbt-migrations/_latestVersion)

Simple, but useful [SBT](https://www.scala-sbt.org/) plugin to manage database migrations (some sort of database versioning).

Databases supported:

* Postgresql
* MySQL

# Before start

_**WORK IN PROGRESS**_

I only tested manually for both, Postgresql and MySQL (unit and integration tests remains to be done).
This is a first raw version, so I didn't care much for handle failures or transactions (with commit and rollback),
so it could happen that the database ends up in an inconsistent state and you are going to need to fix it by hand.


# How to use it

## Plugin dependency

It is built for SBT 1.x, not working (for the moment) on version 0.13.  
This is an auto plugin and you need to add the following to your `project/plugins.sbt`:

```sbt
addSbtPlugin("com.github.leonhardtdavid" % "sbt-migrations" % "0.1.1")
```

## Plugin configurations

### Settings

| Setting | Type  | Default | Description |
| ------- | :---: | :-----: | ----------- |
| migrationsPath | String | {resourceDirectory}/migrations | Directory where the migrations are going to be. |
| migrationsTable | String | app_migrations | Table name to keep track of the applied migrations. |
| migrationsConfigs | Seq | -- | No default, so it is required. List of database configurations. Usually it has only one config, but if you have more than one database, you can set multiple configurations. |

NOTE:  
Each value of migrationsConfigs has an id, the default value is "default". This ids must be unique.

### Tasks

There is only one task for the moment, and is used to apply and update the migrations:

```
migratedb
```

### Examples

[Here](/Example) you can find an empty SBT project containing the specifics configurations for this plugins as an example.

```sbt
import com.github.leonhardtdavid.migrations.DatabaseConfig

migrationsConfigs := Seq(
  new DatabaseConfig(
    url = "jdbc:postgresql://localhost/some_schema",
    user = Some("some_user"),
    password = Some("some_password")
  )
)

migrationsConfigs := Seq(
  new DatabaseConfig(url = "jdbc:postgresql://some_user:some_password@localhost/some_schema")
)

migrationsConfigs := Seq(
  new DatabaseConfig(
    id = "animals_database",
    url = "jdbc:postgresql://localhost/animals_schema",
    user = Some("some_user"),
    password = Some("some_password")
  )
)
```

## Setting up the migrations

Inside the directory configured in migrationsPath (usually `src/main/resources/migrations`, or `conf/migrations` in [Play!](https://www.playframework.com/) proyects, or the value you set),
you must create a subfolder that has to be named as the configuration id. So, if I have the default id, I am going to create the following directory: `src/main/resources/migrations/default`,
instead, if I set the id `animals_database`, I am going to create the following directory: `src/main/resources/migrations/animals_database`.

Inside of each of this directories, there will be the migrations as SQL files.  
The nomenclature of the files are as follow:

* Apply scrips:
  - UP_1.sql
  - UP_2.sql
  - ....
  - UP_n.sql
* Rollback scripts:
  - DOWN_1.sql
  - DOWN_2.sql
  - ....
  - DOWN_n.sql

### Required things you need to know:

1. The versions must start from 1.
1. The versions numbers must be consecutive and positive integers (1, 2, 3, 4, ..., n).
2. Apply scripts must start with "UP_" (the plugin is case sensitive, so it should be in upper case), followed by the version number, followed by `.sql` **.
3. Rollback script must start with "DOWN_" (the plugin is case sensitive, so it should be in upper case), followed by the version number, followed by `.sql` **.

** Technically, It could be a dot and what you want, like, `.txt`, `.#%%&.sql`, etc., but I recommend to be `.sql`.
