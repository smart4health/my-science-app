package com.healthmetrix.myscience.feature.sync.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.healthmetrix.myscience.feature.sync.ConsentDataCategory
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation
import javax.inject.Inject
import javax.inject.Singleton

const val OBSERVATION_CATEGORY_SYSTEM_URL =
    "http://terminology.hl7.org/CodeSystem/observation-category"
const val OBSERVATION_CATEGORY_CODE_LABORATORY = "laboratory"
const val OBSERVATION_CATEGORY_CODE_ACTIVITY = "activity"

/**
 * Observation Resources can be one of three categories, or OTHER, if nothing matches.
 * If the Observation#category field is present, we can locally trace the category.
 * Otherwise the Observation#code field will be checked
 */
@Singleton
class TraceObservationDataCategoryUseCase @Inject constructor(
    private val isCodingPartOfCategoryUseCase: IsCodingPartOfCategoryUseCase,
) {

    suspend operator fun invoke(observation: Observation): Result<ConsentDataCategory, Error> =
        binding {
            when (observation.category.size) {
                0 ->
                    traceByCode(observation.code.coding)
                        .mapError { err ->
                            when (err) {
                                is IsCodingPartOfCategoryUseCase.Error.Retrofit -> Error.Tracing(
                                    err.t,
                                )
                            }
                        }.bind()
                1 -> {
                    val category = traceByCategory(observation.category.first().coding)
                    if (category == ConsentDataCategory.OTHER) {
                        traceByCode(observation.code.coding)
                            .mapError { err ->
                                when (err) {
                                    is IsCodingPartOfCategoryUseCase.Error.Retrofit -> Error.Tracing(
                                        err.t,
                                    )
                                }
                            }.bind()
                    } else category
                }
                else -> Err(Error.MultipleFhirCategories).bind<ConsentDataCategory>()
            }
        }

    private fun traceByCategory(coding: List<Coding>): ConsentDataCategory =
        coding.mapNotNull {
            if (it.system == OBSERVATION_CATEGORY_SYSTEM_URL) {
                when (it.code) {
                    OBSERVATION_CATEGORY_CODE_LABORATORY -> ConsentDataCategory.DIAGNOSTIC_RESULTS_LABORATORY
                    OBSERVATION_CATEGORY_CODE_ACTIVITY -> ConsentDataCategory.DIAGNOSTIC_RESULTS_FITNESS
                    else -> null
                }
            } else {
                null
            }
        }.firstOrNull() ?: ConsentDataCategory.OTHER

    private suspend fun traceByCode(codings: List<Coding>): Result<ConsentDataCategory, IsCodingPartOfCategoryUseCase.Error> =
        binding {
            codings.forEach {
                when {
                    isCodingPartOfCategoryUseCase(it, ConsentDataCategory.VITAL_SIGNS).bind() -> {
                        return@binding ConsentDataCategory.VITAL_SIGNS
                    }
                    isCodingPartOfCategoryUseCase(
                        it,
                        ConsentDataCategory.DIAGNOSTIC_RESULTS_LABORATORY,
                    ).bind() -> {
                        return@binding ConsentDataCategory.DIAGNOSTIC_RESULTS_LABORATORY
                    }
                    isCodingPartOfCategoryUseCase(
                        it,
                        ConsentDataCategory.DIAGNOSTIC_RESULTS_FITNESS,
                    ).bind() -> {
                        return@binding ConsentDataCategory.DIAGNOSTIC_RESULTS_FITNESS
                    }
                }
            }
            ConsentDataCategory.OTHER
        }

    sealed class Error : Throwable() {
        data class Tracing(val t: Throwable) : Error()
        object MultipleFhirCategories : Error()
    }
}
