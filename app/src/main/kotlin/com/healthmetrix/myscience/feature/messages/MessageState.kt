package com.healthmetrix.myscience.feature.messages

import com.healthmetrix.myscience.service.recontact.RecontactService

/**
 * This state is mapped/related from/to the Recontact Services 'RecontactService.Message.State'
 */
enum class MessageState {
    UNREAD, READ, REPORTED, UNKNOWN;

    fun isUnread() = this == UNREAD

    companion object {
        fun fromState(state: RecontactService.Message.State) = when (state) {
            RecontactService.Message.State.CREATED -> UNREAD
            RecontactService.Message.State.DELIVERED -> UNREAD
            RecontactService.Message.State.READ -> READ
            RecontactService.Message.State.UNKNOWN -> UNKNOWN
        }
    }
}
