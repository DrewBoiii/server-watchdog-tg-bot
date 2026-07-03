package org.example.strategy

import okhttp3.OkHttpClient
import org.example.factory.Ordered

interface DockerHttpClientStrategy : Ordered {

    fun createClient(): OkHttpClient

    fun predicate(osName: String): Boolean

}