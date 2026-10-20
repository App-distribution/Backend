package com.appdist.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val database: DatabaseConfig,
    val storage: StorageConfig,
    val jwt: JwtConfig,
    val otp: OtpConfig,
    val firebase: FirebaseConfig,
    val smtp: SmtpConfig?,
) {
    data class DatabaseConfig(
        val url: String,
        val user: String,
        val password: String,
        val maxPoolSize: Int,
    )
    data class StorageConfig(
        val endpoint: String,
        val publicEndpoint: String,
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
    /** null — SMTP не настроен, коды входа пишутся в лог (только для разработки). */
    data class SmtpConfig(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val from: String,
    ) {
        override fun toString() = "SmtpConfig(host=$host, port=$port, username=$username, from=$from)"
    }

    companion object {
        fun from(config: ApplicationConfig): AppConfig {
            val appConfig = AppConfig(
                database = DatabaseConfig(
                    url = config.property("database.url").getString(),
                    user = config.property("database.user").getString(),
                    password = config.property("database.password").getString(),
                    maxPoolSize = config.property("database.maxPoolSize").getString().toInt(),
                ),
                storage = StorageConfig(
                    endpoint = config.property("storage.endpoint").getString(),
                    publicEndpoint = config.propertyOrNull("storage.publicEndpoint")?.getString()
                        ?: config.property("storage.endpoint").getString(),
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
                smtp = smtpFrom(config),
            )
            appConfig.validate()
            return appConfig
        }

        private fun smtpFrom(config: ApplicationConfig): SmtpConfig? {
            fun opt(key: String) = config.propertyOrNull("smtp.$key")?.getString()?.takeIf { it.isNotBlank() }
            val host = opt("host") ?: return null
            // Наполовину заданный SMTP — почти наверняка опечатка в окружении;
            // лучше упасть на старте, чем молча писать коды в лог на проде.
            fun req(key: String, env: String) = requireNotNull(opt(key)) { "smtp.$key must be set via $env when SMTP_HOST is set" }
            return SmtpConfig(
                host = host,
                port = opt("port")?.toInt() ?: 465,
                username = req("username", "SMTP_USERNAME"),
                password = req("password", "SMTP_PASSWORD"),
                from = opt("from") ?: req("username", "SMTP_USERNAME"),
            )
        }

        private fun AppConfig.validate() {
            require(database.password.isConfiguredSecret()) {
                "database.password must be provided via a local secret value, not a repository placeholder"
            }
            require(storage.accessKey.isConfiguredSecret()) {
                "storage.accessKey must be provided via MINIO_ACCESS_KEY"
            }
            require(storage.secretKey.isConfiguredSecret()) {
                "storage.secretKey must be provided via MINIO_SECRET_KEY"
            }
            require(jwt.secret.isConfiguredSecret()) {
                "jwt.secret must be provided via JWT_SECRET"
            }
            require(jwt.secret.length >= 32) {
                "jwt.secret must be at least 32 characters long"
            }
        }

        private fun String.isConfiguredSecret(): Boolean =
            isNotBlank() && !startsWith("SET_")
    }
}
