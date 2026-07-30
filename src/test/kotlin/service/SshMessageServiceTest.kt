package service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.example.service.SshMessageService
import org.example.service.SshService
import org.example.strategy.AuthLogParser
import org.example.strategy.Iso8601AuthLogParser
import org.example.strategy.SyslogAuthLogParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SshMessageServiceTest {

    private val sshServiceMock: SshService = mockk()

    private val syslogParser = SyslogAuthLogParser()
    private val isoParser = Iso8601AuthLogParser()

    private val service = SshMessageService(
        sshService = sshServiceMock,
        authLogParsers = listOf(syslogParser, isoParser)
    )

    @Test
    fun `format success logins via Syslog parser`() = runBlocking {
        val rawLines = listOf(
            "Apr 15 10:22:20 server sshd[1235]: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2",
            "Apr 22 13:28:28 vps-7077 sshd[4813]: Accepted password for root from 158.58.128.103 port 59664 ssh2",
        )

        coEvery { sshServiceMock.getLastSuccessSshLines(any()) } returns rawLines

        val result = service.getLastSuccessSshLogins()

        val expected = """
            Last Success SSH-logins:
            
            • 15.04.2026 13:22:20: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2
            
            • 22.04.2026 16:28:28: Accepted password for root from 158.58.128.103 port 59664 ssh2
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `format success logins via ISO parser`() = runBlocking {
        val rawLines = listOf(
            "2026-07-27T10:30:00.123456+00:00 server sshd-session[123]: Accepted publickey for ubuntu from 192.168.1.100 port 22 ssh2",
            "2026-07-27T11:45:30.654321+00:00 vps-7077 sshd-session[456]: Accepted password for root from 158.58.128.103 port 59664 ssh2"
        )

        coEvery { sshServiceMock.getLastSuccessSshLines(any()) } returns rawLines

        val result = service.getLastSuccessSshLogins()

        val expected = """
            Last Success SSH-logins:
            
            • 27.07.2026 13:30:00: Accepted publickey for ubuntu from 192.168.1.100 port 22 ssh2
            
            • 27.07.2026 14:45:30: Accepted password for root from 158.58.128.103 port 59664 ssh2
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `format failure logins via Syslog parser`() = runBlocking {
        val rawLines = listOf(
            "Apr 22 13:28:26 vps-7077 sshd[4811]: Failed password for root from 2.57.122.191 port 58758 ssh2",
            "Apr 22 13:28:31 vps-7077 sshd[4811]: Failed password for root from 2.57.122.191 port 58758 ssh2",
            "Apr 22 13:28:37 vps-7077 sshd[4811]: message repeated 2 times: [ Failed password for root from 2.57.122.191 port 58758 ssh2]",
            "Apr 22 13:30:02 vps-7077 sshd[4912]: Failed password for ubuntu from 182.253.156.173 port 55910 ssh2",
        )

        coEvery { sshServiceMock.getLastFailedSshLines(any()) } returns rawLines

        val result = service.getLastFailedSshLogins()

        val expected = """
            Last Failed SSH-logins:
            
            • 22.04.2026 16:28:26: Failed password for root from 2.57.122.191 port 58758 ssh2

            • 22.04.2026 16:28:31: Failed password for root from 2.57.122.191 port 58758 ssh2

            • 22.04.2026 16:28:37: message repeated 2 times: [ Failed password for root from 2.57.122.191 port 58758 ssh2]

            • 22.04.2026 16:30:02: Failed password for ubuntu from 182.253.156.173 port 55910 ssh2
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `return not found message when lines are empty`() = runBlocking {
        coEvery { sshServiceMock.getLastSuccessSshLines(any()) } returns emptyList()

        val result = service.getLastSuccessSshLogins()

        assertEquals("File with SSH logs wasn't found.", result)
    }

    @Test
    fun `throws IllegalStateException when no parser matches`() = runBlocking {
        val fakeParser = mockk<AuthLogParser> {
            every { predicate(any()) } returns false
        }
        val serviceWithNoMatch = SshMessageService(sshServiceMock, listOf(fakeParser))
        val rawLines = listOf("some line")

        coEvery { sshServiceMock.getLastSuccessSshLines(any()) } returns rawLines

        val exception = assertThrows<IllegalStateException> {
            serviceWithNoMatch.getLastSuccessSshLogins()
        }

        assertEquals("Auth log parser not found", exception.message)
    }
}