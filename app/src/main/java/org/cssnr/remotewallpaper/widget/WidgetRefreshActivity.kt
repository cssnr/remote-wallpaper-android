package org.cssnr.remotewallpaper.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
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
    // The exception handler is required, not optional: these are root coroutines on a
    // SupervisorJob scope, so an escaping exception goes to Thread.uncaughtExceptionHandler and
    // takes the process down with it.
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            Log.e(LOG_TAG, "unhandled: $e", e)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fully transparent to input: the launcher (or whatever is in front) keeps both touch and
        // keyboard focus, so nothing the user does while the download runs is intercepted and the
        // activity below is not paused.
        // Neither flag affects the process importance that this activity exists to obtain.
        // ActivityTaskManagerService.updateTopApp() picks the top app from the top *resumed
        // activity* (window focus is only a fallback for when nothing is resumed), so a resumed
        // non-focusable window still lands the process in PROCESS_STATE_TOP, which
        // RunningAppProcessInfo.procStateToImportance() maps to IMPORTANCE_FOREGROUND - the exact
        // value WallpaperManagerService.isFromForegroundApp() demands.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        Log.i(LOG_TAG, "START onCreate: $intent")
        // A relaunch is the restart that the wallpaper change schedules, not a user tap: the
        // wallpaper this activity just set recreates every activity in this process, and if that
        // lands after the update finished, onCreate would download and set the wallpaper all over
        // again. Only a relaunch can be told apart from a tap, and only by the non-null state the
        // restart path passes through. A genuine tap always launches with a null bundle, so it can
        // never be suppressed by this.
        // NOTE: This guard is unverified - see TODO.md
        startRefresh(intent, relaunched = savedInstanceState != null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.i(LOG_TAG, "START onNewIntent: $intent")
        startRefresh(intent, relaunched = false)
    }

    private fun startRefresh(intent: Intent, relaunched: Boolean) {
        if (relaunched && REFRESH_COMPLETED) {
            Log.i(LOG_TAG, "relaunch after a completed refresh, finishing")
            finish()
            return
        }

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            PENDING_WIDGET_IDS.add(appWidgetId)
        }

        // The wallpaper change restarts this activity and a second widget can tap refresh while
        // the first update is still running. Attach to the in-flight update instead of starting a
        // second download.
        val running = REFRESH
        if (running != null && running.isActive) {
            Log.i(LOG_TAG, "update already in flight, attaching")
            scope.launch {
                try {
                    running.join()
                } finally {
                    // The running job drains PENDING_WIDGET_IDS just before it completes, so a tap
                    // that landed after that drain would otherwise never be redrawn. Flush again
                    // now that the job is done.
                    try {
                        flushWidgets()
                    } finally {
                        finish()
                    }
                }
            }
            return
        }

        REFRESH_COMPLETED = false
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
                Log.i(LOG_TAG, "DONE")
            } finally {
                // Nested so the invisible activity is always finished and the job slot is always
                // released, even if the redraw blows up.
                try {
                    flushWidgets()
                } finally {
                    REFRESH_COMPLETED = true
                    REFRESH = null
                    finish()
                }
            }
        }
    }

    // Redraws every widget that asked since the last flush. Both callers run on the main
    // dispatcher and there is no suspension between the read and the clear, so they cannot
    // interleave and the set is only ever drained once per call.
    // The ids are dropped from PENDING_WIDGET_IDS before the redraw is attempted, so a failure
    // here is logged and lost rather than retried against a half-drained set.
    // NOTE: Only the tapped widget is redrawn - see TODO.md
    private suspend fun flushWidgets() {
        val appWidgetIds = PENDING_WIDGET_IDS.toIntArray()
        PENDING_WIDGET_IDS.clear()
        if (appWidgetIds.isEmpty()) {
            return
        }
        Log.i(LOG_TAG, "flushWidgets: ${appWidgetIds.contentToString()}")
        try {
            withContext(Dispatchers.IO) {
                WidgetProvider().updateWidgets(
                    this@WidgetRefreshActivity,
                    AppWidgetManager.getInstance(this@WidgetRefreshActivity),
                    appWidgetIds
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(LOG_TAG, "flushWidgets: Exception: $e", e)
        }
    }

    private companion object {
        val PENDING_WIDGET_IDS: MutableSet<Int> = ConcurrentHashMap.newKeySet()

        @Volatile
        var REFRESH: Job? = null

        // True once a refresh has run to completion, cleared when a new one starts. Only consulted
        // for a relaunch of this activity, never for a launch from a widget tap. Read and written
        // only on the main dispatcher.
        @Volatile
        var REFRESH_COMPLETED: Boolean = false
    }
}
