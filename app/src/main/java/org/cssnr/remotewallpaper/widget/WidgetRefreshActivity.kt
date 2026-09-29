package org.cssnr.remotewallpaper.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.remotewallpaper.log.AppLogs
import org.cssnr.remotewallpaper.ui.home.updateWallpaper
import java.util.concurrent.ConcurrentHashMap

private const val LOG_TAG = "WidgetRefreshActivity"

class WidgetRefreshActivity : Activity() {

    // Deliberately NOT cancelled in onDestroy. Setting the wallpaper makes Android 12+ restart
    // every activity in this process, so a scope tied to this instance would cancel the very
    // update it exists to perform.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        Log.i(LOG_TAG, "START onCreate: $intent")
        startRefresh(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.i(LOG_TAG, "START onNewIntent: $intent")
        startRefresh(intent)
    }

    private fun startRefresh(intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            PENDING_WIDGET_IDS.add(appWidgetId)
        }

        // The wallpaper change restarts this activity and a second widget can tap refresh while
        // the first update is still running. Attach to the in-flight update instead of starting a
        // second download; it drains PENDING_WIDGET_IDS when it finishes.
        val running = REFRESH
        if (running != null && running.isActive) {
            Log.i(LOG_TAG, "update already in flight, attaching")
            scope.launch {
                try {
                    running.join()
                } finally {
                    finish()
                }
            }
            return
        }

        REFRESH = scope.launch {
            try {
                val updateResult = try {
                    withContext(Dispatchers.IO) { updateWallpaper() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "updateWallpaper: Exception: $e")
                    e.message ?: "Unknown Error"
                }
                Log.d(LOG_TAG, "updateWallpaper: $updateResult")
                AppLogs.i(this@WidgetRefreshActivity, "Widget: updateWallpaper: $updateResult")
                val appWidgetIds = PENDING_WIDGET_IDS.toIntArray()
                PENDING_WIDGET_IDS.clear()
                if (appWidgetIds.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        WidgetProvider().updateWidgets(
                            this@WidgetRefreshActivity,
                            AppWidgetManager.getInstance(this@WidgetRefreshActivity),
                            appWidgetIds
                        )
                    }
                }
                Log.i(LOG_TAG, "DONE onCreate")
            } finally {
                REFRESH = null
                finish()
            }
        }
    }

    private companion object {
        val PENDING_WIDGET_IDS = ConcurrentHashMap.newKeySet<Int>()

        @Volatile
        var REFRESH: Job? = null
    }
}
