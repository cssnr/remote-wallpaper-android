package org.cssnr.remotewallpaper.work

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.cssnr.remotewallpaper.MainActivity.Companion.LOG_TAG
import org.cssnr.remotewallpaper.ui.home.updateWallpaper
import org.cssnr.remotewallpaper.widget.WidgetProvider

class AppWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.i("AppWorker", "START: doWork")

        // Check Work Interval
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val workInterval = preferences.getString("work_interval", null) ?: "0"
        Log.d("AppWorker", "workInterval: $workInterval")
        if (workInterval == "0") {
            Log.i(LOG_TAG, "Work is Disabled.")
            return Result.success()
        }

        // Update Wallpaper
        applicationContext.updateWallpaper()

        // Update Widget
        Log.d("AppWorker", "Update Widget")
        val componentName = ComponentName(applicationContext, WidgetProvider::class.java)
        Log.d("AppWorker", "componentName: $componentName")
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setClassName(
            applicationContext.packageName,
            "org.cssnr.remotewallpaper.widget.WidgetProvider"
        ).apply {
            val ids =
                AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(componentName)
            Log.d("AppWorker", "ids: $ids")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        Log.d("AppWorker", "sendBroadcast: $intent")
        applicationContext.sendBroadcast(intent)

        Log.i("AppWorker", "DONE: doWork")
        return Result.success()
    }
}
