package org.example.service

import mu.KLogging
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.text.replace

class SshMessageService(
    private val sshService: SshService
) {

    fun getLastSuccessSshLogins(): String {
        val lines = sshService.getLastSuccessSshLines(MIN_LINES_COUNT)

        return getSshLoginsMessage("Last Success SSH-logins:\n\n", lines)
    }

    fun getLastFailedSshLogins(): String {
        val lines = sshService.getLastFailedSshLines(MIN_LINES_COUNT)

        return getSshLoginsMessage("Last Failed SSH-logins:\n\n", lines)
    }

    private fun getSshLoginsMessage(title: String, lines: List<String>): String {
        if (lines.isEmpty()) {
            return "File with SSH logs wasn't found."
        }

        val stringBuilder = StringBuilder(title)

        lines.forEach { line ->
            val shortLine = line.substringAfter("sshd[").substringAfter("]: ")
            val parsedTimestamp = line.split(" ")
                .filter { it.trim().isNotEmpty() }
                .take(3)
                .joinToString(" ")
            val localDateTime = convertToLocalTime(parsedTimestamp, ZoneOffset.ofHours(ZONED_OFFSET_HOURS))

            stringBuilder.append("• $localDateTime: $shortLine\n\n")
        }

        return stringBuilder.toString().trim()
    }

    private fun convertToLocalTime(logTimestamp: String, zoneOffset: ZoneOffset): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy MMM d HH:mm:ss")
        val currentYear = LocalDateTime.now().year
        val dateTime = LocalDateTime.parse("$currentYear $logTimestamp", formatter)

        val utcDateTime = dateTime.atZone(ZoneOffset.UTC)
        val localDateTime = utcDateTime.withZoneSameInstant(zoneOffset)

        return localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
    }

    companion object : KLogging() {
        const val MIN_LINES_COUNT = 5
        const val ZONED_OFFSET_HOURS = 3
    }
}