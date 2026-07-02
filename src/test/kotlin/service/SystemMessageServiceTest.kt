package service

import kotlinx.coroutines.runBlocking
import org.example.service.SystemMessageService
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.test.assertEquals

class SystemMessageServiceTest {

    @ParameterizedTest(name = "Uptime: {0}s should format as {1}")
    @MethodSource("uptimeTestCases")
    fun getUptime(
        uptimeSeconds: Long,
        expected: String,
        @TempDir tempDir: Path
    ) = runBlocking {
        val uptimeFile = tempDir.resolve("proc-uptime").toFile()
        val memoryInfoFile = tempDir.resolve("proc-meminfo").toFile()
        val loadAverageFile = tempDir.resolve("proc-loadavg").toFile()

        uptimeFile.writeText(
            """
            $uptimeSeconds.00 2047183.91
        """.trimIndent()
        )

        val service = SystemMessageService(uptimeFile, memoryInfoFile, loadAverageFile)

        val actual = service.getUptime()

        assertEquals(expected, actual)
    }

    companion object {
        @JvmStatic
        fun uptimeTestCases() = listOf(
            Arguments.of(0L, "Server uptime: 0 days 0 hours 0 minutes."),
            Arguments.of(60L, "Server uptime: 0 days 0 hours 1 minutes."),
            Arguments.of(3600L, "Server uptime: 0 days 1 hours 0 minutes."),
            Arguments.of(86400L, "Server uptime: 1 days 0 hours 0 minutes."),
            Arguments.of(90061L, "Server uptime: 1 days 1 hours 1 minutes."),
            Arguments.of(172800L, "Server uptime: 2 days 0 hours 0 minutes."),
            Arguments.of(90000L, "Server uptime: 1 days 1 hours 0 minutes."),
            Arguments.of(3661L, "Server uptime: 0 days 1 hours 1 minutes."),
            Arguments.of(30L * 86400 + 12 * 3600 + 30 * 60, "Server uptime: 30 days 12 hours 30 minutes.")
        )
    }
}