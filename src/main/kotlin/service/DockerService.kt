package org.example.service

import kotlinx.serialization.json.Json
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.example.dto.DockerContainerDto

class DockerService(
    private val dockerHttpClient: OkHttpClient,
) {

    fun getContainers(): List<DockerContainerDto> {
        return try {
            val request = Request.Builder()
                .url("http://localhost/v1.41/containers/json?all=true")
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

    companion object : KLogging()
}