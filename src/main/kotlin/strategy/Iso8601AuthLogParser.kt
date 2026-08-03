package org.example.strategy

import mu.KLogging
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Iso8601AuthLogParser : AuthLogParser {

    override fun parseSshLogins(
        stringBuilder: StringBuilder,
        lines: List<String>
    ): StringBuilder {
        val zoneOffset = ZoneOffset.ofHours(ZONED_OFFSET_HOURS)

        lines.forEach { line ->
            val message = line.substringAfter("]: ", "")

            if (message.isEmpty()) {
                logger.warn { "Could not extract message from line: $line" }
                return@forEach
            }

            val timestamp = line.substringBefore(" ")
            val localDateTime = convertToLocalTime(timestamp, zoneOffset)

            stringBuilder.append("• $localDateTime: $message\n\n")
        }

        return stringBuilder
    }

    override fun predicate(lines: List<String>): Boolean =
        lines.any { line ->
            line.length >= 10 && line[4] == '-' && line.take(4).all { it.isDigit() }
        }

    private fun convertToLocalTime(isoTimestamp: String, zoneOffset: ZoneOffset): String {
        return try {
            val offsetDateTime = OffsetDateTime.parse(isoTimestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            offsetDateTime.atZoneSameInstant(zoneOffset)
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse ISO timestamp: '$isoTimestamp'" }
            isoTimestamp
        }
    }

    companion object : KLogging() {
        const val ZONED_OFFSET_HOURS = 3
    }
}