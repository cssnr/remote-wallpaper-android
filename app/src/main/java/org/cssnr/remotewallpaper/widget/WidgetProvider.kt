package org.cssnr.remotewallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.cssnr.remotewallpaper.MainActivity
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.db.RemoteDatabase
import org.cssnr.remotewallpaper.log.AppLogs
import org.cssnr.remotewallpaper.ui.home.updateWallpaper
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

// Refresh every widget in a single deterministic pass. Safe to call from any thread;
// the Room query and the RemoteViews build run on a background dispatcher.
fun Context.refreshWidgets() {
    val appWidgetManager = AppWidgetManager.getInstance(this)
    val componentName = ComponentName(this, WidgetProvider::class.java)
    val ids = appWidgetManager.getAppWidgetIds(componentName)
    if (ids.isEmpty()) {
        return
    }
    Log.d("Widget[refreshWidgets]", "ids: ${ids.joinToString()}")
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            WidgetProvider().updateWidgets(this@refreshWidgets, appWidgetManager, ids)
        } catch (e: Exception) {
            Log.e("Widget[refreshWidgets]", "Exception: $e")
        }
    }
}

class WidgetProvider : AppWidgetProvider() {

    companion object {
        // Preference keys rendered by the widget. Keep in sync with the reads in
        // updateWidgets() and add any new key the widget starts displaying here.
        val WIDGET_PREF_KEYS = setOf(
            "widget_text_color",
            "widget_bg_color",
            "widget_bg_opacity",
            "widget_show_icons",
            "set_screens",
            "work_interval",
            "last_update",
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Widget[onReceive]", "intent: $intent")

        when (intent.action) {
            "org.cssnr.remotewallpaper.REFRESH_WIDGET" -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    return
                }
                Log.d("Widget[onReceive]", "REFRESH_WIDGET: START")
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val updateResult = context.updateWallpaper()
                        Log.d("Widget[onReceive]", "context.updateWallpaper: $updateResult")
                        AppLogs.i(context, "Widget: updateWallpaper: $updateResult")
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
                        Log.d("Widget[onReceive]", "REFRESH_WIDGET: DONE")
                    } catch (e: Exception) {
                        Log.e("Widget[onReceive]", "REFRESH_WIDGET: Exception: $e")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                Log.d("Widget[onReceive]", "ACTION_APPWIDGET_UPDATE: START")
                val pendingResult = goAsync()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ?: appWidgetManager.getAppWidgetIds(
                        ComponentName(context, WidgetProvider::class.java)
                    )
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        updateWidgets(context, appWidgetManager, ids)
                        Log.d("Widget[onReceive]", "ACTION_APPWIDGET_UPDATE: DONE")
                    } catch (e: Exception) {
                        Log.e("Widget[onReceive]", "ACTION_APPWIDGET_UPDATE: Exception: $e")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            else -> super.onReceive(context, intent)
        }
    }

