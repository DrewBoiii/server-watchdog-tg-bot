package org.example.dto

data class DockerFormattedStatsDto(
    val containerName: String,
    val usedRamPercentage: Double,
    val usedCpuPercentage: Double,
)
