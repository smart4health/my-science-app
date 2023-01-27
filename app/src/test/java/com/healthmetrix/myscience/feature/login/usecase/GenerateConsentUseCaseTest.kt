package com.healthmetrix.myscience.feature.login.usecase

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Test

class GenerateConsentUseCaseTest {

    private val underTest: GenerateConsentUseCase = GenerateConsentUseCase()
    private val fixedDate = DateTimeType.parseV3("20210804120000+0200")
        .apply { precision = TemporalPrecisionEnum.SECOND }
    private val fhirParser = FhirContext.forR4().newJsonParser().setPrettyPrint(true)

    @Test
    fun `generate active consent with IS added`() {
        val consent = underTest.invoke(
            patientReference = Reference("TODO"),
            thirdPartyAccessAllowed = false,
            timeOfConsent = fixedDate,
            resourceId = "the id",
        )
        assertThat(consent.provision.provision[0].code.size).isEqualTo(5)
        assertThat(consent.provision.provision[0].code.any { concept -> concept.coding.any { it.code == "IS" } }).isTrue
        assertThat(fhirParser.encodeResourceToString(consent)).isEqualTo(
            javaClass.classLoader
                ?.getResource("expected_consent.json")
                ?.readText(),
        )
    }

    @Test
    fun `generate active consent with IS not added`() {
        val consent = underTest.invoke(
            patientReference = Reference("TODO"),
            thirdPartyAccessAllowed = true,
            timeOfConsent = fixedDate,
            resourceId = "the id",
        )
        assertThat(consent.provision.provision[0].code.size).isEqualTo(4)
        assertThat(consent.provision.provision[0].code.any { concept -> concept.coding.any { it.code == "IS" } }).isFalse
    }

    @Test
    fun `generate revoked consent with IS not added`() {
        val consent = underTest.invoke(
            patientReference = Reference("TODO"),
            thirdPartyAccessAllowed = true,
            timeOfConsent = fixedDate,
            revoke = true,
            resourceId = "the id",
        )
        assertThat(consent.provision.provision[0].code.size).isEqualTo(4)
        assertThat(consent.provision.provision[0].code.any { concept -> concept.coding.any { it.code == "IS" } }).isFalse
        assertThat(fhirParser.encodeResourceToString(consent)).isEqualTo(
            javaClass.classLoader
                ?.getResource("expected_consent_revoked.json")
                ?.readText(),
        )
    }
}
