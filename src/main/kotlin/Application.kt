package ru.planner

import io.ktor.server.application.*
import ru.planner.db.configureDatabases
import ru.planner.routes.configureRouting
import ru.planner.security.configureSecurity

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureSecurity()
    configureRouting()
}
