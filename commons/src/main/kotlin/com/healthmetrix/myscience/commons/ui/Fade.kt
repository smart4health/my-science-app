package com.healthmetrix.myscience

/**
 * re-entrant fading animation extension function
 * make sure views are ready by starting as GONE and alpha of 0
 */
// fun View.fade(out: Boolean) {
//    if (!out)
//        visibility = View.VISIBLE
//
//    animate().apply {
//        cancel()
//        duration = context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
//        alpha(if (out) 0f else 1f)
//
//        setListener(null)
//
//        if (out) withEndAction {
//            visibility = View.GONE
//        }
//    }
// }
