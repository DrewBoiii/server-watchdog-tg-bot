package org.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.example.config.ApplicationConfig
import org.example.dto.DockerContainerDto

class DockerService(
    private val dockerHttpClient: OkHttpClient,
    private val applicationConfig: ApplicationConfig,
) {

    suspend fun getContainers(): List<DockerContainerDto> {
        return try {
            val request = Request.Builder()
                .url("${applicationConfig.dockerApiUrl}/containers/json?all=true")
                .build()

            val response = executeHttpRequest(request)

            Json.decodeFromString<List<DockerContainerDto>>(response.body.string())
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            emptyList()
        }
    }

    suspend fun restartContainerBy(containerName: String): String =
        restartContainer(getContainerIdBy(containerName))

    suspend fun stopContainerBy(containerName: String): String =
        stopContainer(getContainerIdBy(containerName))

    suspend fun restartContainer(id: String): String {
        try {
            val request = Request.Builder()
                .url("${applicationConfig.dockerApiUrl}/containers/$id/restart")
                .post(RequestBody.EMPTY)
                .build()

            executeHttpRequest(request)

            return id
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            throw e
        }
    }

    suspend fun stopContainer(id: String): String {
        try {
            val request = Request.Builder()
                .url("${applicationConfig.dockerApiUrl}/containers/$id/stop")
                .post(RequestBody.EMPTY)
                .build()

            executeHttpRequest(request)

            return id
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            throw e
        }
    }

    private suspend fun getContainerIdBy(name: String): String =
        getContainers().firstOrNull { "/$name" in it.names }?.id
            ?: throw IllegalArgumentException("Container $name not found")

    private suspend fun executeHttpRequest(request: Request): Response = withContext(Dispatchers.IO) {
        val response = dockerHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            logger.error { "Docker API responded with: ${response.code}" }
            throw RuntimeException("Docker API responded with: ${response.code}")
        }

        response
    }

    companion object : KLogging()
}