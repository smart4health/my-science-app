package com.healthmetrix.myscience.feature.dataselection

import android.content.res.Resources
import com.healthmetrix.myscience.features.dataselection.DataSelectionSettings
import com.healthmetrix.s4h.myscience.R

fun DataSelectionSettings.toEntries(resources: Resources): List<DataSelectionView.Entry> {
    return listOf(
        DataSelectionView.Entry(
            key = "medications",
            title = resources.getString(R.string.ds_medications_title),
            description = resources.getString(R.string.ds_medications_description),
            checked = medications,
            required = true,
        ),
        DataSelectionView.Entry(
            key = "allergies",
            title = resources.getString(R.string.ds_allergies_title),
            description = resources.getString(R.string.ds_allergies_description),
            checked = allergies,
            required = true,
        ),
        DataSelectionView.Entry(
            key = "problems",
            title = resources.getString(R.string.ds_problems_title),
            description = resources.getString(R.string.ds_problems_description),
            checked = problems,
            required = true,
        ),
        DataSelectionView.Entry(
            key = "immunizations",
            title = resources.getString(R.string.ds_immunization_title),
            description = resources.getString(R.string.ds_immunization_description),
            checked = immunizations,
        ),
        DataSelectionView.Entry(
            key = "procedures",
            title = resources.getString(R.string.ds_procedures_title),
            description = resources.getString(R.string.ds_procedures_description),
            checked = procedures,
        ),
        DataSelectionView.Entry(
            key = "medical_devices",
            title = resources.getString(R.string.ds_medical_devices_title),
            description = resources.getString(R.string.ds_medical_devices_description),
            checked = medicalDevices,
        ),
        DataSelectionView.Entry(
            key = "vitals",
            title = resources.getString(R.string.ds_vitals_title),
            description = resources.getString(R.string.ds_vitals_description),
            checked = vitals,
        ),
        DataSelectionView.Entry(
            key = "diagnostic_results_laboratory",
            title = resources.getString(R.string.ds_diagnostic_results_laboratory_title),
            description = resources.getString(R.string.ds_diagnostic_results_laboratory_description),
            checked = diagnosticResultsLaboratory,
        ),
        DataSelectionView.Entry(
            key = "diagnostic_results_fitness",
            title = resources.getString(R.string.ds_diagnostic_results_fitness_title),
            description = resources.getString(R.string.ds_diagnostic_results_fitness_description),
            checked = diagnosticResultsFitness,
        ),
        DataSelectionView.Entry(
            key = "questionnaire",
            title = resources.getString(R.string.ds_questionnaire_title),
            description = resources.getString(R.string.ds_questionnaire_description),
            checked = questionnaires,
        ),
    )
}

fun List<DataSelectionView.Entry>.toDataSelectionSettings(): DataSelectionSettings {
    return DataSelectionSettings.newBuilder().apply {
        forEach { entry ->
            when (entry.key) {
                "medications" -> medications = entry.checked
                "allergies" -> allergies = entry.checked
                "problems" -> problems = entry.checked
                "immunizations" -> immunizations = entry.checked
                "procedures" -> procedures = entry.checked
                "medical_devices" -> medicalDevices = entry.checked
                "vitals" -> vitals = entry.checked
                "diagnostic_results_laboratory" -> diagnosticResultsLaboratory = entry.checked
                "diagnostic_results_fitness" -> diagnosticResultsFitness = entry.checked
                "questionnaire" -> questionnaires = entry.checked
                else -> error("Missing entry key ${entry.key}")
            }
        }
    }.build()
}
