package org.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.use
import org.example.dto.DockerContainerDto
import org.example.dto.DockerContainerStatsDto

class DockerService(
    private val dockerHttpClient: OkHttpClient,
) {

    suspend fun getContainers(all: Boolean = true): List<DockerContainerDto> {
        return try {
            val url = if (all) {
                "$DEFAULT_BASE_URL/containers/json?all=true"
            } else {
                "$DEFAULT_BASE_URL/containers/json"
            }

            val request = Request.Builder()
                .url(url)
                .build()

            executeHttpRequest(request).use { response ->
                Json.decodeFromString<List<DockerContainerDto>>(response.body.string())
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            emptyList()
        }
    }

    suspend fun getActiveContainers(): List<DockerContainerDto> = getContainers(all = false)
        .filter { it.state == RUNNING_DOCKER_CONTAINER_STATE }

    suspend fun restartContainerBy(containerName: String): String =
        restartContainer(getContainerIdBy(containerName))

    suspend fun stopContainerBy(containerName: String): String =
        stopContainer(getContainerIdBy(containerName))

    suspend fun restartContainer(id: String): String {
        try {
            val request = Request.Builder()
                .url("$DEFAULT_BASE_URL/containers/$id/restart")
                .post(RequestBody.EMPTY)
                .build()

            executeHttpRequest(request).use {  }

            return id
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            throw e
        }
    }

    suspend fun stopContainer(id: String): String {
        try {
            val request = Request.Builder()
                .url("$DEFAULT_BASE_URL/containers/$id/stop")
                .post(RequestBody.EMPTY)
                .build()

            executeHttpRequest(request).use {  }

            return id
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            throw e
        }
    }

    suspend fun getContainerStats(): List<DockerContainerStatsDto> {
        try {
            val containerIds = getActiveContainers().map { it.id }

            return withContext(Dispatchers.IO) {
                containerIds.map { async { getContainerStats(it) } }.awaitAll()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during request to Docker API: ${e.message}" }
            throw e
        }
    }

    private suspend fun getContainerStats(id: String): DockerContainerStatsDto {
        return try {
            val request = Request.Builder()
                .url("$DEFAULT_BASE_URL/containers/$id/stats?stream=false")
                .build()

            executeHttpRequest(request).use { response ->
                Json.decodeFromString<DockerContainerStatsDto>(response.body.string())
            }
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
            response.close()
            logger.error { "Docker API responded with: ${response.code}" }
            throw RuntimeException("Docker API responded with: ${response.code}")
        }

        response
    }

    companion object : KLogging() {
        const val DEFAULT_BASE_URL = "http://localhost"
        const val RUNNING_DOCKER_CONTAINER_STATE = "running"
        const val EXITED_DOCKER_CONTAINER_STATE = "exited"
        const val PAUSED_DOCKER_CONTAINER_STATE = "paused"
    }
}