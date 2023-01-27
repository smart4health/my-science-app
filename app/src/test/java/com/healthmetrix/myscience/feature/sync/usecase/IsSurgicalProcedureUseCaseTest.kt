package com.healthmetrix.myscience.feature.sync.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.healthmetrix.myscience.feature.sync.ConsentDataCategory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Procedure
import org.junit.jupiter.api.Test

class IsSurgicalProcedureUseCaseTest {

    private val isCodingPartOfCategoryUseCase: IsCodingPartOfCategoryUseCase = mockk()
    private val underTest = IsSurgicalProcedureUseCase(isCodingPartOfCategoryUseCase)

    @Test
    fun `empty Resource should return false`() {
        runBlocking { underTest(Procedure()) }
            .also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isFalse
            }
    }

    @Test
    fun `Procedure with at least one surgical procedure should return true`() {
        val surgicalProcedure = Coding("system", "code", "display")
        val nonSurgicalProcedure = Coding("system2", "code2", "display2")
        val procedure = Procedure().apply {
            code =
                CodeableConcept().apply {
                    coding = listOf(surgicalProcedure, nonSurgicalProcedure)
                }
        }
        coEvery {
            isCodingPartOfCategoryUseCase(
                surgicalProcedure,
                ConsentDataCategory.PROCEDURES_SURGERIES,
            )
        } returns Ok(true)
        coEvery {
            isCodingPartOfCategoryUseCase(
                nonSurgicalProcedure,
                ConsentDataCategory.PROCEDURES_SURGERIES,
            )
        } returns Ok(false)

        runBlocking {
            underTest(procedure).also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isTrue
            }
        }
    }

    @Test
    fun `Procedure with no surgical procedure should return true`() {
        val nonSurgicalProcedure = Coding("system2", "code2", "display2")
        val procedure = Procedure().apply { code = CodeableConcept(nonSurgicalProcedure) }
        coEvery {
            isCodingPartOfCategoryUseCase(
                nonSurgicalProcedure,
                ConsentDataCategory.PROCEDURES_SURGERIES,
            )
        } returns Ok(false)

        runBlocking {
            underTest(procedure).also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isFalse
            }
        }
    }

    @Test
    fun `check exceptions being handled`() {
        coEvery {
            isCodingPartOfCategoryUseCase(any(), any())
        } returns Err(IsCodingPartOfCategoryUseCase.Error.Retrofit(Exception()))

        runBlocking {
            underTest(Procedure().apply { code = CodeableConcept(Coding()) }).also {
                assertThat(it).isInstanceOf(Err::class.java)
                assertThat(it.getError()).isInstanceOf(IsSurgicalProcedureUseCase.Error.Tracing::class.java)
            }
        }
    }
}
