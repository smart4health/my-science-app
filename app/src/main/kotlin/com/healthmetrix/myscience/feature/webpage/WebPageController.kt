package com.healthmetrix.myscience.feature.webpage

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bluelinelabs.conductor.Controller
import com.healthmetrix.s4h.myscience.databinding.ControllerWebPageBinding

const val ARG_URL = "ARG_URL"

open class WebPageController(
    bundle: Bundle,
) : Controller(bundle) {

    private val url = args.getString(ARG_URL)!!

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerWebPageBinding.inflate(inflater, container, false).apply {
            with(webView) {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        request?.url?.let { redirectUri ->
                            val linkAction = (targetController as? Redirectable)
                                ?.onRedirect(redirectUri)
                                ?: Redirectable.Action.IGNORE

                            when (linkAction) {
                                Redirectable.Action.IGNORE -> Unit
                                Redirectable.Action.POP -> router.popCurrentController()
                                Redirectable.Action.OPEN_EXTERNAL -> {
                                    val i = Intent().apply {
                                        action = Intent.ACTION_VIEW
                                        data = redirectUri
                                    }

                                    try {
                                        startActivity(i)
                                    } catch (ex: ActivityNotFoundException) {
                                        Log.e(
                                            this::class.simpleName,
                                            "Unable to open url externally",
                                        )
                                    }
                                }
                            }
                        }

                        return true // do not ever navigate inside the webview
                    }
                }

                javascriptInterface()?.let { i ->
                    webView.addJavascriptInterface(i, "Android")
                }

                settings.javaScriptEnabled = true
                loadUrl(this@WebPageController.url)
            }
        }.root
    }

    open fun javascriptInterface(): Any? = null
}
