package org.example.service

import mu.KLogging
import org.example.dto.DockerContainerDto
import org.example.dto.DockerContainerStatsDto
import org.example.dto.TextCommandEnum.DOCKER_RESTART_SERVICE
import org.example.dto.TextCommandEnum.DOCKER_STOP_SERVICE
import org.example.service.DockerService.Companion.EXITED_DOCKER_CONTAINER_STATE
import org.example.service.DockerService.Companion.PAUSED_DOCKER_CONTAINER_STATE
import org.example.service.DockerService.Companion.RUNNING_DOCKER_CONTAINER_STATE
import java.time.Duration
import java.time.Instant

class DockerMessageService(
    private val dockerService: DockerService
) {

    suspend fun getAllDockerContainers(): String {
        val containers = dockerService.getContainers()

        if (containers.isEmpty()) {
            return "No Docker containers"
        }

        val sb = StringBuilder("🐳 Docker containers:\n\n")

        containers.forEach { container ->
            val name = getContainerName(container)
            val stateEmoji = getStateEmoji(container)
            val uptime = getUptime(container)

            sb.append("$stateEmoji $name\n")
            sb.append("   • Status: ${container.status}\n")
            if (container.state == RUNNING_DOCKER_CONTAINER_STATE) {
                sb.append("   • Uptime: $uptime\n")
            }
            sb.append("   • Image: ${container.image}\n")
            sb.append("   • Restart: ${DOCKER_RESTART_SERVICE.command} $name\n")
            sb.append("   • Stop: ${DOCKER_STOP_SERVICE.command} $name\n")
            sb.append("\n")
        }

        return sb.toString().trimEnd()
    }

    suspend fun restartContainer(containerName: String?): String {
        return try {
            if (containerName == null) {
                return "No Docker container name provided"
            }

            dockerService.restartContainerBy(containerName)
            "Container $containerName was restarted"
        } catch (e: Exception) {
            logger.error(e) { "Error during restart container $containerName: ${e.message}" }
            "Error during restart container $containerName"
        }
    }

    suspend fun stopContainer(containerName: String?): String {
        return try {
            if (containerName == null) {
                return "No Docker container name provided"
            }

            if (containerName.contains(UNSTOPPABLE_CONTAINER_NAME_PATTERN, ignoreCase = true)) {
                return "You can't stop this Docker container: $containerName"
            }

            dockerService.stopContainerBy(containerName)
            "Container $containerName was stopped"
        } catch (e: Exception) {
            logger.error(e) { "Error during stop container $containerName: ${e.message}" }
            "Error during stop container $containerName"
        }
    }

    suspend fun statsContainers(): String {
        return try {
            val stats = dockerService.getContainerStats()
            if (stats.isEmpty()) {
                return "No active containers found"
            }

            val formattedStats = stats
                .sortedBy { it.name }
                .joinToString("\n\n") { formatContainerStat(it) }

            "📊 Containers stats:\n\n$formattedStats"
        } catch (e: Exception) {
            logger.error(e) { "Error during retrieve containers stats: ${e.message}" }
            "Error during retrieve containers stats"
        }
    }

    private fun formatContainerStat(stat: DockerContainerStatsDto): String {
        val cleanName = stat.name.removePrefix("/")

        // RAM
        val cacheBytes = stat.memoryStats.stats?.inactiveFile
            ?: stat.memoryStats.stats?.cache
            ?: 0L
        val usedRamBytes = (stat.memoryStats.usage - cacheBytes).coerceAtLeast(0L)
        val usedRamMb = usedRamBytes / (1024.0 * 1024.0)
        val limitRamMb = stat.memoryStats.limit / (1024.0 * 1024.0)
        val ramPercent = if (stat.memoryStats.limit > 0) {
            (usedRamBytes.toDouble() / stat.memoryStats.limit) * 100.0
        } else 0.0

        // CPU
        val cpuDelta = stat.cpuStats.cpuUsage.totalUsage - (stat.precpuStats?.cpuUsage?.totalUsage ?: 0L)
        val systemDelta = (stat.cpuStats.systemCpuUsage ?: 0L) - (stat.precpuStats?.systemCpuUsage ?: 0L)
        val onlineCpus = stat.cpuStats.onlineCpus ?: 1
        val cpuPercent = if (systemDelta > 0 && cpuDelta > 0) {
            (cpuDelta.toDouble() / systemDelta.toDouble()) * onlineCpus * 100.0
        } else 0.0

        // Network
        val rxMb = (stat.networks?.values?.sumOf { it.rxBytes } ?: 0L) / (1024.0 * 1024.0)
        val txMb = (stat.networks?.values?.sumOf { it.txBytes } ?: 0L) / (1024.0 * 1024.0)

        return buildString {
            appendLine("🐳 $cleanName")
            appendLine("• CPU: ${"%.2f".format(cpuPercent)}%")
            appendLine("• RAM: ${"%.1f".format(usedRamMb)} / ${"%.0f".format(limitRamMb)} MiB (${"%.1f".format(ramPercent)}%)")
            append("• Net: ↓ ${"%.1f".format(rxMb)} MiB / ↑ ${"%.1f".format(txMb)} MiB")
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
        const val UNSTOPPABLE_CONTAINER_NAME_PATTERN = "watchdog"
    }
}