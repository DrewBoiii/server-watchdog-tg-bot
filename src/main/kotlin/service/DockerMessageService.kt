package org.example.service

import mu.KLogging
import org.example.dto.DockerContainerDto
import org.example.dto.TextCommandEnum.DOCKER_RESTART_SERVICE
import java.time.Duration
import java.time.Instant

class DockerMessageService(
    private val dockerService: DockerService
) {

    fun getActiveDockerContainers(): String {
        val containers = dockerService.getContainers()

        if (containers.isEmpty()) {
            return "No Docker containers"
        }

        val sb = StringBuilder("🐳 Docker containers:\n\n")

        containers.forEach { container ->
            val name = getContainerName(container)

            val stateEmoji = getStateEmoji(container)

            val uptime = getUptime(container)

            sb.append("$stateEmoji `$name`\n")
            sb.append("   • Status: ${container.status}\n")
            if (container.state == RUNNING_DOCKER_CONTAINER_STATE) {
                sb.append("   • Uptime: $uptime\n")
            }
            sb.append("   • Image: ${container.image}\n")
            sb.append(
                "   • Restart: ${DOCKER_RESTART_SERVICE.command} ${
                    container.names.firstOrNull()?.removePrefix("/")
                }\n"
            )
            sb.append("\n")
        }

        return sb.toString().trimEnd()
    }

    fun restartContainer(containerName: String?): String {
        return try {
            if (containerName == null) {
                return "No Docker container name provided"
            }

            val containerId = dockerService.restartContainerBy(containerName)
            "Container $containerName was restarted"
        } catch (e: Exception) {
            logger.error(e) { "Error during restart container $containerName: ${e.message}" }
            "Error during restart container $containerName"
        }
    }

    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()
        return when {
            days > 0 -> "$days d. $hours h."
            hours > 0 -> "$hours h. $minutes m."
            else -> "$minutes m."
        }
    }

    private fun getContainerName(container: DockerContainerDto): String =
        container.names.firstOrNull()?.removePrefix("/") ?: container.id.take(12)

    private fun getStateEmoji(container: DockerContainerDto): String =
        when (container.state) {
            RUNNING_DOCKER_CONTAINER_STATE -> "🟢"
            EXITED_DOCKER_CONTAINER_STATE -> "🔴"
            PAUSED_DOCKER_CONTAINER_STATE -> "⏸"
            else -> "❓"
        }

    private fun getUptime(container: DockerContainerDto): String =
        if (container.state == RUNNING_DOCKER_CONTAINER_STATE && container.created != null) {
            val created = Instant.ofEpochSecond(container.created)
            val uptimeDuration = Duration.between(created, Instant.now())
            formatDuration(uptimeDuration)
        } else {
            container.status
        }

    companion object : KLogging() {
        const val RUNNING_DOCKER_CONTAINER_STATE = "running"
        const val EXITED_DOCKER_CONTAINER_STATE = "exited"
        const val PAUSED_DOCKER_CONTAINER_STATE = "paused"
    }
}