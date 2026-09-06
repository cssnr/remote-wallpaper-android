package org.cssnr.remotewallpaper

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Context.showSnackbar(message: CharSequence) {
    val activity = findActivity() ?: return
    if (activity.isFinishing || activity.isDestroyed) return
    val host = activity.findViewById<ViewGroup>(R.id.snackbar_host) ?: return
    Snackbar.make(host, message, Snackbar.LENGTH_LONG)
        .setCloseIconVisible(true)
        .show()
}
