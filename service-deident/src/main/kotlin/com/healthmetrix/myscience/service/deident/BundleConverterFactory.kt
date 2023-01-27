package com.healthmetrix.myscience.service.deident

import ca.uhn.fhir.context.FhirContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import org.hl7.fhir.r4.model.Bundle
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.util.zip.DataFormatException

class BundleConverterFactory(
    private val fhirContext: FhirContext,
) : Converter.Factory() {
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? = when (type) {
        Bundle::class.java -> BundleConverter(fhirContext)
        else -> null
    }

    class BundleConverter(
        private val fhirContext: FhirContext,
    ) : Converter<Bundle, RequestBody> {
        override fun convert(value: Bundle): RequestBody? = try {
            val serialized = fhirContext
                .newJsonParser()
                .encodeResourceToString(value)

            object : RequestBody() {
                override fun contentType() = "application/fhir+json".toMediaType()

                override fun writeTo(sink: BufferedSink) {
                    sink.writeString(serialized, Charsets.UTF_8)
                }
            }
        } catch (ex: DataFormatException) {
            null
        }
    }
}
