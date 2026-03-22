package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object OtpCodesTable : Table("otp_codes") {
    val email = varchar("email", 255)
    val code = varchar("code", 10)
    val expiresAt = timestamp("expires_at")
    val used = bool("used")
    override val primaryKey = PrimaryKey(email)
}
