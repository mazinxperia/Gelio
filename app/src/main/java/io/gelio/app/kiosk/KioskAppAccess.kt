package io.gelio.app.kiosk

import android.content.Context
import android.content.Intent
import io.gelio.app.MainActivity
import io.gelio.app.app.AppContainer
import io.gelio.app.app.GelioApp

internal fun Context.appContainer(): AppContainer =
    (applicationContext as GelioApp).appContainer

internal fun Context.launchGelioToFront() {
    val intent = Intent(this, MainActivity::class.java).apply {
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
        )
    }
    startActivity(intent)
}
