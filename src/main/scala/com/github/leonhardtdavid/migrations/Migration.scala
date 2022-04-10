package com.github.leonhardtdavid.migrations

import org.apache.commons.codec.digest.DigestUtils

/**
  * Represents a migration.
  *
  * @param id
  *   Migration identifier/index.
  * @param up
  *   Migration apply.
  * @param down
  *   Migration rollback.
  * @param hash
  *   Hash generated from up and down.
  */
final class Migration(val id: Int, val up: String, val down: String, val hash: String) {

  /**
    * Constructor for [[com.github.leonhardtdavid.migrations.Migration]] that generates the hash from the up and down.
    *
    * @param id
    *   Migration identifier/index.
    * @param up
    *   Migration apply.
    * @param down
    *   Migration rollback.
    */
  def this(id: Int, up: String, down: String) = this(id, up, down, DigestUtils.md5Hex(up + down))

}
