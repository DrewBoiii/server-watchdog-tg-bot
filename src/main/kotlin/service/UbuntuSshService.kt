package org.example.service

import mu.KLogging
import java.io.File

class UbuntuSshService(
    private val sshLogFile: File = SSH_LOGS_FILE,
) : SshService {

    override fun getLastSuccessSshLines(sshLoginCount: Int): List<String> =
        getLastSshLogins(sshLoginCount) { line ->
            line.contains("Accepted password") || line.contains("Accepted publickey")
        }

    override fun getLastFailedSshLines(sshLoginCount: Int): List<String> =
        getLastSshLogins(sshLoginCount) { line ->
            line.contains("Failed password")
        }

    private fun getLastSshLogins(sshLoginCount: Int, predicate: (String) -> Boolean): List<String> {
        return try {
            if (!sshLogFile.exists()) {
                logger.error { "File with SSH logs wasn't found" }
                return emptyList()
            }

            val lines = sshLogFile.readLines()

            lines.filter(predicate).takeLast(sshLoginCount)
        } catch (e: Exception) {
            logger.error(e) { "Error during read SSH log: ${e.message}" }
            emptyList()
        }
    }

    companion object : KLogging() {
        val SSH_LOGS_FILE = File("/var/log/auth.log")
    }
}