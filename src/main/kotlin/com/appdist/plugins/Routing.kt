package com.appdist.plugins

import com.appdist.config.AppConfig
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(config: AppConfig) {
    routing {
        route("/api/v1") {
            // Routes will be wired in subsequent tasks
        }
    }
}
