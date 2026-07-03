package org.example.strategy

import okhttp3.OkHttpClient
import org.example.config.ApplicationConfig
import org.example.client.BaseUrlInterceptor

class DockerHttpWindowsClientStrategy(
    applicationConfig: ApplicationConfig
) : DockerHttpClientStrategy {

    private val windowsClient = applicationConfig.docker.windowsClient

    override fun createClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(windowsClient.connectTimeout)
            .readTimeout(windowsClient.readTimeout)
            .writeTimeout(windowsClient.writeTimeout)
            .addInterceptor(BaseUrlInterceptor(windowsClient.apiUrl))
            .build()

    override fun predicate(osName: String): Boolean =
        osName.contains("Windows", ignoreCase = true)

    override fun order(): Int = 2

}