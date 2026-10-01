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
    // Because the scope outlives the activity, the work below reads through applicationContext.
    // The coroutine still reaches this instance implicitly through flushWidgets() and
    // finishIfIdle(), and the static REFRESH holds the job until it completes - see TODO.md.
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            Log.e(LOG_TAG, "unhandled: $e", e)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
// Invisible to input: these are window flags, not lifecycle ones. FLAG_NOT_FOCUSABLE
        // documents that it enables FLAG_NOT_TOUCH_MODAL whether or not it is set explicitly
        // (WindowManager.LayoutParams), which is what sends pointer events that land outside this
        // window to the window behind it, and it keeps key events going to the window below - so
        // nothing the user does while the download runs is swallowed by an empty window. What it
        // does NOT do is change the lifecycle: the activity below still goes to PAUSED while this
        // one is on top, and that is a visible cost of this approach (see TODO.md).
        // Neither flag affects why this activity exists at all, which is to get the process up to
        // IMPORTANCE_FOREGROUND - WallpaperManagerService.isFromForegroundApp() tests exactly that
        // (mActivityManager.getPackageImportance() == IMPORTANCE_FOREGROUND), and an activity that
        // reaches RESUMED with its task on top is what puts the process there. Resumed is a
        // lifecycle state, so FLAG_NOT_FOCUSABLE does not stand in the way.
        // NOTE: on AOSP master isFromForegroundApp() does NOT gate setWallpaper() - the result is
        // only recorded on the wallpaper and re-broadcast as EXTRA_FROM_FOREGROUND_APP - so no
        // system enforcement is being leaned on here beyond that test existing. See TODO.md.
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        Log.i(LOG_TAG, "START onCreate: $intent")
        // A relaunch is the restart that the wallpaper change schedules, not a user tap: the
        // wallpaper this activity just set recreates every activity in this process, and if that
        // lands after the update finished, onCreate would download and set the wallpaper all over
        // again. Only a relaunch can be told apart from a tap, and only by the non-null state the
        // restart path passes through: ActivityThread saves a (possibly empty) Bundle before every
        // relaunch - handleRelaunchActivityInner() -> callActivityOnStop(saveState = true) ->
        // callActivityOnSaveInstanceState() -> r.state = new Bundle() - while a genuine tap always
        // launches with a null bundle, so a tap can never be suppressed by this.
        // NOTE: this is a "was recreated" flag, not a "the wallpaper changed" flag. Rotation, night
        // mode, font scale and locale all take the same branch and simply finish early, which is
        // the right outcome anyway once the refresh is done.
        // NOTE: the wallpaper-change restart is best effort and does not fire every time - see
        // TODO.md
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
                        finishIfIdle()
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
                AppLogs.i(applicationContext, "Widget: updateWallpaper: $updateResult")
                Log.i(LOG_TAG, "DONE")
            } finally {
                // Release the slot and mark the refresh complete BEFORE the redraw, which
                // suspends on Dispatchers.IO. Releasing afterwards leaves a window where this job
                // is no longer isActive but REFRESH still points at it: a tap landing there starts
                // a second download, and this block then sets REFRESH_COMPLETED = true and
                // REFRESH = null over the top of that second job - orphaning it, so a third tap
                // cannot attach either and starts a third download, and leaving REFRESH_COMPLETED
                // set with nothing running.
                REFRESH_COMPLETED = true
                REFRESH = null
                // Nested so the invisible activity is always finished, even if the redraw blows up.
                try {
                    flushWidgets()
                } finally {
                    finishIfIdle()
                }
            }
        }
    }

    // Only tear the activity down while no newer refresh has taken over this (singleTask)
    // instance. Once a job releases REFRESH, a tap that arrived during that job's final flush has
    // already started its own job and is using this window; finishing here would pull it out from
    // under that job. Both callers run on the main dispatcher with no suspension between the check
    // and finish(), and startRefresh() also runs there, so a tap cannot slip in between them.
    private fun finishIfIdle() {
        if (REFRESH == null) {
            finish()
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
                    applicationContext,
                    AppWidgetManager.getInstance(applicationContext),
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

        // The job that currently owns this activity, or null when idle. Written in exactly two
        // places: startRefresh() when it launches a job, and that job's own finally before it
        // starts its final flush. Nothing else may clear it - a job that has already released the
        // slot must not touch it again, or it overwrites whatever took over.
        @Volatile
        var REFRESH: Job? = null

        // True once a refresh has run to completion, cleared when a new one starts. Only consulted
        // for a relaunch of this activity, never for a launch from a widget tap. Read and written
        // only on the main dispatcher.
        @Volatile
        var REFRESH_COMPLETED: Boolean = false
    }
}
