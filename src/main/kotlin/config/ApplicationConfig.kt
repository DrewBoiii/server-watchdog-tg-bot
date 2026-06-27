package org.example.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.ServerWatchdog
import java.io.File

@Serializable
data class ApplicationConfig(
    @SerialName("docker-api-url")
    val dockerApiUrl: String,
)

object Config {
    private val json = Json { ignoreUnknownKeys = true }

    val config: ApplicationConfig by lazy {
        val externalConfigFile = File("/etc/watchdog-bot/config.json")
        val internalConfigFile = ServerWatchdog::class.java.classLoader.getResourceAsStream("config.json")

        if (externalConfigFile.exists()) {
            try {
                return@lazy json.decodeFromString<ApplicationConfig>(externalConfigFile.readText())
            } catch (e: Exception) {
                println("External config file could not be read: ${e.message}")
            }
        }

        if (internalConfigFile != null) {
            try {
                return@lazy json.decodeFromString<ApplicationConfig>(
                    internalConfigFile.bufferedReader().use { it.readText() }
                )
            } catch (e: Exception) {
                println("Internal config file could not be read: ${e.message}")
            }
        }

        throw IllegalStateException("Config file could not be read")
    }
}