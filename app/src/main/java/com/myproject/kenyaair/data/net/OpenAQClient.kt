package com.myproject.kenyaair.data.net

import com.myproject.kenyaair.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object OpenAQClient {
    private const val BASE_URL = "https://api.openaq.org/"

    private val authHeader = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("X-API-Key", BuildConfig.OPENAQ_API_KEY)
            .build()
        chain.proceed(req)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val http = OkHttpClient.Builder()
        .addInterceptor(authHeader)
        .addInterceptor(logging)
        .build()

    // ✅ Moshi with Kotlin support
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // ✅ Use Moshi for JSON
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(http)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
}
