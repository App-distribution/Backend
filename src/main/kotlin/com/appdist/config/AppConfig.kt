package com.appdist.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val database: DatabaseConfig,
    val storage: StorageConfig,
    val jwt: JwtConfig,
    val otp: OtpConfig,
    val firebase: FirebaseConfig,
) {
    data class DatabaseConfig(
        val url: String,
        val user: String,
        val password: String,
        val maxPoolSize: Int,
    )
    data class StorageConfig(
        val endpoint: String,
        val accessKey: String,
        val secretKey: String,
        val bucket: String,
    )
    data class JwtConfig(
        val secret: String,
        val issuer: String,
        val audience: String,
        val accessTokenTtlMinutes: Long,
        val refreshTokenTtlDays: Long,
    )
    data class OtpConfig(
        val ttlMinutes: Long,
        val length: Int,
    )
    data class FirebaseConfig(
        val credentialsPath: String?,
    )

    companion object {
        fun from(config: ApplicationConfig) = AppConfig(
            database = DatabaseConfig(
                url = config.property("database.url").getString(),
                user = config.property("database.user").getString(),
                password = config.property("database.password").getString(),
                maxPoolSize = config.property("database.maxPoolSize").getString().toInt(),
            ),
            storage = StorageConfig(
                endpoint = config.property("storage.endpoint").getString(),
                accessKey = config.property("storage.accessKey").getString(),
                secretKey = config.property("storage.secretKey").getString(),
                bucket = config.property("storage.bucket").getString(),
            ),
            jwt = JwtConfig(
                secret = config.property("jwt.secret").getString(),
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                accessTokenTtlMinutes = config.property("jwt.accessTokenTtlMinutes").getString().toLong(),
                refreshTokenTtlDays = config.property("jwt.refreshTokenTtlDays").getString().toLong(),
            ),
            otp = OtpConfig(
                ttlMinutes = config.property("otp.ttlMinutes").getString().toLong(),
                length = config.property("otp.length").getString().toInt(),
            ),
            firebase = FirebaseConfig(
                credentialsPath = config.propertyOrNull("firebase.credentialsPath")?.getString(),
            ),
        )
    }
}
