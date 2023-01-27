package com.healthmetrix.myscience.feature.login.controller

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.google.android.material.snackbar.Snackbar
import com.healthmetrix.myscience.commons.ui.fade
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.login.Event
import com.healthmetrix.myscience.feature.login.PdfAdapter
import com.healthmetrix.myscience.feature.login.SpacerAdapter
import com.healthmetrix.myscience.feature.login.di.LoginEntryPoint
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerLoginReviewConsentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch

/**
 * Step 5
 *
 * Display the consent pdf for review
 */
class ReviewConsentController : ViewLifecycleController() {

    private val entryPoint by entryPoint<LoginEntryPoint>()

    private var contentUriCache: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerLoginReviewConsentBinding.inflate(inflater, container, false).apply {
            continueButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.continueLoginUseCase(Event.FORWARD)
                }
            }

            backButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.signConsentUseCase.unsign()
                    entryPoint.continueLoginUseCase(Event.BACK)
                }
            }

            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_review_pdf_view_externally -> {
                        requireViewLifecycleOwner.lifecycleScope.launch {
                            try {
                                contentUriCache
                                    ?.pdfIntent()
                                    ?.let(this@ReviewConsentController::startActivity)
                            } catch (ex: ActivityNotFoundException) {
                                Snackbar.make(
                                    root,
                                    container.context.getString(R.string.snackbar_login_review_consent_open_externally_error_text),
                                    Snackbar.LENGTH_SHORT,
                                ).setAnchorView(continueButton).show()
                            }
                        }

                        true
                    }
                    else -> false
                }
            }

            pdfRecyclerView.apply {
                layoutManager = LinearLayoutManager(container.context)

                adapter =
                    ConcatAdapter(PdfAdapter(Dispatchers.Default.asExecutor()), SpacerAdapter())
                addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) = Unit

                        override fun onViewDetachedFromWindow(v: View) {
                            removeOnAttachStateChangeListener(this)
                            (adapter as? ConcatAdapter)
                                ?.adapters
                                ?.filterIsInstance<PdfAdapter>()
                                ?.firstOrNull()
                                ?.close()
                            adapter = null
                        }
                    },
                )
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                progressHorizontal.fade(out = false)

                toolbar.menu
                    .findItem(R.id.menu_item_review_pdf_view_externally)
                    ?.isEnabled = false

                contentUriCache = null

                entryPoint.downloadSignedPdfUseCase()
                    .onSuccess { pair ->
                        (pdfRecyclerView.adapter as? ConcatAdapter)
                            ?.adapters
                            ?.filterIsInstance<PdfAdapter>()
                            ?.firstOrNull()
                            ?.submit(pair.first)

                        contentUriCache = pair.second
                    }
                    .onFailure {
                        Snackbar.make(
                            root,
                            container.context.getString(R.string.snackbar_login_review_consent_download_error_text),
                            Snackbar.LENGTH_SHORT,
                        ).setAnchorView(continueButton).show()
                    }

                toolbar.menu
                    .findItem(R.id.menu_item_review_pdf_view_externally)
                    ?.isEnabled = true

                progressHorizontal.fade(out = true)
            }
        }.root
    }

    private fun Uri.pdfIntent() = Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(this@pdfIntent, "application/pdf")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
}
