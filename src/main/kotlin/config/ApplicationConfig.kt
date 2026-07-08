package org.example.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.ServerWatchdog
import org.example.config.serializer.JavaTimeDurationSerializer
import java.io.File
import java.time.Duration

@Serializable
data class ApplicationConfig(
    @SerialName("docker")
    val docker: DockerProperties,
    @SerialName("system")
    val system: SystemProperties,
) {
    @Serializable
    data class DockerProperties(
        @SerialName("unix-client")
        val unixClient: UnixClientProperties,
        @SerialName("windows-client")
        val windowsClient: WindowsClientProperties,
    ) {

        @Serializable
        data class UnixClientProperties(
            @SerialName("api-url")
            val apiUrl: String,
            @SerialName("connect-timeout")
            @Serializable(with = JavaTimeDurationSerializer::class)
            val connectTimeout: Duration,
            @SerialName("read-timeout")
            @Serializable(with = JavaTimeDurationSerializer::class)
            val readTimeout: Duration,
            @SerialName("write-timeout")
            @Serializable(with = JavaTimeDurationSerializer::class)
            val writeTimeout: Duration,
            @SerialName("socket-path")
            val socketPath: String,
        )

        @Serializable
        data class WindowsClientProperties(
            @SerialName("api-url")
            val apiUrl: String,
            @SerialName("connect-timeout")
            @Serializable(with = JavaTimeDurationSerializer::class)
            val connectTimeout: Duration,
            @SerialName("read-timeout")
            @Serializable(with = JavaTimeDurationSerializer::class)
            val readTimeout: Duration,
            @SerialName("write-timeout")
            @Serializable(with = JavaTimeDurationSerializer::class)
            val writeTimeout: Duration,
        )
    }

    @Serializable
    data class SystemProperties(
        @SerialName("ssh-auth-log-file-path")
        val sshAuthLogFilePath: String,
        @SerialName("ram-information-file-path")
        val ramInfoFilePath: String,
        @SerialName("uptime-file-path")
        val uptimeFilePath: String,
        @SerialName("load-average-file-path")
        val loadAverageFilePath: String,
    )
}

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