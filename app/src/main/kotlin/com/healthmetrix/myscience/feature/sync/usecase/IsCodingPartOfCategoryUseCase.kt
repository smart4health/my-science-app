package com.healthmetrix.myscience.feature.sync.usecase

import android.util.Log
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.healthmetrix.myscience.feature.sync.ConsentDataCategory
import com.healthmetrix.myscience.service.qomop.QomopService
import org.hl7.fhir.r4.model.Coding
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsCodingPartOfCategoryUseCase @Inject constructor(
    private val qomopService: QomopService,
) {

    suspend operator fun invoke(
        coding: Coding,
        category: ConsentDataCategory,
    ): Result<Boolean, Error> = binding {
        qomopService
            .runCatching {
                traceCodingForCategory(
                    categoryId = category.id,
                    tracingBody = QomopService.TracingBody(
                        system = coding.system,
                        code = coding.code,
                    ),
                )
            }
            .onFailure {
                Log.i(
                    this@IsCodingPartOfCategoryUseCase.javaClass.simpleName,
                    "Failed for ${coding.system}, ${coding.code}, $category",
                )
            }
            .mapError(Error::Retrofit)
            .bind()
            .also { response ->
                response.message?.let {
                    Log.i(
                        this@IsCodingPartOfCategoryUseCase.javaClass.simpleName,
                        it,
                    )
                }
            }
            .match
    }

    sealed class Error {
        data class Retrofit(val t: Throwable) : Error()
    }
}
