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

    @ParameterizedTest(name = "Memory: total={0}kB, available={1}kB")
    @MethodSource("memoryTestCases")
    fun getMemoryInfo(
        totalKb: Long,
        availableKb: Long,
        expected: String,
        @TempDir tempDir: Path
    ) = runBlocking {
        val uptimeFile = tempDir.resolve("proc-uptime").toFile()
        val memoryInfoFile = tempDir.resolve("proc-meminfo").toFile()
        val loadAverageFile = tempDir.resolve("proc-loadavg").toFile()

        memoryInfoFile.writeText(
            """
            MemTotal:       $totalKb kB
            MemAvailable:   $availableKb kB
        """.trimIndent()
        )

        val service = SystemMessageService(uptimeFile, memoryInfoFile, loadAverageFile)
        val actual = service.getMemoryInfo()

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

        @JvmStatic
        fun memoryTestCases() = listOf(
            Arguments.of(1048576L, 524288L, """
                Memory: 512 MB / 1024 MB (50%)
                Available: 512 MB
            """.trimIndent()),
            Arguments.of(1024000L, 512000L, """
                Memory: 500 MB / 1000 MB (50%)
                Available: 500 MB
            """.trimIndent()),
            Arguments.of(2048L, 0L, """
                Memory: 2 MB / 2 MB (100%)
                Available: 0 MB
            """.trimIndent()),
            Arguments.of(1024L, 512L, """
                Memory: 0 MB / 1 MB (50%)
                Available: 0 MB
            """.trimIndent()),
            Arguments.of(16384000L, 8192000L, """
                Memory: 8000 MB / 16000 MB (50%)
                Available: 8000 MB
            """.trimIndent())
        )
    }
}