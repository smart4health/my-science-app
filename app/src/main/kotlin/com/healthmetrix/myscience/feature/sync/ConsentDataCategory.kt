package com.healthmetrix.myscience.feature.sync

/**
 * Categories defined in the context of the User Consent
 */
enum class ConsentDataCategory(
    val id: Int,
) {

    /**
     * The FHIR resource to consent data categories mapping is not exhaustive, hence this field.
     */
    OTHER(0),

    /**
     * Traceable by:
     * ResourceType Medication or MedicationStatement
     */
    MEDICATIONS(1),

    /**
     * Traceable by:
     * ResourceType AllergyIntolerance
     */
    ALLERGIES_INTOLERANCES(2),

    /**
     * Traceable by:
     * ResourceType Condition
     */
    CLINICAL_CONDITIONS(3),

    /**
     * Traceable by:
     * ResourceType Immunization
     */
    IMMUNIZATIONS(4),

    /**
     * FHIR Resource path Procedure.code is traced by qomop-service
     */
    PROCEDURES_SURGERIES(5),

    /**
     * FHIR Resource path Device.type is traced by qomop-service
     */
    MEDICAL_DEVICES_IMPLANTS(6),

    /**
     * FHIR Resource path Observation.code is traced by qomop-service
     */
    VITAL_SIGNS(7),

    /**
     * This Type has two possible indicators:
     * FHIR Resource path Observation.code is traced by qomop-service
     * FHIR Resource path Observation.category=laboratory is checked on the app
     */
    DIAGNOSTIC_RESULTS_LABORATORY(8),

    /**
     * This Type has two possible indicators:
     * FHIR Resource path Observation.code is traced by qomop-service
     * FHIR Resource path Observation.category = activity is checked on the app
     */
    DIAGNOSTIC_RESULTS_FITNESS(9),

    /**
     * Traceable by:
     * ResourceType Questionnaire and QuestionnaireResponse
     */
    QUESTIONNAIRES(10),
    ;

    companion object {
        fun fromId(value: Int) = values().firstOrNull { it.id == value }
    }

    override fun toString(): String {
        return "$name ($id)"
    }
}
