package com.appdist.config

import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SmtpConfigTest {
    private fun config(vararg smtp: Pair<String, String>) = MapApplicationConfig(
        "database.url" to "jdbc:h2:mem:x",
        "database.user" to "u",
        "database.password" to "p",
        "database.maxPoolSize" to "1",
        "storage.endpoint" to "http://minio:9000",
        "storage.accessKey" to "a",
        "storage.secretKey" to "s",
        "storage.bucket" to "b",
        "jwt.secret" to "x".repeat(32),
        "jwt.issuer" to "i",
        "jwt.audience" to "a",
        "jwt.accessTokenTtlMinutes" to "60",
        "jwt.refreshTokenTtlDays" to "30",
        "otp.ttlMinutes" to "5",
        "otp.length" to "6",
        *smtp.map { (k, v) -> "smtp.$k" to v }.toTypedArray(),
    )

    @Test
    fun `no SMTP_HOST means codes go to the log`() {
        assertNull(AppConfig.from(config()).smtp)
    }

    @Test
    fun `full SMTP settings are parsed with port 465 and sender defaulting to login`() {
        val smtp = AppConfig.from(
            config("host" to "smtp.mail.ru", "username" to "appdist.noreply@mail.ru", "password" to "app-pass")
        ).smtp!!
        assertEquals("smtp.mail.ru", smtp.host)
        assertEquals(465, smtp.port)
        assertEquals("appdist.noreply@mail.ru", smtp.from)
    }

    // Наполовину заданный SMTP на проде тихо вернул бы коды в лог.
    @Test
    fun `host without password fails at startup`() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.from(config("host" to "smtp.mail.ru", "username" to "appdist.noreply@mail.ru"))
        }
    }

    @Test
    fun `password never leaks through toString`() {
        val smtp = AppConfig.from(
            config("host" to "smtp.mail.ru", "username" to "u@mail.ru", "password" to "super-secret")
        ).smtp!!
        assertFalse("super-secret" in smtp.toString())
    }
}
