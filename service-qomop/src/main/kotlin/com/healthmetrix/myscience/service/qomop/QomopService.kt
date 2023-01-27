package com.healthmetrix.myscience.service.qomop

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface QomopService {

    @POST("tracer/data-category/{categoryId}")
    suspend fun traceCodingForCategory(
        @Path("categoryId")
        categoryId: Int,
        @Body
        tracingBody: TracingBody,
    ): TracingResponse

    @Serializable
    data class TracingBody(
        val system: String,
        val code: String,
    )

    @Serializable
    data class TracingResponse(
        val match: Boolean,
        val message: String? = null,
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun QomopService(
    baseUrl: String,
    okHttpClient: OkHttpClient,
): QomopService = Retrofit.Builder().apply {
    baseUrl(baseUrl)
    client(okHttpClient)
    addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
}.build().create(QomopService::class.java)
