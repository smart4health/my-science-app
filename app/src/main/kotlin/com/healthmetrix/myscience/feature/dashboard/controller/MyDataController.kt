package com.healthmetrix.myscience.feature.dashboard.controller

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.ColorInt
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.github.michaelbull.result.Err
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.healthmetrix.myscience.chdp.LoginContract
import com.healthmetrix.myscience.commons.ui.fade
import com.healthmetrix.myscience.conductor.ViewLifecycleController
import com.healthmetrix.myscience.entryPoint
import com.healthmetrix.myscience.feature.dashboard.DashboardEvent
import com.healthmetrix.myscience.feature.dashboard.di.DashboardEntryPoint
import com.healthmetrix.myscience.feature.dataselection.toDataSelectionSettings
import com.healthmetrix.myscience.feature.dataselection.toEntries
import com.healthmetrix.myscience.feature.sync.SYNC_ERROR_NOTIFICATION_ID
import com.healthmetrix.myscience.feature.sync.SyncEvent
import com.healthmetrix.myscience.feature.sync.SyncLoadingState
import com.healthmetrix.s4h.myscience.R
import com.healthmetrix.s4h.myscience.databinding.ControllerDashboardMyDataBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MyDataController : ViewLifecycleController() {

    private val entryPoint by entryPoint<DashboardEntryPoint>()

    private lateinit var loginLauncher: ActivityResultLauncher<Intent>
    private var contractCallback: ((Boolean) -> Unit)? = null

    override fun onContextAvailable(context: Context) {
        loginLauncher = (activity as ComponentActivity).activityResultRegistry
            .register(
                "d4l_login_launcher",
                this@MyDataController,
                LoginContract(context.primaryColor),
            ) { isSuccessful ->
                contractCallback?.invoke(isSuccessful)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?,
    ): View {
        return ControllerDashboardMyDataBinding.inflate(inflater, container, false).apply {
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_dashboard_logout -> {
                        requireViewLifecycleOwner.lifecycleScope.launch {
                            entryPoint.logOutUseCase()
                        }
                        true
                    }
                    R.id.menu_item_dashboard_oss -> {
                        Intent(container.context, OssLicensesMenuActivity::class.java)
                            .let(this@MyDataController::startActivity)
                        true
                    }
                    else -> false
                }
            }

            sectionLoginErrorButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint
                        .chdpClient
                        .loginIntent(container.context)
                        .let(loginLauncher::launch)
                }
            }

            contractCallback = { isSuccessful ->
                requireViewLifecycleOwner.lifecycleScope.launch {
                    if (isSuccessful) entryPoint.chdpReloginUseCase()
                }
            }

            sectionDataSelectionDataSelectionView.onEntriesChangedListener = { entries ->
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.dataSelectionSettingsDataStore.updateData {
                        entries.toDataSelectionSettings()
                    }
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.dataSelectionSettingsDataStore.data.collect { dataSelectionSettings ->
                    dataSelectionSettings
                        .toEntries(container.context.resources)
                        .let(sectionDataSelectionDataSelectionView::setEntries)
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.syncSettingsDataStore.data.collect { syncSettings ->
                    sectionLoginError.visibility =
                        if (syncSettings.flagAuthFailed) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    sectionBackgroundSharingEnableSwitch.isChecked =
                        syncSettings.isBackgroundSharingEnabled
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.syncLoadingStateReadonlyStateFlow.collect { loadingState ->
                    progressHorizontal.fade(out = loadingState != SyncLoadingState.IN_PROGRESS)

                    sectionBackgroundSharingShareNowButton.isEnabled = loadingState in listOf(
                        SyncLoadingState.READY,
                        SyncLoadingState.FAILED,
                    )
                }
            }

            sectionBackgroundSharingEnableSwitch.setOnCheckedChangeListener { _, isChecked ->
                requireViewLifecycleOwner.lifecycleScope.launch {
                    entryPoint.syncSettingsDataStore.updateData { syncSettings ->
                        syncSettings.toBuilder()
                            .setIsBackgroundSharingEnabled(isChecked)
                            .build()
                    }
                }
            }

            sectionBackgroundSharingShareNowButton.setOnClickListener {
                entryPoint.applicationScope.launch {
                    entryPoint.fullSyncUseCase()
                }
            }

            sectionRevokeRevokeButton.setOnClickListener {
                requireViewLifecycleOwner.lifecycleScope.launch {
                    val shouldRevoke = suspendCoroutine<Boolean> { cont ->
                        MaterialAlertDialogBuilder(root.context)
                            .setTitle(R.string.dashboard_my_data_revoke_dialog_title)
                            .setMessage(R.string.dashboard_my_data_revoke_dialog_message)
                            .setPositiveButton(R.string.dashboard_my_data_revoke_dialog_positive) { _, _ ->
                                cont.resume(true)
                            }
                            .setNegativeButton(R.string.dashboard_my_data_revoke_dialog_negative) { _, _ ->
                                cont.resume(false)
                            }
                            .setOnCancelListener {
                                cont.resume(false)
                            }
                            .setCancelable(true)
                            .show()
                    }

                    if (shouldRevoke) {
                        sectionRevokeRevokeButton.isEnabled = false
                        progressHorizontal.fade(out = false)

                        if (entryPoint.submitConsentUseCase(revoke = true) is Err) {
                            entryPoint.dashboardEventSender.send(DashboardEvent.RevokeConsentFailure)
                        } else entryPoint.logOutUseCase()

                        progressHorizontal.fade(out = true)
                        sectionRevokeRevokeButton.isEnabled = true
                    }
                }
            }

            requireViewLifecycleOwner.lifecycleScope.launch {
                entryPoint.syncEventSharedFlow.collect { event ->
                    if (event == SyncEvent.AuthException) {
                        scrollView.smoothScrollTo(0, 0)
                    }
                }
            }
        }.root
    }

    override fun onAttach(view: View) {
        NotificationManagerCompat.from(view.context)
            .cancel(SYNC_ERROR_NOTIFICATION_ID)
    }

    override fun onDestroyView(view: View) {
        contractCallback = null
    }
}

// Same as in ConnectChdpController
@get:ColorInt
private val Context.primaryColor: Int
    get() = TypedValue().let { typedValue ->
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        typedValue.data
    }
