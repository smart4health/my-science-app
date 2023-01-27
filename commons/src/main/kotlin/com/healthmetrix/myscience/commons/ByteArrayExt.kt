package com.healthmetrix.myscience.commons

import android.util.Base64

fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
