package com.healthmetrix.myscience.feature.messages

import androidx.datastore.core.DataStore
import com.healthmetrix.myscience.feature.login.usecase.GetPseudonymUseCase
import com.healthmetrix.myscience.service.recontact.RecontactService
import com.healthmetrix.s4h.myscience.Database
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

private const val PSEUDONYM = "123"

class FetchMessagesUseCaseTest {

    private lateinit var messagesDatabase: Database
    private val messageSettingsDataStore: DataStore<MessagesSettings> = mockk(relaxed = true)
    private val messageEventSender: SendChannel<FetchMessagesUseCase.Event> = mockk()
    private val messagesLoadingStateMutableStateFlow: MutableStateFlow<MessagesLoadingState> =
        MutableStateFlow(MessagesLoadingState.READY)
    private val recontactService: RecontactService = mockk()
    private lateinit var underTest: FetchMessagesUseCase

    private val getPseudonymUseCase: GetPseudonymUseCase = mockk {
        every { this@mockk.invoke() } returns flow {
            emit(PSEUDONYM)
        }
    }

    @BeforeEach
    fun init() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            Database.Schema.create(it)
            messagesDatabase = Database(
                it,
                messagesAdapter = Messages.Adapter(stateAdapter = EnumColumnAdapter()),
            )
        }

        underTest = FetchMessagesUseCase(
            messagesDatabase,
            messageSettingsDataStore,
            messageEventSender,
            messagesLoadingStateMutableStateFlow,
            recontactService,
            getPseudonymUseCase,
        )
    }

    @Test
    fun `fetch messages with no new messages available`() {
        runBlocking {
            coEvery {
                recontactService.getNewMessages(
                    PSEUDONYM,
                    RecontactService.Message.State.CREATED,
                )
            } returns RecontactService.GetMessagesResponse(listOf())

            underTest.invoke()

            assertThat(messagesLoadingStateMutableStateFlow.first()).isEqualTo(MessagesLoadingState.READY)
            coVerify { messageEventSender wasNot called }
        }
    }

    @Test
    fun `fetch messages successfully and mark one read`() {
        val unreadMessage = mockMessage(id = "1", state = RecontactService.Message.State.DELIVERED)
        val readMessage = mockMessage(id = "2", state = RecontactService.Message.State.READ)
        messagesDatabase.messagesQueries.insert(
            messages = mockMessage(
                id = "3",
                state = RecontactService.Message.State.READ,
            ).toDomain().copy(state = MessageState.REPORTED),
        )
        runBlocking {
            coEvery {
                recontactService.getNewMessages(PSEUDONYM, RecontactService.Message.State.CREATED)
            } returns RecontactService.GetMessagesResponse(listOf(unreadMessage, readMessage))
            coEvery {
                recontactService.markAsRead(
                    PSEUDONYM,
                    listOf(readMessage.id).toMarkMessageReadBody(),
                )
            } returns RecontactService.MarkMessageReadResponse(
                messages = listOf(
                    mockMessage(
                        readMessage.id,
                        RecontactService.Message.State.READ,
                    ),
                ),
                errors = listOf(),
            )

            underTest.invoke()

            assertThat(messagesLoadingStateMutableStateFlow.first()).isEqualTo(MessagesLoadingState.READY)
            coVerify { messageEventSender wasNot called }

            messagesDatabase.messagesQueries.getById(unreadMessage.id).executeAsOne().also {
                assertThat(it.id).isEqualTo(unreadMessage.id)
                assertThat(it.state).isEqualTo(MessageState.UNREAD)
                println(it.toString())
            }
            messagesDatabase.messagesQueries.getById(readMessage.id).executeAsOne().also {
                assertThat(it.id).isEqualTo(readMessage.id)
                assertThat(it.state).isEqualTo(MessageState.REPORTED)
            }
            messagesDatabase.messagesQueries.getById("3").executeAsOne().also {
                assertThat(it.id).isEqualTo("3")
                assertThat(it.state).isEqualTo(MessageState.REPORTED)
            }
        }
    }

    @Test
    fun `fetch messages with one failed mark read item`() {
        messagesDatabase.messagesQueries.insert(
            messages = mockMessage(id = "2", state = RecontactService.Message.State.READ).toDomain(),
        )
        runBlocking {
            coEvery {
                recontactService.getNewMessages(PSEUDONYM, RecontactService.Message.State.CREATED)
            } returns RecontactService.GetMessagesResponse(listOf())
            coEvery {
                recontactService.markAsRead(
                    PSEUDONYM,
                    listOf("2").toMarkMessageReadBody(),
                )
            } returns RecontactService.MarkMessageReadResponse(
                messages = listOf(),
                errors = listOf("2"),
            )

            underTest.invoke()

            assertThat(messagesLoadingStateMutableStateFlow.first()).isEqualTo(MessagesLoadingState.READY)
            coVerify { messageEventSender wasNot called }

            assertThat(messagesDatabase.messagesQueries.getById("2").executeAsOneOrNull()).isNull()
        }
    }

    @Test
    fun `fetch messages with exception`() {
        runBlocking {
            coEvery {
                recontactService.getNewMessages(
                    PSEUDONYM,
                    RecontactService.Message.State.CREATED,
                )
            } throws Exception()

            coEvery { messageEventSender.send(FetchMessagesUseCase.Event.Failed) } just runs

            underTest.invoke()

            assertThat(messagesLoadingStateMutableStateFlow.first()).isEqualTo(MessagesLoadingState.FAILED)
            coVerify { messageEventSender.send(FetchMessagesUseCase.Event.Failed) }
            coVerify { messageSettingsDataStore wasNot called }
        }
    }

    private fun mockMessage(
        id: String,
        state: RecontactService.Message.State,
    ): RecontactService.Message = RecontactService.Message(
        id = id,
        linkedRequest = "RequestId",
        createdAt = ZonedDateTime.of(LocalDate.of(2020, 4, 4).atStartOfDay(), ZoneId.of("UTC")),
        updatedAt = null,
        content = RecontactService.Message.Content(text = "text", title = "title"),
        recipientId = PSEUDONYM,
        state = state,
    )
}
