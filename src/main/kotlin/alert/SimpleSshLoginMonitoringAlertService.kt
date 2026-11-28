package org.example.alert

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import mu.KLogging
import org.example.sender.MessageSender
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class SimpleSshLoginMonitoringAlertService(
    private val authLogPath: String,
    private val alertChatId: Long,
    private val messageSender: MessageSender,
) {

    suspend fun run() = withContext(Dispatchers.IO) {
        logger.info { "Starting SSH login monitoring alert..." }
        while (isActive) {
            // Объединяем stderr и stdout в один поток (redirectErrorStream = true),
            // чтобы не плодить вторую корутину для чтения ошибок
            val process = ProcessBuilder("tail", "-n", "0", "-F", authLogPath)
                .redirectErrorStream(true)
                .start()

            try {
                process.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        val match = acceptedRegex.find(line)
                        if (match != null) {
                            val (user, ip) = match.destructured
                            try {
                                messageSender.sendMessage(alertChatId, buildMessage(user, ip))
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                logger.error(e) { "Failed to send ssh login monitoring alert." }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                logger.error(e) { "Cancelling SSH login: ${e.message}" }
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Error while reading tail: ${e.message}" }
                delay(5000.milliseconds)
            } finally {
                logger.info { "Process tail was finished" }
                process.destroyForcibly()
                process.waitFor()
            }

            logger.warn { "Process exited without error" }
            delay(5000.milliseconds)
        }
    }

    private fun buildMessage(user: String, ip: String): String =
        buildString {
            appendLine("🔐 Successful login via SSH")
            appendLine("👤 User: $user")
            appendLine("🌐 IP: $ip")
        }

    companion object : KLogging() {
        private val acceptedRegex = Regex("""Accepted\s+\S+\s+for\s+(\S+)\s+from\s+(\S+)""")
    }
}