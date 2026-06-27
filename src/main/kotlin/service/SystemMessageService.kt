package org.example.service

import java.io.File

class SystemMessageService(
    private val uptimeFile: File = UPTIME_FILE,
    private val memoryInfoFile: File = MEMORY_INFO_FILE,
    private val loadAverageFile: File = LOAD_AVERAGE_FILE,
) {

    fun getStatus(): String {
        return """
            📊 Server Status:
            ${getUptime()}
            
            ${getMemoryInfo()}
            
            ${getLoadAverage()}
        """.trimIndent()
    }

    fun getUptime(): String {
        return try {
            val uptimeSeconds = uptimeFile
                .readText()
                .trimIndent()
                .split(" ")
                .first()
                .toDouble()
                .toLong()

            val days = uptimeSeconds / (60 * 60 * 24)
            val hours = (uptimeSeconds / (60 * 60)) % 24
            val minutes = (uptimeSeconds / 60) % 60

            "Server uptime: $days days $hours hours $minutes minutes."
        } catch (e: Exception) {
            "Failed to get server uptime: ${e.message}"
        }
    }

    fun getMemoryInfo(): String {
        return try {
            val memInfo = memoryInfoFile.readLines()
                .associate { line ->
                    val parts = line.split(":")
                    parts[0].trim() to parts[1].trim().split(" ")[0].toLong()
                }

            val total = memInfo["MemTotal"] ?: 0
            val available = memInfo["MemAvailable"] ?: 0
            val used = total - available
            val percent = (used.toDouble() / total * 100).toInt()

            val totalGB = total / (1024 * 1024)
            val usedGB = used / (1024 * 1024)
            val availableGB = available / (1024 * 1024)

            """
            Memory: $usedGB GB / $totalGB GB ($percent%)
            Available: $availableGB GB
        """.trimIndent()
        } catch (e: Exception) {
            "Failed to get memory info: ${e.message}"
        }
    }

    fun getLoadAverage(): String {
        return try {
            val loadavg = loadAverageFile
                .readText()
                .trim()
                .split("\\s+".toRegex())

            val load1min = loadavg[0].toDouble()
            val load5min = loadavg[1].toDouble()
            val load15min = loadavg[2].toDouble()

            val processes = loadavg[3].split("/")
            val runningProcesses = processes[0].toInt()
            val totalProcesses = processes[1].toInt()

            val cpuCores = Runtime.getRuntime().availableProcessors()

            val loadPercent1min = (load1min / cpuCores * 100)
            val loadPercent5min = (load5min / cpuCores * 100)
            val loadPercent15min = (load15min / cpuCores * 100)

            buildString {
                appendLine("System Load Average")
                appendLine()
                appendLine("Load (1m/5m/15m):")
                appendLine("• 1 min:  $load1min (${String.format("%.1f", loadPercent1min)}%)")
                appendLine("• 5 min:  $load5min (${String.format("%.1f", loadPercent5min)}%)")
                appendLine("• 15 min: $load15min (${String.format("%.1f", loadPercent15min)}%)")
                appendLine()
                appendLine("Processes:")
                appendLine("• Running: $runningProcesses")
                appendLine("• Total: $totalProcesses")
                appendLine()
                appendLine("CPU Cores: $cpuCores")

                val status = when {
                    load1min > cpuCores * 0.9 -> "🔴 High load!"
                    load1min > cpuCores * 0.7 -> "🟡 Moderate load"
                    else -> "🟢 Normal"
                }
                appendLine("Status: $status")
            }
        } catch (e: Exception) {
            "Failed to read load average: ${e.message}"
        }
    }

    companion object {
        val UPTIME_FILE = File("/proc/uptime")
        val MEMORY_INFO_FILE = File("/proc/meminfo")
        val LOAD_AVERAGE_FILE = File("/proc/loadavg")
    }
}