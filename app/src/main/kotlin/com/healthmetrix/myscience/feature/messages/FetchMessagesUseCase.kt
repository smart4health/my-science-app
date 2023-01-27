package com.healthmetrix.myscience.feature.messages

import android.util.Log
import androidx.datastore.core.DataStore
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.myscience.feature.login.usecase.GetPseudonymUseCase
import com.healthmetrix.myscience.service.recontact.MessageId
import com.healthmetrix.myscience.service.recontact.RecontactService
import com.healthmetrix.s4h.myscience.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FetchMessagesUseCase @Inject constructor(
    private val messagesDatabase: Database,
    private val messageSettingsDataStore: DataStore<MessagesSettings>,
    private val messageEventSender: @JvmSuppressWildcards SendChannel<Event>,
    private val messagesLoadingStateMutableStateFlow: MutableStateFlow<MessagesLoadingState>,
    private val recontactService: RecontactService,
    private val getPseudonymUseCase: GetPseudonymUseCase,
) {

    private val mutex = Mutex()

    suspend operator fun invoke(): Result<Int, Error> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                binding<Int, Error> {
                    messagesLoadingStateMutableStateFlow.value = MessagesLoadingState.IN_PROGRESS

                    delay(1000)

                    val pseudonym = getPseudonymUseCase()
                        .first()
                        .toResultOr { Error.NoPseudonym }
                        .bind()

                    val messages = recontactService
                        .runCatching { getNewMessages(citizenId = pseudonym) }
                        .mapError(Error::Retrofit)
                        .bind()
                        .messages
                        .filter {
                            messagesDatabase.messagesQueries.getById(it.id)
                                .executeAsOneOrNull() == null
                        }
                        .map(RecontactService.Message::toDomain)

                    messagesDatabase.messagesQueries.transaction {
                        messages.forEach(messagesDatabase.messagesQueries::insert)
                    }

                    val messagesToBeReported = messagesDatabase
                        .messagesQueries
                        .getAll()
                        .executeAsList()
                        .filter { it.state == MessageState.READ }
                        .map(Messages::id)

                    if (messagesToBeReported.isNotEmpty()) {
                        val res = recontactService
                            .runCatching {
                                markAsRead(
                                    citizenId = pseudonym,
                                    markMessageReadBody = messagesToBeReported.toMarkMessageReadBody(),
                                )
                            }
                            .mapError(Error::Retrofit)
                            .bind()

                        messagesDatabase.messagesQueries.transaction {
                            res.messages.forEach {
                                messagesDatabase.messagesQueries.setState(
                                    MessageState.REPORTED,
                                    it.id,
                                )
                            }
                            res.errors.forEach(messagesDatabase.messagesQueries::deleteById)
                        }
                        messagesLoadingStateMutableStateFlow.value = MessagesLoadingState.READY
                    }
                    messageSettingsDataStore.updateData { messagesSettings ->
                        messagesSettings.toBuilder()
                            .setLastFetchedAt(System.currentTimeMillis())
                            .build()
                    }

                    messages.size
                }.onSuccess {
                    messagesLoadingStateMutableStateFlow.value = MessagesLoadingState.READY
                }.onFailure { e ->
                    Log.e(
                        this@FetchMessagesUseCase::class.simpleName,
                        "Failed to fetch messages: $e",
                    )
                    messageEventSender.send(Event.Failed)
                    messagesLoadingStateMutableStateFlow.value = MessagesLoadingState.FAILED
                }
            }
        }

    sealed class Event {
        object Failed : Event()
    }

    sealed class Error {
        object NoPseudonym : Error()

        data class Retrofit(val t: Throwable) : Error()
    }
}

fun List<MessageId>.toMarkMessageReadBody() =
    RecontactService.MarkMessageReadBody(
        toUpdate = this.map {
            RecontactService.MarkMessageReadBody.MessageItem(
                it,
                RecontactService.MarkMessageReadBody.Action.READ,
            )
        },
    )

fun RecontactService.Message.toDomain() =
    Messages(
        id = id,
        state = MessageState.fromState(state),
        text = content.text,
        title = content.title,
        inserted_at = System.currentTimeMillis(),
        created_at = createdAt.toInstant().toEpochMilli(),
        updated_at = updatedAt?.toInstant()?.toEpochMilli(),
        linked_request = linkedRequest,
        recipient_id = recipientId,
    )
