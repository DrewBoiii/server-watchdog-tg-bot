package org.example.factory

import mu.KLogging
import okhttp3.OkHttpClient
import org.example.config.ApplicationConfig
import org.example.strategy.DockerHttpUnixClientStrategy
import org.example.strategy.DockerHttpWindowsClientStrategy

class DockerHttpClientFactory(
    applicationConfig: ApplicationConfig
) {

    private val strategies = listOf(
        DockerHttpWindowsClientStrategy(applicationConfig),
        DockerHttpUnixClientStrategy(applicationConfig),
    ).sortedByDescending { it.order() }

    fun createHttpClient(): OkHttpClient {
        val operationSystemName = System.getProperty(OS_NAME_PROPERTY)

        logger.info { "Creating Docker client for OS: $operationSystemName..." }

        val clientStrategy = strategies.firstOrNull { it.predicate(operationSystemName) }

        if (clientStrategy == null) {
            throw IllegalStateException("Docker client strategy not found: $operationSystemName")
        }

        return clientStrategy.createClient()
    }

    companion object : KLogging() {
        const val OS_NAME_PROPERTY: String = "os.name"
    }
}