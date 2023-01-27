package com.healthmetrix.myscience.chdp

import android.content.Context
import android.content.Intent
import android.util.Log
import care.data4life.fhir.r4.model.DomainResource
import care.data4life.sdk.Data4LifeClient
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.listener.ResultListener
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.threeten.bp.ZonedDateTime
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private typealias Page = List<Fhir4Record<DomainResource>>

class ChdpClient(
    applicationContext: Context,
) {
    init {
        AndroidThreeTen.init(applicationContext)
        Data4LifeClient.init(applicationContext)
    }

    suspend fun loginIntent(context: Context): Intent = withContext(Dispatchers.IO) {
        Data4LifeClient.getInstance().getLoginIntent(
            context,
            setOf(
                "perm:r",
                "rec:r",
                "rec:w",
                "attachment:r",
                "attachment:w",
                "user:r",
                "user:w",
                "user:q",
            ),
        )
    }

    suspend fun isLoggedIn(): Result<Boolean, D4LException> = ioCoroutine { cont ->
        Data4LifeClient.getInstance().isUserLoggedIn(
            object : ResultListener<Boolean> {
                override fun onSuccess(t: Boolean) = cont.resume(Ok(t))

                override fun onError(exception: D4LException) = cont.resume(Err(exception))
            },
        )
    }

    suspend fun getClientId(): String = withContext(Dispatchers.IO) {
        Data4LifeClient.getInstance().userId
    }

    suspend fun logout(): Result<Unit, D4LException> = if (isLoggedIn() == Ok(true)) {
        ioCoroutine { cont ->
            Data4LifeClient.getInstance().logout(
                object : care.data4life.sdk.listener.Callback {
                    override fun onSuccess() = cont.resume(Ok(Unit))

                    override fun onError(exception: D4LException) = cont.resume(Err(exception))
                },
            )
        }
    } else Ok(Unit)

    suspend fun downloadRecord(
        id: String,
    ): Result<Fhir4Record<DomainResource>, D4LException> = ioCoroutine { cont ->
        Data4LifeClient.getInstance().fhir4.download(id, Data4LifeCallback(cont))
    }

    suspend fun createRecord(
        resource: DomainResource,
    ): Result<Fhir4Record<DomainResource>, D4LException> = ioCoroutine { cont ->
        Data4LifeClient.getInstance().fhir4.create(
            resource = resource,
            annotations = listOf(),
            callback = Data4LifeCallback(cont),
        )
    }

    private suspend fun downloadPage(
        pageNumber: Int,
        pageSize: Int,
        startDate: ZonedDateTime?,
    ): Result<Page, D4LException> = binding {
        ioCoroutine<Result<Page, D4LException>> { cont ->
            Data4LifeClient.getInstance().fhir4.search(
                resourceType = DomainResource::class.java,
                annotations = listOf(),
                startDate = startDate?.toLocalDate(),
                endDate = null,
                pageSize = pageSize,
                offset = pageNumber * pageSize,
                callback = Data4LifeCallback(cont),
            )
        }.bind().filter { record ->
            startDate?.toLocalDateTime()?.let {
                record.meta.updatedDate.isAfter(it)
            } ?: true
        }.map { record ->
            downloadRecord(record.identifier).bind()
        }
    }

    suspend fun downloadAllWithErr(
        pageSize: Int = 10,
        startDate: ZonedDateTime? = null,
    ): Flow<Result<Page, D4LException>> = flow {
        binding<Unit, D4LException> {
            var pageNumber = 0
            while (true) {
                val page = downloadPage(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    startDate = startDate,
                ).onFailure { ex ->
                    Log.e(
                        this@ChdpClient::class.simpleName,
                        "Failed to download page $pageNumber",
                        ex,
                    )
                }.bind()

                if (page.isNotEmpty()) {
                    emit(Ok(page))
                }

                if (page.size < pageSize) {
                    break
                }

                pageNumber += 1
            }
        }.onFailure { e ->
            emit(Err(e))
        }
    }.flowOn(Dispatchers.IO)

    @Deprecated(message = "Does not surface D4L Exceptions when downloading", replaceWith = ReplaceWith("downloadAllWithErr"))
    suspend fun downloadAll(
        pageSize: Int = 10,
        startDate: ZonedDateTime? = null,
    ): Flow<Page> = flow {
        var pageNumber = 0
        while (true) {
            val page = downloadPage(
                pageNumber = pageNumber,
                pageSize = pageSize,
                startDate = startDate,
            ).onFailure { ex ->
                Log.e(this@ChdpClient::class.simpleName, "Failed to download page $pageNumber", ex)
            }.getOrElse { listOf() }

            if (page.isNotEmpty()) {
                emit(page)
            }

            if (page.size < pageSize) {
                break
            }

            pageNumber += 1
        }
    }.flowOn(Dispatchers.IO)
}

private suspend fun <T> ioCoroutine(lambda: (Continuation<T>) -> Unit): T =
    withContext(Dispatchers.IO) {
        suspendCoroutine { cont ->
            lambda(cont)
        }
    }

private class Data4LifeCallback<T>(
    private val continuation: Continuation<Result<T, D4LException>>,
) : Callback<T> {
    override fun onSuccess(result: T) {
        continuation.resume(Ok(result))
    }

    override fun onError(exception: D4LException) {
        continuation.resume(Err(exception))
    }
}
