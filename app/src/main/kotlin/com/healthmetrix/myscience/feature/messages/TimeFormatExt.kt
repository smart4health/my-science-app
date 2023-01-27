package com.healthmetrix.myscience.feature.messages

import java.util.concurrent.TimeUnit

sealed class TimeAgo {
    object Never : TimeAgo()

    object Recent : TimeAgo()

    data class Hours(val number: Int) : TimeAgo()

    data class Days(val number: Int) : TimeAgo()
}

fun Long.toTimeAgo(): TimeAgo {
    if (this <= 0) {
        return TimeAgo.Never
    }

    val numberOfHours = System.currentTimeMillis()
        .minus(this)
        .let(TimeUnit.MILLISECONDS::toHours)

    return when (numberOfHours) {
        0L -> TimeAgo.Recent
        in 1L until 24L -> TimeAgo.Hours(numberOfHours.toInt())
        else -> TimeAgo.Days((numberOfHours / 24L).toInt())
    }
}
