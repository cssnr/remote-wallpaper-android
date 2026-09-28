package org.cssnr.remotewallpaper.widget

import android.app.Activity
import android.app.ActivityManager
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.remotewallpaper.log.AppLogs
import org.cssnr.remotewallpaper.ui.home.updateWallpaper

private const val LOG_TAG = "WidgetRefreshActivity"

class WidgetRefreshActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        Log.i(LOG_TAG, "START onCreate: $intent")
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        scope.launch {
            if (!awaitForegroundProcess()) {
                Log.w(LOG_TAG, "timed out waiting for IMPORTANCE_FOREGROUND, setting anyway")
            }
            val updateResult = try {
                withContext(Dispatchers.IO) { updateWallpaper() }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "updateWallpaper: Exception: $e")
                e.message ?: "Unknown Error"
            }
            Log.d(LOG_TAG, "updateWallpaper: $updateResult")
            AppLogs.i(this@WidgetRefreshActivity, "Widget: updateWallpaper: $updateResult")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                withContext(Dispatchers.IO) {
                    WidgetProvider().updateWidgets(
                        this@WidgetRefreshActivity,
                        AppWidgetManager.getInstance(this@WidgetRefreshActivity),
                        intArrayOf(appWidgetId)
                    )
                }
            }
            Log.i(LOG_TAG, "DONE onCreate")
            finish()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun awaitForegroundProcess(timeoutMs: Long = FOREGROUND_TIMEOUT_MS): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        do {
            ActivityManager.getMyMemoryState(state)
            Log.d(LOG_TAG, "importance: ${state.importance}")
            if (state.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true
            }
            delay(FOREGROUND_POLL_INTERVAL_MS)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    private companion object {
        const val FOREGROUND_TIMEOUT_MS = 5_000L
        const val FOREGROUND_POLL_INTERVAL_MS = 250L
    }
}
