package com.healthmetrix.myscience.feature.webpage

import android.net.Uri

/**
 * Controllers implementing redirectable receive redirects
 * when links are clicked in the web view
 */
interface Redirectable {
    fun onRedirect(uri: Uri): Action

    enum class Action {
        IGNORE,
        POP,
        OPEN_EXTERNAL,
    }
}
