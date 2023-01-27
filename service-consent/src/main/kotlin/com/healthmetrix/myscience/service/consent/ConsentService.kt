package com.healthmetrix.myscience.service.consent

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ConsentService {
    @POST("consents/{consentId}/documents")
    @Streaming
    suspend fun configurePdf(
        @Path("consentId")
        consentId: String,
        @Body
        consentOptions: ConsentOptions,
    ): Response<ResponseBody>

    @POST("signatures")
    suspend fun submitPdf(
        @Header("Authorization")
        bearerToken: String,
        @Header("X-Hmx-Success-Redirect-Url")
        successRedirectUrl: String,
        @Header("X-Hmx-Consent-Id")
        consentId: String,
        @Body
        pdf: RequestBody,
    ): SubmitPdfResponse

    @GET("signatures/{documentId}")
    suspend fun fetchPdf(
        @Header("Authorization")
        bearerToken: String,
        @Path("documentId")
        documentId: String,
    ): Response<ResponseBody>

    @Serializable
    data class ConsentOptions(val options: List<ConsentOption>)

    @Serializable
    data class ConsentOption(val optionId: Int, val consented: Boolean)

    @Serializable
    data class SubmitPdfResponse(
        val documentId: String,
        val token: String,
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun ConsentService(
    baseUrl: String,
): ConsentService = Retrofit.Builder().apply {
    baseUrl(baseUrl)
    addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
}.build().create(ConsentService::class.java)
