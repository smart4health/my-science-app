package com.healthmetrix.myscience.feature.sync.usecase

import android.util.Log
import androidx.datastore.core.DataStore
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.binding.binding
import com.healthmetrix.myscience.feature.sync.ConsentDataCategory
import com.healthmetrix.myscience.features.dataselection.DataSelectionSettings
import kotlinx.coroutines.flow.first
import org.hl7.fhir.r4.model.AllergyIntolerance
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.DomainResource
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Medication
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Procedure
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSyncPermissionCheckUseCase @Inject constructor(
    private val dataSelectionSettingsDataStore: DataStore<DataSelectionSettings>,
    private val isSurgicalProcedureUseCase: IsSurgicalProcedureUseCase,
    private val isMedicalDeviceUseCase: IsMedicalDeviceUseCase,
    private val traceObservationDataCategoryUseCase: TraceObservationDataCategoryUseCase,
) {
    suspend operator fun invoke(resource: DomainResource): Result<DataSyncPermission, Throwable> =
        binding {
            val settings = dataSelectionSettingsDataStore.data.first().toConsentCategories()

            // No need to check the data selection if the citizen consents to all of it
            if (settings.consentsToAllDataProvision()) {
                return@binding DataSyncPermission.ALLOWED
            }

            // Patient and Consent must pass in all cases
            if (resource is Patient || resource is Consent) {
                return@binding DataSyncPermission.ALLOWED
            }

            val consentDataCategory = detectConsentDataCategory(resource).bind()

            settings.consentsTo(consentDataCategory)
                .also {
                    Log.i(
                        this@DataSyncPermissionCheckUseCase::class.simpleName,
                        "Resource [${resource.idElement.idPart}] belongs to ConsentDataCategory$consentDataCategory, with consent $it",
                    )
                }
        }

    private suspend fun detectConsentDataCategory(resource: DomainResource): Result<ConsentDataCategory, Throwable> =
        binding {
            when (resource) {
                is Medication -> ConsentDataCategory.MEDICATIONS
                is MedicationStatement -> ConsentDataCategory.MEDICATIONS
                is AllergyIntolerance -> ConsentDataCategory.ALLERGIES_INTOLERANCES
                is Condition -> ConsentDataCategory.CLINICAL_CONDITIONS
                is Immunization -> ConsentDataCategory.IMMUNIZATIONS
                is Procedure -> if (isSurgicalProcedureUseCase(resource).bind()) ConsentDataCategory.PROCEDURES_SURGERIES else ConsentDataCategory.OTHER
                is Device -> if (isMedicalDeviceUseCase(resource).bind()) ConsentDataCategory.MEDICAL_DEVICES_IMPLANTS else ConsentDataCategory.OTHER
                is Observation -> traceObservationDataCategoryUseCase(resource).bind()
                is Questionnaire -> ConsentDataCategory.QUESTIONNAIRES
                is QuestionnaireResponse -> ConsentDataCategory.QUESTIONNAIRES
                else -> ConsentDataCategory.OTHER
            }
        }

    private fun HashMap<ConsentDataCategory, Boolean>.consentsTo(
        category: ConsentDataCategory,
    ): DataSyncPermission =
        if (category != ConsentDataCategory.OTHER && this[category] == true) {
            DataSyncPermission.ALLOWED
        } else {
            DataSyncPermission.FORBIDDEN
        }

    private fun HashMap<ConsentDataCategory, Boolean>.consentsToAllDataProvision(): Boolean =
        values.all { it }

    private fun DataSelectionSettings.toConsentCategories(): HashMap<ConsentDataCategory, Boolean> =
        hashMapOf(
            ConsentDataCategory.MEDICATIONS to true, // required by IPS
            ConsentDataCategory.ALLERGIES_INTOLERANCES to true, // required by IPS
            ConsentDataCategory.CLINICAL_CONDITIONS to true, // required by IPS
            ConsentDataCategory.IMMUNIZATIONS to immunizations,
            ConsentDataCategory.PROCEDURES_SURGERIES to procedures,
            ConsentDataCategory.MEDICAL_DEVICES_IMPLANTS to medicalDevices,
            ConsentDataCategory.VITAL_SIGNS to vitals,
            ConsentDataCategory.DIAGNOSTIC_RESULTS_LABORATORY to diagnosticResultsLaboratory,
            ConsentDataCategory.DIAGNOSTIC_RESULTS_FITNESS to diagnosticResultsFitness,
            ConsentDataCategory.QUESTIONNAIRES to questionnaires,
        )
}

enum class DataSyncPermission {
    ALLOWED, FORBIDDEN
}
