package com.healthmetrix.myscience.feature.sync.usecase

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.healthmetrix.myscience.feature.sync.ConsentDataCategory
import org.hl7.fhir.r4.model.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsMedicalDeviceUseCase @Inject constructor(
    private val isCodingPartOfCategoryUseCase: IsCodingPartOfCategoryUseCase,
) {

    suspend operator fun invoke(device: Device): Result<Boolean, Error> =
        binding {
            device.type.coding.any {
                isCodingPartOfCategoryUseCase(it, ConsentDataCategory.MEDICAL_DEVICES_IMPLANTS)
                    .mapError { err ->
                        when (err) {
                            is IsCodingPartOfCategoryUseCase.Error.Retrofit -> Error.Tracing(err.t)
                        }
                    }
                    .bind()
            }
        }

    sealed class Error : Throwable() {
        data class Tracing(val t: Throwable) : Error()
    }
}
