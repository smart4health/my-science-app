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
import org.hl7.fhir.r4.model.Device
import org.junit.jupiter.api.Test

class IsMedicalDeviceUseCaseTest {
    private val isCodingPartOfCategoryUseCase: IsCodingPartOfCategoryUseCase = mockk()
    private val underTest = IsMedicalDeviceUseCase(isCodingPartOfCategoryUseCase)

    @Test
    fun `empty Resource should return false`() {
        runBlocking { underTest(Device()) }
            .also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isFalse
            }
    }

    @Test
    fun `Device with at least one medical Device should return true`() {
        val medicalDevice = Coding("system", "code", "display")
        val nonMedicalDevice = Coding("system2", "code2", "display2")
        val device = Device().apply {
            type =
                CodeableConcept().apply {
                    coding = listOf(medicalDevice, nonMedicalDevice)
                }
        }
        coEvery {
            isCodingPartOfCategoryUseCase(
                medicalDevice,
                ConsentDataCategory.MEDICAL_DEVICES_IMPLANTS,
            )
        } returns Ok(true)
        coEvery {
            isCodingPartOfCategoryUseCase(
                nonMedicalDevice,
                ConsentDataCategory.MEDICAL_DEVICES_IMPLANTS,
            )
        } returns Ok(false)

        runBlocking {
            underTest(device).also {
                assertThat(it).isInstanceOf(Ok::class.java)
                assertThat(it.get()).isTrue
            }
        }
    }

    @Test
    fun `Device with no medical Device should return true`() {
        val nonMedicalDevice = Coding("system2", "code2", "display2")
        val device = Device().apply { type = CodeableConcept(nonMedicalDevice) }
        coEvery {
            isCodingPartOfCategoryUseCase(
                nonMedicalDevice,
                ConsentDataCategory.MEDICAL_DEVICES_IMPLANTS,
            )
        } returns Ok(false)

        runBlocking {
            underTest(device).also {
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
            underTest(Device().apply { type = CodeableConcept(Coding()) }).also {
                assertThat(it).isInstanceOf(Err::class.java)
                assertThat(it.getError()).isInstanceOf(IsMedicalDeviceUseCase.Error.Tracing::class.java)
            }
        }
    }
}