    // Builds and applies every widget in one pass. The database is queried up-front so
    // the render is applied with a single updateAppWidget — there is no fire-and-forget
    // update left to a background coroutine that could be lost to process death. Must NOT
    // run on the main thread (Room forbids main-thread queries); callers schedule this on IO.
    internal fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) {
            Log.i("Widget[onUpdate]", "No Widgets")
            return
        }
        Log.i("Widget[onUpdate]", "BEGIN - appWidgetIds: ${appWidgetIds.joinToString()}")

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val dao = RemoteDatabase.getInstance(context).remoteDao()
        val remote = dao.getActive()
        Log.d("Widget[onUpdate]", "remote: ${remote?.url}")

        val bgColor = preferences.getString("widget_bg_color", null) ?: "black"
        Log.d("Widget[onUpdate]", "bgColor: $bgColor")
        val textColor = preferences.getString("widget_text_color", null) ?: "white"
        Log.d("Widget[onUpdate]", "textColor: $textColor")
        val bgOpacity = preferences.getInt("widget_bg_opacity", 35)
        Log.d("Widget[onUpdate]", "bgOpacity: $bgOpacity")
        val workInterval = preferences.getString("work_interval", null)?.toIntOrNull() ?: 0
        Log.d("Widget[onUpdate]", "workInterval: $workInterval")
        //val values = context.resources.getStringArray(R.array.work_interval_values)
        //val entries = context.resources.getStringArray(R.array.work_interval_entries)
        //val updateString = values.indexOf(workInterval).takeIf { it >= 0 }?.let { entries[it] } ?: "Unknown"
        val lastUpdate = preferences.getString("last_update", null)
        Log.d("Widget[onUpdate]", "lastUpdate: $lastUpdate")
        val dateTime = lastUpdate?.let {
            try {
                ZonedDateTime.parse(it, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            } catch (e: Exception) {
                Log.w("Widget[onUpdate]", "Failed to parse lastUpdate", e)
                null
            }
        }
        Log.d("Widget[onUpdate]", "dateTime: $dateTime")
        val showIcons = preferences.getBoolean("widget_show_icons", true)
        Log.d("Widget[onUpdate]", "showIcons: $showIcons")
        val setScreens = preferences.getString("set_screens", "both") ?: "both"
        Log.d("Widget[onUpdate]", "setScreens: $setScreens")

        val colorMap = mapOf(
            "white" to Color.WHITE,
            "black" to Color.BLACK,
            "liberty" to "#565AA9".toColorInt(),
        )

        val selectedBgColor = colorMap[bgColor] ?: Color.BLACK
        Log.d("Widget[onUpdate]", "selectedBgColor: $selectedBgColor")
        val selectedTextColor = colorMap[textColor] ?: Color.WHITE
        Log.d("Widget[onUpdate]", "selectedTextColor: $selectedTextColor")

        val alpha = (bgOpacity * 255 / 100).coerceIn(1, 255)
        val finalBgColor = ColorUtils.setAlphaComponent(selectedBgColor, alpha)
        Log.d("Widget[onUpdate]", "finalBgColor: $finalBgColor")

        // Interval
        val intervalText = when {
            workInterval >= 1440 -> "${workInterval / 1440}d"
            workInterval >= 60 -> "${workInterval / 60}h"
            workInterval > 0 -> "${workInterval}m"
            else -> "Off"
        }
        Log.d("Widget[onUpdate]", "intervalText: $intervalText")

        // Time
        //val time = DateFormat.getTimeFormat(context).format(Date())
        //views.setTextViewText(R.id.update_time, time)
        val timeText = dateTime?.let {
            DateFormat.getTimeFormat(context).format(Date.from(it.toInstant()))
        }
        Log.d("Widget[onUpdate]", "time: $timeText")

        appWidgetIds.forEach { appWidgetId ->
            Log.i("Widget[onUpdate]", "START appWidgetId: $appWidgetId")

            // Widget Root
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val pendingIntent0: PendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply { action = Intent.ACTION_MAIN },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent0)

            // Set Colors
            views.setInt(R.id.widget_root, "setBackgroundColor", finalBgColor)
            views.setInt(R.id.widget_refresh_button, "setColorFilter", selectedTextColor)
            views.setInt(R.id.update_interval_icon, "setColorFilter", selectedTextColor)
            views.setInt(R.id.lock_screen_icon, "setColorFilter", selectedTextColor)
            views.setInt(R.id.home_screen_icon, "setColorFilter", selectedTextColor)
            views.setTextColor(R.id.remote_url, selectedTextColor)
            views.setTextColor(R.id.update_interval, selectedTextColor)
            views.setTextColor(R.id.update_time, selectedTextColor)

            // Show Icons
            views.setViewVisibility(R.id.screen_icons, if (showIcons) View.VISIBLE else View.GONE)

            // Screens
            if (showIcons) {
                when (setScreens) {
                    "lock" -> {
                        views.setViewVisibility(R.id.lock_screen_icon, View.VISIBLE)
                        views.setViewVisibility(R.id.home_screen_icon, View.GONE)
                    }

                    "home" -> {
                        views.setViewVisibility(R.id.lock_screen_icon, View.GONE)
                        views.setViewVisibility(R.id.home_screen_icon, View.VISIBLE)
                    }

                    else -> {
                        views.setViewVisibility(R.id.lock_screen_icon, View.VISIBLE)
                        views.setViewVisibility(R.id.home_screen_icon, View.VISIBLE)
                    }
                }
            }

            // Text
            views.setTextViewText(R.id.remote_url, remote?.url ?: "No Remotes")
            views.setTextViewText(R.id.update_interval, intervalText)
            if (timeText != null) {
                views.setTextViewText(R.id.update_time, timeText)
            }

            // Refresh
            val intent1 = Intent(context, WidgetProvider::class.java).apply {
                action = "org.cssnr.remotewallpaper.REFRESH_WIDGET"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent1 = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, pendingIntent1)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            Log.i("Widget[onUpdate]", "DONE appWidgetId: $appWidgetId")
        }
        Log.i("Widget[onUpdate]", "END - all done")
    }
}


// TODO: Documentation: https://developer.android.com/develop/ui/views/appwidgets#implementing_collections
