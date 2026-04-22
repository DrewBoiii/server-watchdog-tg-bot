package org.example.service

import mu.KLogging
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class UbuntuSshLoginsService(
    private val sshLogFile: File = File("/var/log/auth.log"),
) : SshLoginsService {

    override fun getLastSuccessSshLogins(sshLoginCount: Int): String =
        getLastSshLogins(sshLoginCount, "**Last Success SSH-logins:**\n") { line ->
            line.contains("Accepted password") || line.contains("Accepted publickey")
        }

    override fun getLastFailedSshLogins(sshLoginCount: Int): String =
        getLastSshLogins(sshLoginCount, "**Last Failed SSH-logins:**\n") { line ->
            line.contains("Failed password")
        }

    private fun getLastSshLogins(sshLoginCount: Int, header: String, predicate: (String) -> Boolean): String {
        return try {
            if (!sshLogFile.exists()) {
                return "File with SSH logs wasn't found."
            }

            val lines = sshLogFile.readLines()

            val sshLines = lines.filter(predicate).takeLast(sshLoginCount)

            if (sshLines.isEmpty()) {
                "SSH-logins wasn't found."
            } else {
                val stringBuilder = StringBuilder(header)
                sshLines.forEach { line ->
                    val shortLine = line.substringAfter("sshd[").substringAfter("]: ")
                    val parsedTimestamp = line.split(" ").take(3).joinToString(" ")
                    val localDateTime = convertToLocalTime(parsedTimestamp, ZoneOffset.ofHours(3))
                    stringBuilder.append("• $localDateTime: $shortLine\n")
                }
                stringBuilder.toString().trim()
            }
        } catch (e: Exception) {
            "Error during read log SSH: ${e.message}"
        }
    }

    private fun convertToLocalTime(logTimestamp: String, zoneOffset: ZoneOffset): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy MMM d HH:mm:ss")
        val currentYear = LocalDateTime.now().year
        val dateTime = LocalDateTime.parse("$currentYear $logTimestamp", formatter)

        val utcDateTime = dateTime.atZone(ZoneOffset.UTC)
        val localDateTime = utcDateTime.withZoneSameInstant(zoneOffset)

        return localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
    }

    companion object : KLogging()
}