package com.healthmetrix.myscience.service.recontact

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.threeten.bp.ZonedDateTime
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

typealias CitizenId = String
typealias MessageId = String
typealias RequestId = String

private const val CITIZEN_ID_HEADER = "X-Recontact-Citizen-Id"

interface RecontactService {
    @GET("messages")
    suspend fun getNewMessages(
        @Header(CITIZEN_ID_HEADER)
        citizenId: CitizenId,
        @Query("state")
        stateParam: Message.State? = Message.State.CREATED,
    ): GetMessagesResponse

    @POST("messages")
    suspend fun markAsRead(
        @Header(CITIZEN_ID_HEADER)
        citizenId: CitizenId,
        @Body
        markMessageReadBody: MarkMessageReadBody,
    ): MarkMessageReadResponse

    @Serializable
    data class GetMessagesResponse(
        val messages: List<Message>,
    )

    @Serializable
    data class MarkMessageReadBody(
        val toUpdate: List<MessageItem>,
    ) {
        @Serializable
        data class MessageItem(val messageId: MessageId, val action: Action)

        enum class Action {
            READ,
        }
    }

    @Serializable
    data class Message(
        val id: MessageId,
        val linkedRequest: RequestId,
        @Serializable(ZonedDateTimeSerializer::class)
        val createdAt: ZonedDateTime,
        @Serializable(ZonedDateTimeSerializer::class)
        val updatedAt: ZonedDateTime?,
        val content: Content,
        val recipientId: CitizenId,
        val state: State,
    ) {
        @Serializable
        data class Content(
            val text: String,
            val title: String,
        )

        @Serializable(StateSerializer::class)
        enum class State {
            CREATED, DELIVERED, READ, UNKNOWN
        }
    }

    @Serializable
    data class MarkMessageReadResponse(
        val messages: List<Message>,
        val errors: List<MessageId>,
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun RecontactService(
    baseUrl: String,
    okHttpClient: OkHttpClient,
): RecontactService = Retrofit.Builder().apply {
    baseUrl(baseUrl)
    client(okHttpClient)
    addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
}.build().create(RecontactService::class.java)
