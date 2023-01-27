package com.healthmetrix.myscience.feature.dashboard

sealed class DashboardEvent {
    object AuthFailure : DashboardEvent()

    data class ViewMessage(
        val messageId: String,
    ) : DashboardEvent()

    object GetStatisticsFailure : DashboardEvent()

    object RevokeConsentFailure : DashboardEvent()
}
