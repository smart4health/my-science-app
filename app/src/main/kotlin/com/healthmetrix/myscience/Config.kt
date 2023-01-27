package com.healthmetrix.myscience

import android.net.Uri
import androidx.core.os.LocaleListCompat
import com.healthmetrix.s4h.myscience.BuildConfig
import java.util.Locale

object DataProvConfig {
    val consent = ConsentConfig
    val recontact = RecontactConfig
    val deident = DeidentConfig
    val qomop = QomopConfig
}

object ConsentConfig {
    val host = BuildConfig.CONSENT_HOST.trimEnd('/')
    const val baseUrl = BuildConfig.CONSENT_BASEURL
    val configureSuccessUri: Uri = Uri.parse("com.healthmetrix.myscience://app/consent_success")
    val configureCancelUri: Uri = Uri.parse("com.healthmetrix.myscience://app/consent_cancel")
    val signingSuccessUri: Uri = Uri.parse("com.healthmetrix.myscience://app/signing_success")

    private const val consentEnglish = "smart4health-research-consent-en"
    private const val consentGerman = "smart4health-research-consent-de"
    private val localeToConsentIdMapping = mapOf(
        Locale.GERMAN.language to consentGerman,
        Locale.ENGLISH.language to consentEnglish,
    )

    /**
     * NOTE: This will not work if the user opens the consent, switches the language and switches
     * back to the suspended app.
     * Also could be improved by using strings.xml injected resources.
     */
    fun getId(): String {
        val primaryLocale: Locale = LocaleListCompat.getAdjustedDefault()[0] ?: Locale.ENGLISH
        return localeToConsentIdMapping[primaryLocale.language] ?: consentEnglish
    }
}

object RecontactConfig {
    const val baseUrl = BuildConfig.RECONTACT_BASEURL
    const val user = BuildConfig.RECONTACT_USER
    const val pass = BuildConfig.RECONTACT_PASS
}

object DeidentConfig {
    const val baseUrl = BuildConfig.DEIDENT_BASEURL
    const val user = BuildConfig.DEIDENT_USER
    const val pass = BuildConfig.DEIDENT_PASS
}

object QomopConfig {
    const val baseUrl = BuildConfig.QOMOP_BASEURL
    const val user = BuildConfig.QOMOP_USER
    const val pass = BuildConfig.QOMOP_PASS
}
