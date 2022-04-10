package com.github.leonhardtdavid.migrations

/**
  * Custom exception for migrations.
  *
  * @param message
  *   Error message.
  */
class MigrationException(message: String) extends RuntimeException(message)
