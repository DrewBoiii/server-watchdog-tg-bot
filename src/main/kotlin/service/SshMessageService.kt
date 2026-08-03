package org.example.service

import mu.KLogging
import org.example.strategy.AuthLogParser

class SshMessageService(
    private val sshService: SshService,
    private val authLogParsers: List<AuthLogParser>,
) {

    suspend fun getLastSuccessSshLogins(): String {
        val lines = sshService.getLastSuccessSshLines(MIN_LINES_COUNT)

        return getSshLoginsMessage("Last Success SSH-logins:\n\n", lines)
    }

    suspend fun getLastFailedSshLogins(): String {
        val lines = sshService.getLastFailedSshLines(MIN_LINES_COUNT)

        return getSshLoginsMessage("Last Failed SSH-logins:\n\n", lines)
    }

    private fun getSshLoginsMessage(title: String, lines: List<String>): String {
        if (lines.isEmpty()) {
            return "File with SSH logs wasn't found."
        }

        val stringBuilder = StringBuilder(title)

        val modifiedStringBuilder = getAuthLogParser(lines).parseSshLogins(stringBuilder, lines)

        return modifiedStringBuilder.toString().trim()
    }

    private fun getAuthLogParser(lines: List<String>): AuthLogParser =
        authLogParsers.firstOrNull { it.predicate(lines) }
            ?: throw IllegalStateException("Auth log parser not found")

    companion object : KLogging() {
        const val MIN_LINES_COUNT = 5
    }
}