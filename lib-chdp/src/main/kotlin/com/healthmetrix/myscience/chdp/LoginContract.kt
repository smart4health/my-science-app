package com.healthmetrix.myscience.chdp

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.browser.customtabs.CustomTabsIntent

class LoginContract(
    @ColorInt private val toolbarColor: Int,
) : ActivityResultContract<Intent, Boolean>() {
    override fun createIntent(context: Context, input: Intent) =
        input.apply {
            // revered engineered slightly to match the custom tabs color
            // null access operator ensures that if it breaks, nothing crashes
            (
                (
                    extras
                        ?.get("AUTHORIZATION_INTENT") as? Intent
                    )
                    ?.extras
                    ?.get("authIntent") as? Intent
                )
                ?.putExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, toolbarColor)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}
