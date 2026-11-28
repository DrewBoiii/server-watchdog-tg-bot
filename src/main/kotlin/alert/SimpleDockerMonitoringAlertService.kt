package org.example.alert

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import mu.KLogging
import org.example.config.ApplicationConfig
import org.example.dto.DockerFormattedStatsDto
import org.example.sender.MessageSender
import org.example.service.DockerService
import org.example.utils.calculateCpuPercentage
import org.example.utils.calculateRamPercentage
import org.example.utils.getContainerName
import kotlin.time.Duration.Companion.milliseconds

class SimpleDockerMonitoringAlertService(
    private val alertChatId: Long,
    private val messageSender: MessageSender,
    private val dockerService: DockerService,
    private val alertConfig: ApplicationConfig.AlertProperties
) {

    private val alertedRamContainers = mutableSetOf<String>()
    private val alertedCpuContainers = mutableSetOf<String>()

    suspend fun run() = withContext(Dispatchers.IO) {
        logger.info { "Starting Docker stats monitoring alert..." }

        while (isActive) {
            try {
                val containerStats = dockerService.getContainerStats()
                    .map {
                        DockerFormattedStatsDto(
                            containerName = getContainerName(it.name),
                            usedRamPercentage = calculateRamPercentage(it),
                            usedCpuPercentage = calculateCpuPercentage(it),
                        )
                    }

                actualizeDockerContainers(containerStats)

                for (stats in containerStats) {
                    // todo chain of rules to check
                    checkMetric(
                        containerName = stats.containerName,
                        currentValue = stats.usedRamPercentage,
                        threshold = alertConfig.ramThresholdPercentage,
                        alertedSet = alertedRamContainers,
                        messageBuilder = { buildCriticalRamUsageMessage(stats) },
                        messageOnRecoveryBuilder = { buildRamRecoveryMessage(stats) }
                    )

                    checkMetric(
                        containerName = stats.containerName,
                        currentValue = stats.usedCpuPercentage,
                        threshold = alertConfig.cpuThresholdPercentage,
                        alertedSet = alertedCpuContainers,
                        messageBuilder = { buildCriticalCpuUsageMessage(stats) },
                        messageOnRecoveryBuilder = { buildCpuRecoveryMessage(stats) }
                    )
                }
            } catch (e: CancellationException) {
                logger.info { "Cancelling Docker stats monitoring..." }
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Error while checking Docker container stats: ${e.message}" }
            }

            logger.debug { "RAM: ${alertedRamContainers.joinToString(separator = ",")}" }
            logger.debug { "CPU ${alertedCpuContainers.joinToString(separator = ",")}" }

            delay(alertConfig.dockerContainerPollingInterval.toMillis().milliseconds)
        }
    }

    private suspend fun checkMetric(
        containerName: String,
        currentValue: Double,
        threshold: Double,
        alertedSet: MutableSet<String>,
        messageBuilder: () -> String,
        messageOnRecoveryBuilder: () -> String,
    ) {
        if (currentValue > threshold) {
            if (alertedSet.add(containerName)) {
                try {
                    messageSender.sendMessage(alertChatId, messageBuilder())
                } catch (e: CancellationException) {
                    alertedSet.remove(containerName)
                    logger.error(e) { "Cancelling message sending on alert..." }
                    throw e
                } catch (e: Exception) {
                    alertedSet.remove(containerName)
                    logger.error(e) { "Failed to send critical docker alert for $containerName" }
                }
            }
        } else {
            if (alertedSet.remove(containerName)) {
                try {
                    messageSender.sendMessage(alertChatId, messageOnRecoveryBuilder())
                } catch (e: CancellationException) {
                    alertedSet.add(containerName)
                    logger.error(e) { "Cancelling message sending on recovery..." }
                    throw e
                } catch (e: Exception) {
                    alertedSet.add(containerName)
                    logger.error(e) { "Failed to send recovery docker alert for $containerName" }
                }
            }
        }
    }

    private fun buildCriticalRamUsageMessage(formattedStats: DockerFormattedStatsDto): String =
        buildString {
            appendLine("🐳 RAM above the threshold for ${formattedStats.containerName}")
            appendLine("• RAM: ${"%.1f".format(formattedStats.usedRamPercentage)}%")
        }

    private fun buildCriticalCpuUsageMessage(formattedStats: DockerFormattedStatsDto): String =
        buildString {
            appendLine("🐳 CPU above the threshold for ${formattedStats.containerName}")
            appendLine("• CPU: ${"%.2f".format(formattedStats.usedCpuPercentage)}%")
        }

    private fun buildRamRecoveryMessage(formattedStats: DockerFormattedStatsDto): String =
        buildString {
            appendLine("🐳 RAM recovered for ${formattedStats.containerName}")
            appendLine("• RAM: ${"%.1f".format(formattedStats.usedRamPercentage)}%")
        }

    private fun buildCpuRecoveryMessage(formattedStats: DockerFormattedStatsDto): String =
        buildString {
            appendLine("🐳 CPU recovered for ${formattedStats.containerName}")
            appendLine("• CPU: ${"%.1f".format(formattedStats.usedCpuPercentage)}%")
        }

    private fun actualizeDockerContainers(containerStats: List<DockerFormattedStatsDto>) =
        containerStats.mapTo(mutableSetOf()) { it.containerName }.let {
            alertedRamContainers.retainAll(it)
            alertedCpuContainers.retainAll(it)
        }

    companion object : KLogging()
}