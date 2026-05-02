package service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.example.dto.DockerContainerDto
import org.example.service.DockerService
import org.example.service.DockerService.Companion.DOCKER_API_URL
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException

@ExtendWith(MockKExtension::class)
class DockerServiceTest {

    @MockK
    lateinit var okHttpClient: OkHttpClient

    @InjectMockKs
    lateinit var service: DockerService

    @Test
    fun `getContainers should return list of containers when response is successful`() {
        val rawJsonResponse = """
            [
                {
                    "Id": "71104ae86fdf",
                    "Names": ["watchdog-bot"],
                    "Image": "drewboiiiiii/server-watchdog-tg-bot:latest",
                    "State": "running",
                    "Status": "Up 2 hours",
                    "Created": 1700000000
                }
            ]
        """.trimIndent()

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(rawJsonResponse.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val mockCall = mockk<Call>()

        every { okHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val response = service.getContainers()

        assertEquals(1, response.size)

        assertEquals("71104ae86fdf", response[0].id)
        assertEquals("watchdog-bot", response[0].names.first())
        assertEquals("drewboiiiiii/server-watchdog-tg-bot:latest", response[0].image)
        assertEquals("running", response[0].state)
        assertEquals("Up 2 hours", response[0].status)
        assertEquals(1700000000, response[0].created)

        verify { okHttpClient.newCall(any()) }
        verify { mockCall.execute() }
    }

    @Test
    fun `getContainers should return empty list when response is failed with 500`() {
        val failedResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Internal Server Error")
            .build()

        val mockCall: Call = mockk()

        every { okHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns failedResponse

        val result = service.getContainers()

        assertEquals(emptyList<DockerContainerDto>(), result)
        verify { mockCall.execute() }
    }

    @Test
    fun `getContainers should return empty list and log error when network exception occurs`() {
        val mockCall: Call = mockk()

        every { okHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } throws IOException("Socket closed")

        val result = service.getContainers()

        assertEquals(emptyList<DockerContainerDto>(), result)

        verify { mockCall.execute() }
    }

    @Test
    fun `getContainers should handle malformed JSON gracefully`() {
        val malformedJson = "{ invalid json "

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(malformedJson.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val mockCall: Call = mockk()
        every { okHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val result = service.getContainers()

        assertEquals(emptyList<DockerContainerDto>(), result)
    }

    @Test
    fun `restartContainerBy should return container id when response is successful`() {
        val containerId = "71104ae86fdf"

        val rawJson = """
        [
            {
                "Id": "$containerId",
                "Names": ["/watchdog-bot"],
                "Image": "drewboiiiiii/server-watchdog-tg-bot:latest",
                "State": "running",
                "Status": "Up 2 hours",
                "Created": 1700000000
            }
        ]
    """.trimIndent()

        val getContainersRequest = Request.Builder()
            .url("$DOCKER_API_URL/containers/json?all=true")
            .build()

        val getContainersResponse = Response.Builder()
            .request(getContainersRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(rawJson.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val restartContainerRequest = Request.Builder()
            .url("$DOCKER_API_URL/containers/$containerId/restart")
            .post(RequestBody.EMPTY)
            .build()

        val restartResponse = Response.Builder()
            .request(restartContainerRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(204)
            .message("No Content")
            .body(ResponseBody.EMPTY)
            .build()

        val getCallMock: Call = mockk()
        val restartCallMock: Call = mockk()

        every {
            okHttpClient.newCall(match { it.url.toString() == "$DOCKER_API_URL/containers/json?all=true" })
        } returns getCallMock

        every { getCallMock.execute() } returns getContainersResponse

        every {
            okHttpClient.newCall(match { it.url.toString() == "$DOCKER_API_URL/containers/$containerId/restart" && it.method == "POST" })
        } returns restartCallMock

        every { restartCallMock.execute() } returns restartResponse

        val result = service.restartContainerBy("watchdog-bot")

        assertEquals(containerId, result)
    }

    @Test
    fun `restartContainerBy should throw exception when container is not found`() {
        val rawJson = """
        [
            {
                "Id": "unknownContainerId",
                "Names": ["/unknown-container-name"],
                "Image": "drewboiiiiii/server-watchdog-tg-bot:latest",
                "State": "running",
                "Status": "Up 2 hours",
                "Created": 1700000000
            }
        ]
    """.trimIndent()

        val getContainersRequest = Request.Builder()
            .url("$DOCKER_API_URL/containers/json?all=true")
            .build()

        val getContainersResponse = Response.Builder()
            .request(getContainersRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(rawJson.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val getCallMock: Call = mockk()

        every {
            okHttpClient.newCall(any())
        } returns getCallMock

        every { getCallMock.execute() } returns getContainersResponse

        val result = assertThrows<IllegalArgumentException> { service.restartContainerBy("watchdog-bot") }

        assertEquals("Container watchdog-bot not found", result.message)
    }

    @Test
    fun `stopContainerBy should return container id when response is successful`() {
        val containerId = "71104ae86fdf"

        val rawJson = """
        [
            {
                "Id": "$containerId",
                "Names": ["/watchdog-bot"],
                "Image": "drewboiiiiii/server-watchdog-tg-bot:latest",
                "State": "running",
                "Status": "Up 2 hours",
                "Created": 1700000000
            }
        ]
    """.trimIndent()

        val getContainersRequest = Request.Builder()
            .url("$DOCKER_API_URL/containers/json?all=true")
            .build()

        val getContainersResponse = Response.Builder()
            .request(getContainersRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(rawJson.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val stopContainerRequest = Request.Builder()
            .url("$DOCKER_API_URL/containers/$containerId/stop")
            .post(RequestBody.EMPTY)
            .build()

        val stopResponse = Response.Builder()
            .request(stopContainerRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(204)
            .message("No Content")
            .body(ResponseBody.EMPTY)
            .build()

        val getCallMock: Call = mockk()
        val restartCallMock: Call = mockk()

        every {
            okHttpClient.newCall(match { it.url.toString() == "$DOCKER_API_URL/containers/json?all=true" })
        } returns getCallMock

        every { getCallMock.execute() } returns getContainersResponse

        every {
            okHttpClient.newCall(match { it.url.toString() == "$DOCKER_API_URL/containers/$containerId/stop" && it.method == "POST" })
        } returns restartCallMock

        every { restartCallMock.execute() } returns stopResponse

        val result = service.stopContainerBy("watchdog-bot")

        assertEquals(containerId, result)
    }

}