package com.healthmetrix.myscience.feature.login

import kotlinx.serialization.Serializable

@Serializable
class ConfigureConsentProgress(
    val progress: Progress,
) {
    @Serializable
    class Progress(
        val slide: Int,
        val slidesTotal: Int,
    )

    companion object {
        // magic values to reset to 0 submilestone progress
        val NONE = ConfigureConsentProgress(Progress(1, 2))
    }
}

fun ConfigureConsentProgress.calculateProgress(): Float =
    progress.slide.minus(1).toFloat() / progress.slidesTotal.toFloat()
