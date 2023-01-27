package com.healthmetrix.myscience

import android.app.Activity
import android.app.Application
import com.bluelinelabs.conductor.Controller
import dagger.hilt.EntryPoints

inline fun <reified T> Application.entryPoint() = lazy {
    EntryPoints.get(applicationContext, T::class.java)!!
}

inline fun <reified T> Activity.entryPoint() = lazy {
    EntryPoints.get(applicationContext, T::class.java)!!
}

inline fun <reified T> Controller.entryPoint() = lazy {
    EntryPoints.get(applicationContext!!, T::class.java)!!
}
