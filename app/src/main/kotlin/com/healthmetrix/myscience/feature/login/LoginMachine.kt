package com.healthmetrix.myscience.feature.login

import android.util.Log
import com.healthmetrix.myscience.feature.login.State.CONFIGURE
import com.healthmetrix.myscience.feature.login.State.CONNECT_CHDP
import com.healthmetrix.myscience.feature.login.State.DASHBOARD
import com.healthmetrix.myscience.feature.login.State.FINISHED
import com.healthmetrix.myscience.feature.login.State.REVIEW
import com.healthmetrix.myscience.feature.login.State.SELECT_DATA
import com.healthmetrix.myscience.feature.login.State.SIGN
import com.healthmetrix.myscience.feature.login.State.WELCOME

enum class State {
    WELCOME,
    CONFIGURE,
    SIGN,
    REVIEW,
    CONNECT_CHDP,
    SELECT_DATA,
    FINISHED,
    DASHBOARD,
}

enum class Event {
    FORWARD,
    BACK,
}

data class NavEvent(
    val newState: State,
    val event: Event,
)

fun State.submit(event: Event): State = when (this) {
    WELCOME -> when (event) {
        Event.FORWARD -> CONFIGURE
        Event.BACK -> this
    }
    CONFIGURE -> when (event) {
        Event.FORWARD -> SIGN
        Event.BACK -> WELCOME
    }
    SIGN -> when (event) {
        Event.FORWARD -> REVIEW
        Event.BACK -> CONFIGURE
    }
    REVIEW -> when (event) {
        Event.FORWARD -> CONNECT_CHDP
        Event.BACK -> SIGN
    }
    CONNECT_CHDP -> when (event) {
        Event.FORWARD -> SELECT_DATA
        Event.BACK -> REVIEW
    }
    SELECT_DATA -> when (event) {
        Event.FORWARD -> FINISHED
        Event.BACK -> CONNECT_CHDP
    }
    FINISHED -> when (event) {
        Event.FORWARD -> DASHBOARD
        Event.BACK -> SELECT_DATA
    }
    DASHBOARD -> this // terminal state
}

fun State.toDataStore(): LoginSettings.LoginState = when (this) {
    WELCOME -> LoginSettings.LoginState.WELCOME
    CONFIGURE -> LoginSettings.LoginState.CONFIGURE
    SIGN -> LoginSettings.LoginState.SIGN
    REVIEW -> LoginSettings.LoginState.REVIEW
    CONNECT_CHDP -> LoginSettings.LoginState.CONNECT_CHDP
    SELECT_DATA -> LoginSettings.LoginState.SELECT_DATA
    FINISHED -> LoginSettings.LoginState.FINISHED
    DASHBOARD -> LoginSettings.LoginState.DASHBOARD
}

fun LoginSettings.LoginState.toDomain(): State = when (this) {
    LoginSettings.LoginState.WELCOME -> WELCOME
    LoginSettings.LoginState.GDPR_CONSENT -> WELCOME
    LoginSettings.LoginState.CONFIGURE -> CONFIGURE
    LoginSettings.LoginState.SIGN -> SIGN
    LoginSettings.LoginState.REVIEW -> REVIEW
    LoginSettings.LoginState.CONNECT_CHDP -> CONNECT_CHDP
    LoginSettings.LoginState.SELECT_DATA -> SELECT_DATA
    LoginSettings.LoginState.FINISHED -> FINISHED
    LoginSettings.LoginState.DASHBOARD -> DASHBOARD
    LoginSettings.LoginState.UNRECOGNIZED -> {
        Log.e("LoginMachine", "Unrecognized login state: $this")
        WELCOME
    }
}
