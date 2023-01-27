package com.healthmetrix.myscience.feature.sync

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DomainResource

fun Collection<DomainResource>.toBundle() = Bundle().apply {
    type = Bundle.BundleType.COLLECTION

    forEach { domainResource ->
        Bundle.BundleEntryComponent().apply {
            resource = domainResource
        }.let(this::addEntry)
    }
}
