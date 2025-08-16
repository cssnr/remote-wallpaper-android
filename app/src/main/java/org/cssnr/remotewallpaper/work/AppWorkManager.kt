package org.cssnr.remotewallpaper.work

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun Context.enqueueWorkRequest(
    workInterval: String? = null,
    existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
) {
    Log.i("AppWorkManager", "enqueueWorkRequest: $existingPeriodicWorkPolicy")
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    //val interval = (preferences.getString("work_interval", null) ?: "0").toLong()
    val interval = workInterval?.toLongOrNull()
        ?: preferences.getString("work_interval", null)?.toLongOrNull() ?: 0
    Log.i("AppWorkManager", "interval: $interval")
    if (interval < 15) {
        Log.i("AppWorkManager", "RETURN on interval < 15")
        return
    }
    val workRequestBuilder =
        PeriodicWorkRequestBuilder<AppWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(getWorkerConstraints(preferences))
    if (existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.UPDATE) {
        Log.i("AppWorkManager", "workRequestBuilder.setInitialDelay: $interval")
        workRequestBuilder.setInitialDelay(interval, TimeUnit.MINUTES)
    }
    //val workRequest = workRequestBuilder.build()
    //Log.i("AppWorkManager", "workRequest: $workRequest")
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "app_worker",
        existingPeriodicWorkPolicy,
        workRequestBuilder.build(),
    )
}

fun getWorkerConstraints(preferences: SharedPreferences): Constraints {
    val workMetered = preferences.getBoolean("work_metered", false)
    Log.d("AppWorkManager", "getWorkerConstraints: workMetered: $workMetered")
    val networkType = if (workMetered) NetworkType.CONNECTED else NetworkType.UNMETERED
    Log.d("AppWorkManager", "getWorkerConstraints: networkType: $networkType")
    return Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .setRequiresCharging(false)
        .setRequiresDeviceIdle(false)
        .setRequiredNetworkType(networkType)
        .build()
}

