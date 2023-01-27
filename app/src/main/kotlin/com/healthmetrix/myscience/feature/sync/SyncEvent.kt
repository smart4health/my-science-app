package com.healthmetrix.myscience.feature.sync

sealed class SyncEvent {
    object AuthException : SyncEvent()

    object MismatchedAccountError : SyncEvent()
}
