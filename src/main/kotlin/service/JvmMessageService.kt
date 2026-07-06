package org.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.management.ManagementFactory

class JvmMessageService {

    suspend fun getJvmStatus(): String {
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory

        val (totalDisk, freeDisk, usedDisk) = withContext(Dispatchers.IO) {
            val root = File("/")
            val total = root.totalSpace / (1024 * 1024 * 1024)
            val free = root.freeSpace / (1024 * 1024 * 1024)
            Triple(total, free, total - free)
        }

        val load = osBean.systemLoadAverage

        return """
            📊 JVM Status:
            Uptime: ${getJvmUptime()} 
            RAM: $usedMemory MB / $totalMemory MB
            Disk: $usedDisk GB / $totalDisk GB (Available $freeDisk GB)
            Load Average: ${if (load >= 0) String.format("%.2f", load) else "N/A"}
            Available Processors: ${osBean.availableProcessors}
        """.trimIndent()
    }

    private fun getJvmUptime(): String {
        val uptimeMillis = ManagementFactory.getRuntimeMXBean().uptime
        val days = uptimeMillis / (1000 * 60 * 60 * 24)
        val hours = (uptimeMillis / (1000 * 60 * 60)) % 24
        val minutes = (uptimeMillis / (1000 * 60)) % 60
        return "$days days $hours hours $minutes minutes."
    }
}