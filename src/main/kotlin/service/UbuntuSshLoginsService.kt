package org.example.service

import mu.KLogging
import java.io.File

class UbuntuSshLoginsService(
    private val sshLogFile: File = File("/var/log/auth.log"),
): SshLoginsService {

    override fun getLastSuccessSshLogins(sshLoginCount: Int): String {
        return try {
            if (!sshLogFile.exists()) {
                return "File with SSH logs wasn't found."
            }

            val lines = sshLogFile.readLines()

            val sshLines = lines.filter {
                it.contains("Accepted password") || it.contains("Accepted publickey")
            }.takeLast(sshLoginCount)

            if (sshLines.isEmpty()) {
                "SSH-logins wasn't found."
            } else {
                val stringBuilder = StringBuilder("**Last SSH-logins:**\n")
                sshLines.forEach { line ->
                    val shortLine = line.substringAfter("sshd[").substringAfter("]: ")
                    val timestamp = line.split(" ").take(3).joinToString(" ")
                    stringBuilder.append("• $timestamp: $shortLine\n")
                }
                stringBuilder.toString().trim()
            }
        } catch (e: Exception) {
            "Error during read log SSH: ${e.message}"
        }
    }

    override fun getLastFailedSshLogins(sshLoginCount: Int): String {
        return try {
            if (!sshLogFile.exists()) {
                return "File with SSH logs wasn't found."
            }

            val lines = sshLogFile.readLines()

            val sshLines = lines.filter {
                it.contains("Failed password")
            }.takeLast(sshLoginCount)

            if (sshLines.isEmpty()) {
                "SSH-logins wasn't found."
            } else {
                val stringBuilder = StringBuilder("**Last Failed SSH-logins:**\n")
                sshLines.forEach { line ->
                    val shortLine = line.substringAfter("sshd[").substringAfter("]: ")
                    val timestamp = line.split(" ").take(3).joinToString(" ")
                    stringBuilder.append("• $timestamp: $shortLine\n")
                }
                stringBuilder.toString().trim()
            }
        } catch (e: Exception) {
            "Error during read log SSH: ${e.message}"
        }
    }

    companion object : KLogging()
}