package org.example.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
@OptIn(ExperimentalSerializationApi::class)
data class DockerContainerStatsDto(
    val id: String,
    val name: String,
    @SerialName("cpu_stats") val cpuStats: CpuStatsDto,
    @SerialName("precpu_stats") val precpuStats: CpuStatsDto? = null,
    @SerialName("memory_stats") val memoryStats: MemoryStatsDto,
    val networks: Map<String, NetworkStatsDto>? = null,
    @SerialName("pids_stats") val pidsStats: PidsStatsDto? = null,
) {
    @Serializable
    @JsonIgnoreUnknownKeys
    data class CpuStatsDto(
        @SerialName("cpu_usage") val cpuUsage: CpuUsageDto,
        @SerialName("system_cpu_usage") val systemCpuUsage: Long? = null,
        @SerialName("online_cpus") val onlineCpus: Int? = null,
    ) {
        @Serializable
        @JsonIgnoreUnknownKeys
        data class CpuUsageDto(
            @SerialName("total_usage") val totalUsage: Long,
        )
    }

    @Serializable
    @JsonIgnoreUnknownKeys
    data class MemoryStatsDto(
        val usage: Long = 0,
        val limit: Long = 0,
        val stats: MemoryDetailedStatsDto? = null,
    ) {
        @Serializable
        @JsonIgnoreUnknownKeys
        data class MemoryDetailedStatsDto(
            @SerialName("inactive_file") val inactiveFile: Long = 0,
            val cache: Long = 0,
        )
    }

    @Serializable
    @JsonIgnoreUnknownKeys
    data class NetworkStatsDto(
        @SerialName("rx_bytes") val rxBytes: Long = 0,
        @SerialName("tx_bytes") val txBytes: Long = 0,
    )

    @Serializable
    @JsonIgnoreUnknownKeys
    data class PidsStatsDto(
        val current: Int = 0,
        val limit: Int = 0,
    )
}