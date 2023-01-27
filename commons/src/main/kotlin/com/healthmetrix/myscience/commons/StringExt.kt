package com.healthmetrix.myscience.commons

import java.net.URLEncoder

fun String.urlEncode(): String {
    return URLEncoder.encode(this, "UTF-8")
}
