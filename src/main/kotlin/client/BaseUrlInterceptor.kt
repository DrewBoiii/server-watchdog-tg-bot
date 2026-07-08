package org.example.client

import okhttp3.Interceptor
import okhttp3.Response
import org.example.service.DockerService.Companion.DEFAULT_BASE_URL

class BaseUrlInterceptor(
    private val baseUrl: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val newUrl = originalUrl.toString().replace(DEFAULT_BASE_URL, baseUrl)

        val requestBuilder = originalRequest.newBuilder()
            .url(newUrl)

        return chain.proceed(requestBuilder.build())
    }
}