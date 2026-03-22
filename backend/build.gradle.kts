plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("io.ktor.plugin") version "3.1.1"
}

group = "com.appdist"
version = "0.1.0"

application {
    mainClass.set("com.appdist.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-request-validation-jvm")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:${property("exposed_version")}")
    implementation("org.jetbrains.exposed:exposed-dao:${property("exposed_version")}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${property("exposed_version")}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${property("exposed_version")}")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:${property("hikari_version")}")

    // MinIO
    implementation("io.minio:minio:${property("minio_version")}")

    // JWT
    implementation("com.auth0:java-jwt:${property("java_jwt_version")}")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // APK parsing
    implementation("net.dongliu:apk-parser:2.6.10")

    // Firebase Admin SDK — optional (disabled if FIREBASE_CREDENTIALS_PATH not set)
    implementation("com.google.firebase:firebase-admin:${property("firebase_admin_version")}")

    // Logging
    implementation("ch.qos.logback:logback-classic:${property("logback_version")}")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // Test
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm")
    testImplementation("io.ktor:ktor-client-cio-jvm")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    // Testcontainers for integration tests
    testImplementation("org.testcontainers:testcontainers:${property("testcontainers_version")}")
    testImplementation("org.testcontainers:postgresql:${property("testcontainers_version")}")
}

tasks.test {
    useJUnitPlatform()
}
