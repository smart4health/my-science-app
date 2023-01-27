package com.healthmetrix.myscience.service.deident

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.hl7.fhir.r4.model.Bundle
import org.threeten.bp.ZonedDateTime
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DeidentService {
    @GET("jobs")
    suspend fun getStatus(
        @Query("user")
        userId: String,
        @Header("X-Deident-User-Secret")
        userSecret: String,
    ): StatusResponse

    @POST("downloader/{userId}")
    suspend fun uploadBundle(
        @Path("userId")
        userId: String,
        @Header("X-Deident-Fetched-At")
        fetchedAt: ZonedDateTime,
        @Header("X-Deident-User-Secret")
        userSecret: String, // base64 string
        @Header("X-Deident-D4l-Id")
        d4lId: String, // base64 string
        @Body
        bundle: Bundle,
    )

    @GET("stats")
    suspend fun getStatistics(
        @Query("user")
        userId: String,
        @Header("X-Deident-User-Secret")
        userSecret: String,
    ): StatsResponse
}

@Serializable
data class StatusResponse(
    val lastSuccess: Job?,
    val lastFailure: Job?,
    val firstRejected: Job?,
    val inProgress: List<Job>,
)

@Serializable
data class Job(
    val status: String,
    @Serializable(ZonedDateTimeSerializer::class)
    val createdAt: ZonedDateTime,
    @Serializable(ZonedDateTimeSerializer::class)
    val updatedAt: ZonedDateTime,
)

@Serializable
data class StatsResponse(
    val user: UserStats,
    val global: GlobalStats,
) {
    @Serializable
    data class GlobalStats(
        val usersCount: Int,
        val resourcesUploadedCount: Int,
    )

    @Serializable
    data class UserStats(
        val resourcesUploadedCount: Int,
    )
}

fun DeidentService(
    baseUrl: String,
    bundleConverterFactory: BundleConverterFactory,
    client: OkHttpClient,
    json: StringFormat,
): DeidentService = Retrofit.Builder().apply {
    baseUrl(baseUrl)
    addConverterFactory(bundleConverterFactory)
    addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    client(client)
}.build().create(DeidentService::class.java)
