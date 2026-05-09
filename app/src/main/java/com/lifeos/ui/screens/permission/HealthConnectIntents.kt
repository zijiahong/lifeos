package com.lifeos.ui.screens.permission

import android.content.Context
import android.content.Intent
import android.net.Uri

private const val HC_PACKAGE = "com.google.android.apps.healthdata"

internal fun openHealthConnectStore(context: Context) {
    val marketIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=$HC_PACKAGE")
        setPackage("com.android.vending")
    }
    try {
        context.startActivity(marketIntent)
    } catch (_: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$HC_PACKAGE"))
        )
    }
}
