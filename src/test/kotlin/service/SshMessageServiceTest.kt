package service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.example.service.SshMessageService
import org.example.service.SshService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SshMessageServiceTest {

    @MockK
    lateinit var sshServiceMock: SshService

    @InjectMockKs
    lateinit var sshMessageService: SshMessageService

    @Test
    fun `return not found message when lines are empty`() {
        every { sshServiceMock.getLastSuccessSshLines(any()) } returns emptyList()

        val result = sshMessageService.getLastSuccessSshLogins()
        assertEquals("File with SSH logs wasn't found.", result)
    }

    @Test
    fun `format success logins correctly`() {
        val rawLines = listOf(
            "Apr 15 10:22:20 server sshd[1235]: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2",
            "Apr 22 13:28:28 vps-7077 sshd[4813]: Accepted password for root from 158.58.128.103 port 59664 ssh2",
        )

        every { sshServiceMock.getLastSuccessSshLines(any()) } returns rawLines

        val result = sshMessageService.getLastSuccessSshLogins()

        val expected = """
            Last Success SSH-logins:
            
            • 15.04.2026 13:22:20: Accepted publickey for ubuntu from 192.168.1.100 port 52143 ssh2
            
            • 22.04.2026 16:28:28: Accepted password for root from 158.58.128.103 port 59664 ssh2
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `format failure logins correctly`() {
        val rawLines = listOf(
            "Apr 22 13:28:26 vps-7077 sshd[4811]: Failed password for root from 2.57.122.191 port 58758 ssh2",
            "Apr 22 13:28:31 vps-7077 sshd[4811]: Failed password for root from 2.57.122.191 port 58758 ssh2",
            "Apr 22 13:28:37 vps-7077 sshd[4811]: message repeated 2 times: [ Failed password for root from 2.57.122.191 port 58758 ssh2]",
            "Apr 22 13:30:02 vps-7077 sshd[4912]: Failed password for ubuntu from 182.253.156.173 port 55910 ssh2",
        )

        every { sshServiceMock.getLastFailedSshLines(any()) } returns rawLines

        val result = sshMessageService.getLastFailedSshLogins()

        val expected = """
            Last Failed SSH-logins:
            
            • 22.04.2026 16:28:26: Failed password for root from 2.57.122.191 port 58758 ssh2

            • 22.04.2026 16:28:31: Failed password for root from 2.57.122.191 port 58758 ssh2

            • 22.04.2026 16:28:37: message repeated 2 times: [ Failed password for root from 2.57.122.191 port 58758 ssh2]

            • 22.04.2026 16:30:02: Failed password for ubuntu from 182.253.156.173 port 55910 ssh2
        """.trimIndent()

        assertEquals(expected, result)
    }
}