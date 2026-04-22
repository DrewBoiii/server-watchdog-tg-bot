package org.example.handler.impl

import mu.KLogging
import org.example.handler.CommandHandler
import org.example.service.SshLoginsService
import org.example.service.UbuntuSshLoginsService
import org.telegram.telegrambots.meta.api.objects.message.Message
import java.io.File
import java.lang.management.ManagementFactory

class DefaultCommandHandler : CommandHandler {

    private val sshLoginsService: SshLoginsService = UbuntuSshLoginsService()

    override fun handle(message: Message): String {
        val text = message.text ?: ""

        return when (text) {
            "/start" -> "Hi, it's server watchdog bot. Available commands: /status, /uptime, /ssh, /ssh-failed"
            "/status" -> getSystemStatus()
            "/uptime" -> getUptime()
            "/ssh" -> sshLoginsService.getLastSuccessSshLogins(5)
            "/ssh-failed" -> sshLoginsService.getLastSuccessSshLogins(5)
            else -> "Unknown command. Available: /status, /uptime, /ssh, /ssh-failed"
        }
    }

    private fun getUptime(): String {
        val uptimeMillis = ManagementFactory.getRuntimeMXBean().uptime
        val days = uptimeMillis / (1000 * 60 * 60 * 24)
        val hours = (uptimeMillis / (1000 * 60 * 60)) % 24
        val minutes = (uptimeMillis / (1000 * 60)) % 60
        return "⏱ Server uptime: $days days $hours hours $minutes minutes."
    }

    private fun getSystemStatus(): String {
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory

        val root = File("/")
        val totalDisk = root.totalSpace / (1024 * 1024 * 1024)
        val freeDisk = root.freeSpace / (1024 * 1024 * 1024)
        val usedDisk = totalDisk - freeDisk

        val load = osBean.systemLoadAverage

        return """
            📊 **Status VPS**
            💾 **RAM:** $usedMemory MB / $totalMemory MB
            💽 **Disk:** $usedDisk GB / $totalDisk GB (Available $freeDisk GB)
            ⚙️ **Load Average:** ${if (load >= 0) String.format("%.2f", load) else "N/A"}
            ⚙️ **Available Processors:** ${osBean.availableProcessors}
        """.trimIndent()
    }

    companion object: KLogging()
}