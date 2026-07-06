package handler.impl

import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.example.dto.TextCommandEnum
import org.example.handler.impl.DefaultCommandMessageHandler
import org.example.service.DockerMessageService
import org.example.service.JvmMessageService
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
    lateinit var jvmMessageService: JvmMessageService

    @MockK
    lateinit var systemMessageService: SystemMessageService

    @MockK
    lateinit var dockerMessageService: DockerMessageService

    @InjectMockKs
    lateinit var handler: DefaultCommandMessageHandler

    @Test
    fun `handle start command`() = runBlocking {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.START.command
        }

        val response = handler.handle(message)

        assertEquals("Hi, it's server watchdog bot. Available commands: $availableCommands", response)
    }

    @Test
    fun `handle unknown command`() = runBlocking {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns "/unknown_command"
        }

        val response = handler.handle(message)

        assertEquals("Unknown command. Available: $availableCommands", response)
    }

    @Test
    fun `handle jvm_status command`() = runBlocking {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.JVM_STATUS.command
        }

        coEvery { jvmMessageService.getJvmStatus() } returns "JVM status"

        val response = handler.handle(message)

        assertEquals("JVM status", response)
    }

    @Test
    fun `handle status command`() = runBlocking {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.STATUS.command
        }

        coEvery { systemMessageService.getStatus() } returns "Server status"

        val response = handler.handle(message)

        assertEquals("Server status", response)
    }

    @Test
    fun `handle ssh command`() = runBlocking {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.SSH.command
        }

        coEvery { sshMessageService.getLastSuccessSshLogins() } returns "Last success ssh logins"

        val response = handler.handle(message)

        assertEquals("Last success ssh logins", response)
    }

    @Test
    fun `handle ssh_failed command`() = runBlocking {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.SSH_FAILED.command
        }

        coEvery { sshMessageService.getLastFailedSshLogins() } returns "Last failed ssh logins"

        val response = handler.handle(message)

        assertEquals("Last failed ssh logins", response)
    }

    @Test
    fun `handle docker_active_services command`() = runBlocking {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.DOCKER_ACTIVE_SERVICES.command
        }

        coEvery { dockerMessageService.getActiveDockerContainers() } returns "Active docker containers"

        val response = handler.handle(message)

        assertEquals("Active docker containers", response)
    }

    @Test
    fun `handle docker_restart_services command`() = runBlocking {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.DOCKER_RESTART_SERVICE.command
        }

        coEvery { dockerMessageService.restartContainer(any()) } returns "Restart docker container"

        val response = handler.handle(message)

        assertEquals("Restart docker container", response)
    }

    @Test
    fun `handle docker_stop_services command`() = runBlocking {
        val message = mockk<Message>(relaxed = true) {
            every { text } returns TextCommandEnum.DOCKER_STOP_SERVICE.command
        }

        coEvery { dockerMessageService.stopContainer(any()) } returns "Stop docker container"

        val response = handler.handle(message)

        assertEquals("Stop docker container", response)
    }

    companion object {
        val availableCommands: String = TextCommandEnum.entries.joinToString(", ") { it.command }
    }
}