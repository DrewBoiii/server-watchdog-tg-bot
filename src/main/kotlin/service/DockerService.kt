package org.example.service

import kotlinx.serialization.json.Json
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.example.dto.DockerContainerDto

class DockerService(
    private val dockerHttpClient: OkHttpClient,
) {

    fun getContainers(): List<DockerContainerDto> {
        return try {
            val request = Request.Builder()
                .url("$DOCKER_API_URL/containers/json?all=true")
                .build()

            val response = executeHttpRequest(request)

            Json.decodeFromString<List<DockerContainerDto>>(response.body.string())
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            emptyList()
        }
    }

    fun restartContainerBy(containerName: String): String =
        restartContainer(getContainerIdBy(containerName))

    fun stopContainerBy(containerName: String): String =
        stopContainer(getContainerIdBy(containerName))

    fun restartContainer(id: String): String {
        try {
            val request = Request.Builder()
                .url("$DOCKER_API_URL/containers/$id/restart")
                .post(RequestBody.EMPTY)
                .build()

            executeHttpRequest(request)

            return id
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            throw e
        }
    }

    fun stopContainer(id: String): String {
        try {
            val request = Request.Builder()
                .url("$DOCKER_API_URL/containers/$id/stop")
                .post(RequestBody.EMPTY)
                .build()

            executeHttpRequest(request)

            return id
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            throw e
        }
    }

    private fun getContainerIdBy(name: String): String =
        getContainers().firstOrNull { "/$name" in it.names }?.id
            ?: throw IllegalArgumentException("Container $name not found")

    private fun executeHttpRequest(request: Request): Response {
        val response = dockerHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            logger.error { "Docker API responded with: ${response.code}" }
            throw RuntimeException("Docker API responded with: ${response.code}")
        }

        return response
    }

    companion object : KLogging() {
        const val DOCKER_API_URL = "http://localhost/v1.41"
    }
}