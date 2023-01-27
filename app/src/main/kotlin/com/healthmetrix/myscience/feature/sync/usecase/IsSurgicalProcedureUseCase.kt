package com.healthmetrix.myscience.feature.sync.usecase

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.healthmetrix.myscience.feature.sync.ConsentDataCategory
import org.hl7.fhir.r4.model.Procedure
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsSurgicalProcedureUseCase @Inject constructor(
    private val isCodingPartOfCategoryUseCase: IsCodingPartOfCategoryUseCase,
) {

    suspend operator fun invoke(procedure: Procedure): Result<Boolean, Error> =
        binding {
            procedure.code.coding.any {
                isCodingPartOfCategoryUseCase(it, ConsentDataCategory.PROCEDURES_SURGERIES)
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
