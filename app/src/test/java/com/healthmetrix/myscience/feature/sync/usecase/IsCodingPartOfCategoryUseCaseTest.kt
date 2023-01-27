package com.healthmetrix.myscience.feature.sync.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.healthmetrix.myscience.feature.sync.ConsentDataCategory
import com.healthmetrix.myscience.service.qomop.QomopService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.Coding
import org.junit.jupiter.api.Test

class IsCodingPartOfCategoryUseCaseTest {

    private val qomopService: QomopService = mockk()
    private val underTest = IsCodingPartOfCategoryUseCase(qomopService)

    @Test
    fun `qomop service looks up the coding successfully as true`() {
        coEvery {
            qomopService.traceCodingForCategory(
                ConsentDataCategory.PROCEDURES_SURGERIES.id,
                QomopService.TracingBody("s", "c"),
            )
        } returns QomopService.TracingResponse(true)
        runBlocking { underTest(Coding("s", "c", "d"), ConsentDataCategory.PROCEDURES_SURGERIES) }
            .also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isTrue
            }
    }

    @Test
    fun `qomop service looks up the coding successfully as false`() {
        coEvery {
            qomopService.traceCodingForCategory(
                ConsentDataCategory.PROCEDURES_SURGERIES.id,
                QomopService.TracingBody("s", "c"),
            )
        } returns QomopService.TracingResponse(false, "errorMessage")
        runBlocking { underTest(Coding("s", "c", "d"), ConsentDataCategory.PROCEDURES_SURGERIES) }
            .also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isFalse
            }
    }

    @Test
    fun `qomop service fails`() {
        coEvery {
            qomopService.traceCodingForCategory(
                ConsentDataCategory.PROCEDURES_SURGERIES.id,
                QomopService.TracingBody("s", "c"),
            )
        } throws Exception("oh no")
        runBlocking { underTest(Coding("s", "c", "d"), ConsentDataCategory.PROCEDURES_SURGERIES) }
            .also {
                assertThat(it).isInstanceOf(Err::class.java)
                assertThat(it.getError()).isInstanceOf(IsCodingPartOfCategoryUseCase.Error.Retrofit::class.java)
            }
    }
}
