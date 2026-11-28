package org.example.utils

import org.example.dto.DockerContainerStatsDto

fun getContainerName(containerName: String): String = containerName.removePrefix("/")

fun calculateRamPercentage(stats: DockerContainerStatsDto): Double {
    val cacheBytes = stats.memoryStats.stats?.inactiveFile
        ?: stats.memoryStats.stats?.cache
        ?: 0L
    val usedRamBytes = (stats.memoryStats.usage - cacheBytes).coerceAtLeast(0L)

    return if (stats.memoryStats.limit > 0) {
        (usedRamBytes.toDouble() / stats.memoryStats.limit) * 100.0
    } else 0.0
}

fun calculateCpuPercentage(stats: DockerContainerStatsDto): Double {
    val cpuDelta = stats.cpuStats.cpuUsage.totalUsage - (stats.precpuStats?.cpuUsage?.totalUsage ?: 0L)
    val systemDelta = (stats.cpuStats.systemCpuUsage ?: 0L) - (stats.precpuStats?.systemCpuUsage ?: 0L)
    val onlineCpus = stats.cpuStats.onlineCpus ?: 1

    return if (systemDelta > 0 && cpuDelta > 0) {
        (cpuDelta.toDouble() / systemDelta.toDouble()) * onlineCpus * 100.0
    } else 0.0
}