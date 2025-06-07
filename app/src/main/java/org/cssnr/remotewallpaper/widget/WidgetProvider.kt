package org.cssnr.remotewallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.format.DateFormat
import android.util.Log
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.preference.PreferenceManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.cssnr.remotewallpaper.MainActivity
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.db.RemoteDatabase
import org.cssnr.remotewallpaper.ui.home.updateWallpaper
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

class WidgetProvider : AppWidgetProvider() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("Widget[onReceive]", "intent: $intent")

        if (intent.action == "org.cssnr.remotewallpaper.REFRESH_WIDGET") {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return
            }
            Log.d("Widget[onReceive]", "GlobalScope.launch: START")
            GlobalScope.launch(Dispatchers.IO) {
                val update = context.updateWallpaper()
                Log.d("Widget[onReceive]", "context.updateWallpaper: $update")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
                Log.d("Widget[onReceive]", "GlobalScope.launch: DONE")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) {
            Log.i("Widget[onUpdate]", "No Widgets")
            return
        }
        Log.i("Widget[onUpdate]", "BEGIN - appWidgetIds: ${appWidgetIds.joinToString()}")

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val bgColor = preferences.getString("widget_bg_color", null) ?: "black"
        Log.d("Widget[onUpdate]", "bgColor: $bgColor")
        val textColor = preferences.getString("widget_text_color", null) ?: "white"
        Log.d("Widget[onUpdate]", "textColor: $textColor")
        val bgOpacity = preferences.getInt("widget_bg_opacity", 35)
        Log.d("Widget[onUpdate]", "bgOpacity: $bgOpacity")
        val workInterval = preferences.getString("work_interval", null) ?: "0"
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

        val colorMap = mapOf(
            "white" to Color.WHITE,
            "black" to Color.BLACK,
            "liberty" to "#565AA9".toColorInt(),
        )

        val selectedBgColor = colorMap[bgColor] ?: Color.BLACK
        Log.d("Widget[onUpdate]", "selectedBgColor: $selectedBgColor")
        val selectedTextColor = colorMap[textColor] ?: Color.WHITE
        Log.d("Widget[onUpdate]", "selectedTextColor: $selectedTextColor")

        val opacityPercent = bgOpacity
        val alpha = (opacityPercent * 255 / 100).coerceIn(1, 255)
        val finalBgColor = ColorUtils.setAlphaComponent(selectedBgColor, alpha)
        Log.d("Widget[onUpdate]", "finalBgColor: $finalBgColor")

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
            views.setTextColor(R.id.remote_url, selectedTextColor)
            views.setTextColor(R.id.update_interval, selectedTextColor)
            views.setTextColor(R.id.update_time, selectedTextColor)

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

            // TODO: Determine if this should be outside of the loop, somehow...
            val dao = RemoteDatabase.getInstance(context).remoteDao()
            GlobalScope.launch(Dispatchers.IO) {
                val remote = dao.getActive()
                // Url
                Log.d("Widget[onUpdate]", "remote: ${remote?.url}")
                views.setTextViewText(R.id.remote_url, remote?.url ?: "No Remotes")

                // Interval
                val interval = if (workInterval != "0") workInterval else "Off"
                Log.d("Widget[onUpdate]", "interval: $interval")
                views.setTextViewText(R.id.update_interval, interval)

                // Time
                //val time = DateFormat.getTimeFormat(context).format(Date())
                //views.setTextViewText(R.id.update_time, time)
                if (dateTime != null) {
                    val instant = dateTime.toInstant()
                    val date = Date.from(instant)
                    val time = DateFormat.getTimeFormat(context).format(date)
                    Log.d("Widget[onUpdate]", "time: $time")
                    views.setTextViewText(R.id.update_time, time)
                }

                // Done
                Log.i("Widget[onUpdate]", "appWidgetManager.updateAppWidget: $appWidgetId")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
            Log.i("Widget[onUpdate]", "DONE appWidgetId: $appWidgetId")
        }
        Log.i("Widget[onUpdate]", "END - all done")
    }
}


// TODO: Documentation: https://developer.android.com/develop/ui/views/appwidgets#implementing_collections
