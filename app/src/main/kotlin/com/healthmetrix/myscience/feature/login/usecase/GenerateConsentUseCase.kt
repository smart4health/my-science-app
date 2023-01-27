package com.healthmetrix.myscience.feature.login.usecase

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import java.util.UUID
import javax.inject.Inject

class GenerateConsentUseCase @Inject constructor() {

    /**
     * The institute-specific consent option should be added only if the citizen does NOT consent
     * with third party access
     */
    operator fun invoke(
        resourceId: String = UUID.randomUUID().toString(),
        patientReference: Reference,
        thirdPartyAccessAllowed: Boolean,
        timeOfConsent: DateTimeType,
        revoke: Boolean = false,
    ): Consent =
        Consent().apply {
            id = resourceId
            patient = patientReference
            status = Consent.ConsentState.ACTIVE
            scope = CodeableConcept(
                Coding().apply {
                    system = "http://hl7.org/fhir/ValueSet/consent-scope"
                    code = "research"
                    display = "Research"
                },
            )
            category = listOf(
                CodeableConcept(
                    Coding().apply {
                        system = "http://terminology.hl7.org/CodeSystem/consentcategorycodes"
                        code = "rsreid"
                        display = "Re-identifiable Information Access"
                    },
                ),
            )
            policyRule = CodeableConcept(
                Coding().apply {
                    system = "http://terminology.hl7.org/CodeSystem/consentpolicycodes"
                    code = "ga4gh"
                    display = "Population origins and ancestry research consent"
                },
            )
            dateTimeElement =
                timeOfConsent.copy().apply { precision = TemporalPrecisionEnum.SECOND }
            provision = Consent.provisionComponent().apply {
                period = Period().apply {
                    startElement =
                        timeOfConsent.copy().apply { precision = TemporalPrecisionEnum.DAY }
                    endElement = if (revoke) timeOfConsent.copy().apply {
                        precision = TemporalPrecisionEnum.DAY
                    } else null
                }
                provision = listOf(
                    Consent.provisionComponent().apply {
                        type = Consent.ConsentProvisionType.PERMIT
                        code = mutableListOf(
                            ga4ghItemFrom(
                                theCode = "HMB",
                                theDisplay = "health/medical/biomedical research",
                            ),
                            ga4ghItemFrom(theCode = "RUO", theDisplay = "research use only"),
                            ga4ghItemFrom(theCode = "IRB", theDisplay = "ethics approval required"),
                            ga4ghItemFrom(theCode = "GS-EU", "geographical restrictions EU"),
                        ).apply {
                            if (!thirdPartyAccessAllowed) {
                                add(
                                    ga4ghItemFrom(
                                        theCode = "IS",
                                        theDisplay = "institution-specific restrictions",
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }

    private fun ga4ghItemFrom(theCode: String, theDisplay: String) = CodeableConcept(
        Coding().apply {
            system = "http://ga4gh.org"
            code = theCode
            display = theDisplay
        },
    )
}
