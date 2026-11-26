package org.example.alert

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import mu.KLogging
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Слушает auth-лог через `tail -F` и эмитит AlertEvent.SshLogin
 * на каждую строку "Accepted ...".
 *
 * - `-n 0` — флаг нужен чтобы игнорировать старые записи и выдавать только те строки, которые появятся после старта процесса
 * - `-F` — переживает ротацию (--follow=name --retry).
 * - stderr читается отдельной корутиной, чтобы не смешиваться с stdout и не блокировать pipe.
 * - dedupWindowMillis защищает от дублей sshd (password + publickey на один вход).
 */
class SshLogAlertSource(
    private val authLogPath: String,
    private val dedupWindowMillis: Long,
    private val tailReadingRestartMillis: Long,
) : AlertSource {

    override fun events(): Flow<AlertEvent> = flow {
        while (true) {
            try {
                tailForever().collect { emit(it) }
                // tailForever завершился без исключения (например, файл пропал
                // и retry не сработал) — перезапускаем через паузу.
                delay(tailReadingRestartMillis.milliseconds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "SSH tail crashed, restarting in 5s" }
                delay(tailReadingRestartMillis.milliseconds)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun tailForever(): Flow<AlertEvent> = flow {
        logger.info { "Starting tail -F on $authLogPath" }
        val process = ProcessBuilder("tail", "-n", "0", "-F", authLogPath).start()

        try {
            coroutineScope {
                // stderr — в отдельную корутину: диагностика tail в debug-лог, а не в поток событий.
                val stderrJob = launch {
                    process.errorStream.bufferedReader().forEachLine { line ->
                        logger.debug { "tail[$authLogPath]: $line" }
                    }
                }

                try {
                    process.inputStream.bufferedReader().useLines { lines ->
                        for (line in lines) {
                            parseAndFilter(line)?.let { emit(it) }
                        }
                    }
                } finally {
                    stderrJob.cancel()
                }
            }
        } finally {
            logger.info { "Stopping tail on $authLogPath" }
            process.destroy()
        }
    }

    // Простой LRU-фильтр от дублей, ограничен 200 записями.
    private val recent = object : LinkedHashMap<String, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>) = size > 200
    }

    private fun parseAndFilter(line: String): AlertEvent.SshLoginAlertEvent? {
        if (!line.contains("Accepted")) {
            return null
        }

        val match = acceptedRegex.find(line) ?: run {
            logger.debug { "Accepted line not parsed: $line" }
            return null
        }

        val user = match.groupValues[1]
        val ip = match.groupValues[2]

        val key = "$user@$ip"
        val now = System.currentTimeMillis()
        val prev = recent[key]

        if (prev != null && now - prev < dedupWindowMillis) {
            return null
        }

        recent[key] = now

        return AlertEvent.SshLoginAlertEvent(user = user, ip = ip, timestamp = Instant.now())
    }

    companion object: KLogging() {
        val acceptedRegex = Regex(
            """Accepted\s+\S+\s+for\s+(\S+)\s+from\s+(\S+)\s+port\s+\d+"""
        )
    }
}