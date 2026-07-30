package org.example.strategy

import mu.KLogging
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SyslogAuthLogParser : AuthLogParser {

    override fun parseSshLogins(
        stringBuilder: StringBuilder,
        lines: List<String>
    ): StringBuilder {
        lines.forEach { line ->
            val shortLine = line.substringAfter("sshd[").substringAfter("]: ")
            val parsedTimestamp = line.split(" ")
                .filter { it.trim().isNotEmpty() }
                .take(RETRIEVE_DATETIME_ARGUMENTS_COUNT)
                .joinToString(" ")
            val localDateTime = convertToLocalTime(parsedTimestamp, ZoneOffset.ofHours(ZONED_OFFSET_HOURS))

            stringBuilder.append("• $localDateTime: $shortLine\n\n")
        }

        return stringBuilder
    }

    override fun predicate(lines: List<String>): Boolean =
        lines.any { line ->
            try {
                val month = line.substring(0, 3)
                Month.entries.any { it.name.take(3).equals(month, ignoreCase = true) }
            } catch (e: Exception) {
                logger.error(e) { "Error while parsing $line" }
                false
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

    companion object : KLogging() {
        const val ZONED_OFFSET_HOURS = 3
        const val RETRIEVE_DATETIME_ARGUMENTS_COUNT = 3
    }
}