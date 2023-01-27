package com.healthmetrix.myscience.feature.webpage

import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.asTransaction
import com.healthmetrix.myscience.conductor.MaterialSharedZAxisChangeHandler

fun <T> T.showPageTransaction(
    url: String,
): RouterTransaction where T : Controller, T : Redirectable =
    WebPageController(
        Bundle().apply {
            putString(ARG_URL, url)
        },
    ).apply {
        targetController = this@showPageTransaction
    }.asTransaction().apply {
        pushChangeHandler(MaterialSharedZAxisChangeHandler())
        popChangeHandler(MaterialSharedZAxisChangeHandler())
    }
