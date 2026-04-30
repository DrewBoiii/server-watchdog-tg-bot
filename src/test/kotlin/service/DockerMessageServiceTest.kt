package service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.example.dto.DockerContainerDto
import org.example.service.DockerMessageService
import org.example.service.DockerMessageService.Companion.EXITED_DOCKER_CONTAINER_STATE
import org.example.service.DockerMessageService.Companion.PAUSED_DOCKER_CONTAINER_STATE
import org.example.service.DockerMessageService.Companion.RUNNING_DOCKER_CONTAINER_STATE
import org.example.service.DockerService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.time.temporal.TemporalField

@ExtendWith(MockKExtension::class)
class DockerMessageServiceTest {

    @MockK
    lateinit var dockerService: DockerService

    @InjectMockKs
    lateinit var dockerMessageService: DockerMessageService

    @Test
    fun `get no docker containers message`() {
        every { dockerService.getContainers() } returns emptyList()

        val activeDockerContainersMessage = dockerMessageService.getActiveDockerContainers()

        assertEquals("No Docker containers", activeDockerContainersMessage)
    }

    @Test
    fun `get docker containers formatted message`() {
        val expected = """
            🐳 Docker containers:

            🟢 `name1`
               • Status: status
               • Uptime: 20571 d. 22 h.
               • Image: image1
            
            ⏸ `name2`
               • Status: status
               • Image: image2
            
            🔴 `name3`
               • Status: status
               • Image: image3
        """.trimIndent()

        every { dockerService.getContainers() } returns activeContainers

        val activeDockerContainersMessage = dockerMessageService.getActiveDockerContainers()

        assertEquals(expected, activeDockerContainersMessage)
    }

    companion object {
        val activeContainers = listOf(
            DockerContainerDto(
                id = "id1",
                names = listOf(
                    "name1",
                ),
                image = "image1",
                state = RUNNING_DOCKER_CONTAINER_STATE,
                status = "status",
                created = 123456L,
            ),
            DockerContainerDto(
                id = "id2",
                names = listOf(
                    "name2",
                ),
                image = "image2",
                state = PAUSED_DOCKER_CONTAINER_STATE,
                status = "status",
                created = 321321321L,
            ),
            DockerContainerDto(
                id = "id3",
                names = listOf(
                    "name3",
                ),
                image = "image3",
                state = EXITED_DOCKER_CONTAINER_STATE,
                status = "status",
                created = 123123123L,
            )
        )
    }

}