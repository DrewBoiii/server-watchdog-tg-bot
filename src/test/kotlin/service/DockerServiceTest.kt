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
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.example.dto.DockerContainerDto
import org.example.service.DockerService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException

@ExtendWith(MockKExtension::class)
class DockerServiceTest {

    @MockK
    lateinit var mockClient: OkHttpClient

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

        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val response = service.getContainers()

        assertEquals(1, response.size)

        assertEquals("71104ae86fdf", response[0].id)
        assertEquals("watchdog-bot", response[0].names.first())
        assertEquals("drewboiiiiii/server-watchdog-tg-bot:latest", response[0].image)
        assertEquals("running", response[0].state)
        assertEquals("Up 2 hours", response[0].status)
        assertEquals(1700000000, response[0].created)

        verify { mockClient.newCall(any()) }
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

        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns failedResponse

        val result = service.getContainers()

        assertEquals(emptyList<DockerContainerDto>(), result)
        verify { mockCall.execute() }
    }

    @Test
    fun `getContainers should return empty list and log error when network exception occurs`() {
        val mockCall: Call = mockk()

        every { mockClient.newCall(any()) } returns mockCall
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
        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val result = service.getContainers()

        assertEquals(emptyList<DockerContainerDto>(), result)
    }

}