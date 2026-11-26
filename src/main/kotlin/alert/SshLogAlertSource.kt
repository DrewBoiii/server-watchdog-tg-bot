package org.example.alert

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import mu.KLogging
import java.io.IOException
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Слушает auth-лог через `tail -F` и эмитит AlertEvent.SshLoginAlertEvent на каждую строку "Accepted ...".
 *
 * - `-n 0` — флаг нужен чтобы игнорировать старые записи и выдавать только те строки, которые появятся после старта процесса
 * - `-F` — переживает ротацию (--follow=name --retry).
 * - stderr читается отдельной корутиной, чтобы не смешиваться с stdout и не блокировать pipe.
 */
class SshLogAlertSource(
    private val authLogPath: String,
    private val tailReadingRestartCooldownInMillis: Long,
) : AlertSource {

    override fun events(): Flow<AlertEvent> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                tailForever().collect { emit(it) }
                delay(tailReadingRestartCooldownInMillis.milliseconds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "SSH tail crashed, restarting in ${tailReadingRestartCooldownInMillis}ms" }
                delay(tailReadingRestartCooldownInMillis.milliseconds)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun tailForever(): Flow<AlertEvent> = flow {
        logger.info { "Starting tail -F on $authLogPath" }
        val process = ProcessBuilder("tail", "-n", "0", "-F", authLogPath).start()

        // Мгновенно разблокируем read() при отмене корутины
        val handle = currentCoroutineContext().job.invokeOnCompletion {
            process.destroyForcibly()
        }

        try {
            coroutineScope {
                val stderrJob = launch {
                    runCatching {
                        // если не читать stderr то он быстро заполнит буфер пайпа и Linux повесит процесс tail на паузу,
                        // мы никогда не сможем вычитать новые строчки из stdout
                        process.errorStream.bufferedReader().forEachLine { line ->
                            logger.debug { "tail[$authLogPath]: $line" }
                        }
                    }
                }

                try {
                    process.inputStream.bufferedReader().useLines { lines ->
                        for (line in lines) {
                            parseAndFilter(line)?.let { emit(it) }
                        }
                    }
                } catch (e: IOException) {
                    // Если корутину отменили, IOException от закрытого пайпа — это норма
                    currentCoroutineContext().ensureActive()
                    throw e
                } finally {
                    stderrJob.cancel()
                }
            }
        } finally {
            handle.dispose()
            logger.info { "Stopping tail on $authLogPath" }
            process.destroyForcibly()
            process.waitFor() // Не оставляем зомби-процессов в ОС
        }
    }

    private fun parseAndFilter(
        line: String,
    ): AlertEvent.SshLoginAlertEvent? {
        if (!line.contains("Accepted")) {
            return null
        }

        val match = acceptedRegex.find(line) ?: run {
            logger.debug { "Accepted line not parsed: $line" }
            return null
        }

        val user = match.groupValues[1]
        val ip = match.groupValues[2]

        return AlertEvent.SshLoginAlertEvent(user = user, ip = ip, timestamp = Instant.now())
    }

    companion object : KLogging() {
        val acceptedRegex = Regex(
            """Accepted\s+\S+\s+for\s+(\S+)\s+from\s+(\S+)\s+port\s+\d+"""
        )
    }
}