package org.example.strategy

import okhttp3.OkHttpClient
import org.example.config.ApplicationConfig
import org.example.client.BaseUrlInterceptor
import org.newsclub.net.unix.AFSocketFactory
import org.newsclub.net.unix.AFUNIXSocketAddress
import java.nio.file.Path

class DockerHttpUnixClientStrategy(
    applicationConfig: ApplicationConfig
) : DockerHttpClientStrategy {

    private val unixClient = applicationConfig.docker.unixClient

    override fun createClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(unixClient.connectTimeout)
            .readTimeout(unixClient.readTimeout)
            .writeTimeout(unixClient.writeTimeout)
            .socketFactory(
                socketFactory = AFSocketFactory.FixedAddressSocketFactory(
                    AFUNIXSocketAddress.of(Path.of(unixClient.socketPath))
                )
            )
            .addInterceptor(BaseUrlInterceptor(unixClient.apiUrl))
            .build()

    override fun predicate(osName: String): Boolean =
        osName.contains("Unix", ignoreCase = true)
                || osName.contains("Linux", ignoreCase = true)


    override fun order(): Int = 1

}