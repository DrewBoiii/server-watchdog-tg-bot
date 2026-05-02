package handler.impl

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.example.dto.TextCommandEnum
import org.example.handler.impl.DefaultCommandMessageHandler
import org.example.service.DockerMessageService
import org.example.service.SshMessageService
import org.example.service.SystemMessageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.telegram.telegrambots.meta.api.objects.message.Message

@ExtendWith(MockKExtension::class)
class DefaultCommandMessageHandlerTest {

    @MockK
    lateinit var sshMessageService: SshMessageService

    @MockK
    lateinit var systemMessageService: SystemMessageService

    @MockK
    lateinit var dockerMessageService: DockerMessageService

    @InjectMockKs
    lateinit var handler: DefaultCommandMessageHandler

    @Test
    fun `handle start command`() {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.START.command
        }

        val response = handler.handle(message)

        assertEquals("Hi, it's server watchdog bot. Available commands: $availableCommands", response)
    }

    @Test
    fun `handle unknown command`() {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns "/unknown_command"
        }

        val response = handler.handle(message)

        assertEquals("Unknown command. Available: $availableCommands", response)
    }

    @Test
    fun `handle status command`() {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.STATUS.command
        }

        every { systemMessageService.getSystemStatus() } returns "System status"

        val response = handler.handle(message)

        assertEquals("System status", response)
    }

    @Test
    fun `handle uptime command`() {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.UPTIME.command
        }

        every { systemMessageService.getUptime() } returns "System uptime"

        val response = handler.handle(message)

        assertEquals("System uptime", response)
    }

    @Test
    fun `handle ssh command`() {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.SSH.command
        }

        every { sshMessageService.getLastSuccessSshLogins() } returns "Last success ssh logins"

        val response = handler.handle(message)

        assertEquals("Last success ssh logins", response)
    }

    @Test
    fun `handle ssh_failed command`() {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.SSH_FAILED.command
        }

        every { sshMessageService.getLastFailedSshLogins() } returns "Last failed ssh logins"

        val response = handler.handle(message)

        assertEquals("Last failed ssh logins", response)
    }

    @Test
    fun `handle docker_active_services command`() {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.DOCKER_ACTIVE_SERVICES.command
        }

        every { dockerMessageService.getActiveDockerContainers() } returns "Active docker containers"

        val response = handler.handle(message)

        assertEquals("Active docker containers", response)
    }

    @Test
    fun `handle docker_restart_services command`() {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.DOCKER_RESTART_SERVICE.command
        }

        every { dockerMessageService.restartContainer(any()) } returns "Restart docker container"

        val response = handler.handle(message)

        assertEquals("Restart docker container", response)
    }

    @Test
    fun `handle docker_stop_services command`() {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.DOCKER_STOP_SERVICE.command
        }

        every { dockerMessageService.stopContainer(any()) } returns "Stop docker container"

        val response = handler.handle(message)

        assertEquals("Stop docker container", response)
    }

    companion object {
        val availableCommands: String = TextCommandEnum.entries.joinToString(", ") { it.command }
    }
}