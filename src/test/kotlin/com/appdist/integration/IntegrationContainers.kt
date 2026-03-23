package com.appdist.integration

import com.appdist.config.AppConfig
import com.appdist.infrastructure.database.DatabaseFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import java.time.Duration

/**
 * Singleton that holds shared Testcontainers instances for integration tests.
 * Containers are started lazily on first access and reused across all test classes
 * within the same JVM process.
 */
object IntegrationContainers {

    val postgres: PostgreSQLContainer<Nothing> by lazy {
        PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("appdist_test")
            withUsername("appdist")
            withPassword("appdist_test")
            start()
        }
    }

    val minio: GenericContainer<Nothing> by lazy {
        GenericContainer<Nothing>("minio/minio:latest").apply {
            withEnv("MINIO_ROOT_USER", "minioadmin")
            withEnv("MINIO_ROOT_PASSWORD", "test-minio-password")
            withCommand("server", "/data")
            withExposedPorts(9000)
            waitingFor(HttpWaitStrategy().forPath("/minio/health/live").withStartupTimeout(Duration.ofSeconds(30)))
            start()
        }
    }

    val minioEndpoint: String get() = "http://${minio.host}:${minio.getMappedPort(9000)}"

    val dbConfig: AppConfig.DatabaseConfig get() = AppConfig.DatabaseConfig(
        url = postgres.jdbcUrl,
        user = postgres.username,
        password = postgres.password,
        maxPoolSize = 5,
    )

    val storageConfig: AppConfig.StorageConfig get() = AppConfig.StorageConfig(
        endpoint = minioEndpoint,
        accessKey = "minioadmin",
        secretKey = "test-minio-password",
        bucket = "appdist-test",
    )

    /**
     * Initializes DatabaseFactory with the shared PostgreSQL container.
     * Safe to call multiple times — DatabaseFactory.init will reconnect.
     */
    fun initDatabase() {
        DatabaseFactory.init(dbConfig)
    }
}
