package com.healthmetrix.myscience.feature.login.controller

import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.asTransaction
import com.github.michaelbull.result.onSuccess
import com.healthmetrix.myscience.conductor.MaterialSharedZAxisChangeHandler
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.login.ConfigureConsentProgress
import com.healthmetrix.myscience.feature.login.di.ConfigureConsentWebPageEntryPoint
import com.healthmetrix.myscience.feature.webpage.ARG_URL
import com.healthmetrix.myscience.feature.webpage.Redirectable
import com.healthmetrix.myscience.feature.webpage.WebPageController
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.github.michaelbull.result.runCatching as catch

class ConfigureConsentWebPageController(
    bundle: Bundle,
) : WebPageController(bundle) {

    private val entryPoint by entryPoint<ConfigureConsentWebPageEntryPoint>()

    override fun javascriptInterface() = object {
        @Suppress("unused")
        @JavascriptInterface
        fun progress(progress: String) {
            catch {
                Json.decodeFromString<ConfigureConsentProgress>(progress)
            }.onSuccess {
                entryPoint
                    .configureConsentProgressSender
                    .trySendBlocking(it)
                    .onFailure {
                        Log.e(
                            this::class.simpleName,
                            "Failed to send configure consent progress",
                        )
                    }
            }
        }
    }

    override fun handleBack(): Boolean {
        entryPoint.configureConsentProgressSender.trySendBlocking(ConfigureConsentProgress.NONE)

        return super.handleBack()
    }
}

fun <T> T.showConfigureConsentWebPageTransaction(
    url: String,
): RouterTransaction where T : Controller, T : Redirectable =
    ConfigureConsentWebPageController(
        Bundle().apply {
            putString(ARG_URL, url)
        },
    ).apply {
        targetController = this@showConfigureConsentWebPageTransaction
    }.asTransaction().apply {
        pushChangeHandler(MaterialSharedZAxisChangeHandler())
        popChangeHandler(MaterialSharedZAxisChangeHandler())
    }
