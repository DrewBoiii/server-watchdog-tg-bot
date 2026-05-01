package org.example.service

import kotlinx.serialization.json.Json
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.example.dto.DockerContainerDto

class DockerService(
    private val dockerHttpClient: OkHttpClient,
) {

    fun getContainers(): List<DockerContainerDto> {
        return try {
            val request = Request.Builder()
                .url("$DOCKER_API_URL/containers/json?all=true")
                .build()

            val response = dockerHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                logger.error { "Error during get Docker containers: ${response.code}" }
                return emptyList()
            }

            Json.decodeFromString<List<DockerContainerDto>>(response.body.string())
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            emptyList()
        }
    }

    fun restartContainerBy(containerName: String): String =
        getContainerIdBy(containerName)
            ?.let { containerId -> restartContainer(containerId) }
            ?: throw RuntimeException("Could not restart container")

    fun restartContainer(id: String): String {
        try {
            val request = Request.Builder()
                .url("$DOCKER_API_URL/containers/$id/restart")
                .post(RequestBody.EMPTY)
                .build()

            val response = dockerHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                logger.error { "Docker API responded with: ${response.code}" }
                throw RuntimeException("Error during restart")
            }

            return id
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            throw e
        }
    }

    private fun getContainerIdBy(name: String): String? =
        getContainers().firstOrNull { "/$name" in it.names }?.id

    companion object : KLogging() {
        const val DOCKER_API_URL = "http://localhost/v1.41"
    }
}