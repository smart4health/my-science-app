package com.healthmetrix.myscience.feature.sync.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.healthmetrix.myscience.feature.sync.ConsentDataCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class TraceObservationDataCategoryUseCaseTest {

    private val isCodingPartOfCategoryUseCase: IsCodingPartOfCategoryUseCase = mockk()
    private val underTest = TraceObservationDataCategoryUseCase(isCodingPartOfCategoryUseCase)

    @Test
    fun `empty Resource should return OTHER`() {
        runBlocking { underTest(Observation()) }
            .also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isEqualTo(ConsentDataCategory.OTHER)
            }
    }

    @Test
    fun `Observation having a laboratory category should return DIAGNOSTIC_RESULTS_LABORATORY`() {
        runBlocking {
            underTest(
                Observation().apply {
                    category = listOf(
                        CodeableConcept(
                            Coding(
                                OBSERVATION_CATEGORY_SYSTEM_URL,
                                OBSERVATION_CATEGORY_CODE_LABORATORY,
                                "d",
                            ),
                        ),
                    )
                },
            ).also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isEqualTo(ConsentDataCategory.DIAGNOSTIC_RESULTS_LABORATORY)
            }
        }
    }

    @Test
    fun `Observation having an activity category should return DIAGNOSTIC_RESULTS_FITNESS`() {
        runBlocking {
            underTest(
                Observation().apply {
                    category = listOf(
                        CodeableConcept(
                            Coding(
                                OBSERVATION_CATEGORY_SYSTEM_URL,
                                OBSERVATION_CATEGORY_CODE_ACTIVITY,
                                "d",
                            ),
                        ),
                    )
                },
            ).also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isEqualTo(ConsentDataCategory.DIAGNOSTIC_RESULTS_FITNESS)
            }
        }
    }

    @Test
    fun `Observation having more than one category should return an error`() {
        runBlocking {
            underTest(
                Observation().apply {
                    category = listOf(
                        CodeableConcept(
                            Coding(
                                OBSERVATION_CATEGORY_SYSTEM_URL,
                                OBSERVATION_CATEGORY_CODE_ACTIVITY,
                                "d",
                            ),
                        ),
                        CodeableConcept(Coding("a", "b", "c")),
                    )
                },
            ).also {
                assertThat(it).isInstanceOf(Err::class.java)
                assertThat(it.getError()).isInstanceOf(TraceObservationDataCategoryUseCase.Error.MultipleFhirCategories::class.java)
            }
        }
    }

    @Test
    fun `Observation having some other category should lookup by code`() {
        coEvery { isCodingPartOfCategoryUseCase(any(), any()) } returns Ok(false)
        runBlocking {
            underTest(
                Observation().apply {
                    category = listOf(CodeableConcept(Coding("a", "b", "c")))
                    code = CodeableConcept(Coding("a", "b", "c"))
                },
            ).also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isEqualTo(ConsentDataCategory.OTHER)
            }
        }
        coVerify(exactly = 3) { isCodingPartOfCategoryUseCase(any(), any()) }
    }

    @Test
    fun `Observation having no category should lookup by code`() {
        coEvery { isCodingPartOfCategoryUseCase(any(), any()) } returns Ok(false)
        runBlocking {
            underTest(
                Observation().apply {
                    code = CodeableConcept(Coding("a", "b", "c"))
                },
            ).also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isEqualTo(ConsentDataCategory.OTHER)
            }
        }
        coVerify(exactly = 3) { isCodingPartOfCategoryUseCase(any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(
        value = ConsentDataCategory::class,
        names = ["VITAL_SIGNS", "DIAGNOSTIC_RESULTS_LABORATORY", "DIAGNOSTIC_RESULTS_FITNESS"],
    )
    fun `Observation lookup by Code with`(category: ConsentDataCategory) {
        val coding = Coding("a", "b", "c")
        coEvery { isCodingPartOfCategoryUseCase(coding, category) } returns Ok(true)
        coEvery {
            isCodingPartOfCategoryUseCase(
                coding,
                match { it != category },
            )
        } returns Ok(false)
        runBlocking {
            underTest(
                Observation().apply {
                    code = CodeableConcept(coding)
                },
            ).also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isEqualTo(category)
            }
        }
    }

    @Test
    fun `check exceptions being handled`() {
        coEvery {
            isCodingPartOfCategoryUseCase(any(), any())
        } returns Err(IsCodingPartOfCategoryUseCase.Error.Retrofit(Exception()))

        runBlocking {
            underTest(
                Observation().apply {
                    code = CodeableConcept(Coding("a", "b", "c"))
                },
            ).also {
                assertThat(it).isInstanceOf(Err::class.java)
                assertThat(it.getError()).isInstanceOf(TraceObservationDataCategoryUseCase.Error.Tracing::class.java)
            }
        }
    }
}
