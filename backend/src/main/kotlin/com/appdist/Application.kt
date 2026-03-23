package com.appdist

import com.appdist.config.AppConfig
import com.appdist.infrastructure.database.DatabaseFactory
import com.appdist.plugins.*
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.netty.*
import java.io.FileInputStream

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = AppConfig.from(environment.config)

    DatabaseFactory.init(config.database)

    // Firebase init — optional, disabled if credentials path not set
    initFirebase(config.firebase.credentialsPath)

    configureSerialization()
    configureCallLogging()
    configureCORS()
    configureStatusPages()
    configureAuth(config.jwt)
    configureRouting(config)
}

fun initFirebase(credentialsPath: String?) {
    if (credentialsPath == null) {
        log.warn { "FIREBASE_CREDENTIALS_PATH not set — push notifications disabled" }
        return
    }
    if (FirebaseApp.getApps().isNotEmpty()) return   // already initialized
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(FileInputStream(credentialsPath)))
        .build()
    FirebaseApp.initializeApp(options)
    log.info { "Firebase initialized" }
}
