package com.appdist.config

data class AppConfig(
    val database: DatabaseConfig,
) {
    data class DatabaseConfig(
        val url: String,
        val user: String,
        val password: String,
        val maxPoolSize: Int = 10,
    )
}
