package com.appdist

import com.appdist.config.AppConfig
import com.appdist.infrastructure.database.DatabaseFactory
import com.appdist.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = AppConfig.from(environment.config)

    DatabaseFactory.init(config.database)

    configureSerialization()
    configureCallLogging()
    configureCORS()
    configureStatusPages()
    configureAuth(config.jwt)
    configureRouting(config)
}
