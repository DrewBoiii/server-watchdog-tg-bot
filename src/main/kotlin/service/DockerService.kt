package org.example.service

import kotlinx.serialization.json.Json
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.example.dto.DockerContainerDto
import org.newsclub.net.unix.AFSocketFactory
import org.newsclub.net.unix.AFUNIXSocketAddress
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

class DockerService {

    private val dockerHttpClient = OkHttpClient.Builder()
        .socketFactory(
            AFSocketFactory.FixedAddressSocketFactory(
                AFUNIXSocketAddress.of(Path.of("/var/run/docker.sock"))
            )
        )
        .build()

    fun getActiveDockerContainers(): String {
        return try {
            val request = Request.Builder()
                .url("http://localhost/v1.41/containers/json?all=true")
                .build()

            val response = dockerHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                logger.error { "Error during get Docker containers: ${response.code}" }
                return "Error during get Docker containers"
            }

            val containers = Json.decodeFromString<List<DockerContainerDto>>(response.body.string())

            if (containers.isEmpty()) {
                return "No Docker containers"
            }

            val sb = StringBuilder("🐳 **Docker containers:**\n\n")
            containers.forEach { container ->
                val name = container.names.firstOrNull()?.removePrefix("/") ?: container.id.take(12)
                val stateEmoji = when (container.state) {
                    RUNNING_DOCKER_CONTAINER_STATE -> "🟢"
                    EXITED_DOCKER_CONTAINER_STATE -> "🔴"
                    PAUSED_DOCKER_CONTAINER_STATE -> "⏸"
                    else -> "❓"
                }
                val uptime = if (container.state == RUNNING_DOCKER_CONTAINER_STATE) {
                    val created = Instant.ofEpochSecond(container.created)
                    val uptimeDuration = Duration.between(created, Instant.now())
                    formatDuration(uptimeDuration)
                } else {
                    container.status
                }

                sb.append("$stateEmoji `$name`\n")
                sb.append("   • Status: ${container.status}\n")
                if (container.state == RUNNING_DOCKER_CONTAINER_STATE) {
                    sb.append("   • Uptime: $uptime\n")
                }
                sb.append("   • Image: ${container.image}\n\n")
            }
            sb.toString().trimEnd()
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            "Error during request to Docker API"
        }
    }

    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()
        return when {
            days > 0 -> "$days дн. $hours ч."
            hours > 0 -> "$hours ч. $minutes мин."
            else -> "$minutes мин."
        }
    }

    companion object : KLogging() {
        const val RUNNING_DOCKER_CONTAINER_STATE = "running"
        const val EXITED_DOCKER_CONTAINER_STATE = "exited"
        const val PAUSED_DOCKER_CONTAINER_STATE = "paused"
    }
}