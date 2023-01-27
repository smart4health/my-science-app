package com.healthmetrix.myscience.feature.sync.usecase

import ca.uhn.fhir.context.FhirContext
import care.data4life.fhir.r4.FhirR4Parser
import care.data4life.sdk.call.Fhir4Record
import com.github.michaelbull.result.Result
import org.hl7.fhir.instance.model.api.IBaseResource
import javax.inject.Inject
import care.data4life.fhir.r4.model.DomainResource as D4LDomainResource
import care.data4life.fhir.r4.model.Patient as D4LPatient
import com.github.michaelbull.result.runCatching as catch
import org.hl7.fhir.r4.model.DomainResource as HapiDomainResource
import org.hl7.fhir.r4.model.Patient as HapiPatient

class ConvertFhirUseCase @Inject constructor(
    private val d4lParser: FhirR4Parser,
    private val hapiContext: FhirContext,
) {

    fun toHapi(
        record: Fhir4Record<D4LDomainResource>,
    ): Result<HapiDomainResource, Throwable> = catch {
        val jsonParser = hapiContext.newJsonParser()

        record
            .resource
            .let<D4LDomainResource, String>(d4lParser::fromFhir)
            .let<String, IBaseResource?>(jsonParser::parseResource)
            as HapiDomainResource
    }

    fun toHapiPatient(
        record: Fhir4Record<D4LPatient>,
    ): Result<HapiPatient, Throwable> = catch {
        val jsonParser = hapiContext.newJsonParser()

        record
            .resource
            .let<D4LPatient, String>(d4lParser::fromFhir)
            .let { jsonParser.parseResource(HapiPatient::class.java, it) }
    }

    fun fromHapi(
        resource: HapiDomainResource,
    ): Result<D4LDomainResource, Throwable> = catch {
        val jsonParser = hapiContext.newJsonParser()

        resource
            .let<IBaseResource, String>(jsonParser::encodeResourceToString)
            .let { d4lParser.toFhir(D4LDomainResource::class.java, it) }
    }
}
