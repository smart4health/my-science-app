package com.healthmetrix.myscience.conductor

import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType

fun Controller.doOnNextChangeEnd(block: (Controller, ControllerChangeHandler, ControllerChangeType) -> Unit) {
    addLifecycleListener(
        object : Controller.LifecycleListener() {
            override fun onChangeEnd(
                controller: Controller,
                changeHandler: ControllerChangeHandler,
                changeType: ControllerChangeType,
            ) {
                removeLifecycleListener(this)
                block(controller, changeHandler, changeType)
            }
        },
    )
}
