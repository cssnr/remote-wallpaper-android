package org.cssnr.remotewallpaper

import android.app.Application
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import org.acra.config.httpSender
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import org.cssnr.remotewallpaper.db.RemoteDatabase
import org.cssnr.remotewallpaper.widget.refreshWidgets

class MainApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        initAcra {
            // core configuration
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            httpSender {
                uri = BuildConfig.ACRA_URI
                basicAuthLogin = BuildConfig.ACRA_USER
                basicAuthPassword = BuildConfig.ACRA_PASS
                httpMethod = HttpSender.Method.POST
            }
            // toast configuration
            toast {
                text = base.getString(R.string.acra_toast_text)
                length = Toast.LENGTH_LONG
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Refresh the widget whenever the active remote changes so its URL stays in sync
        // without scattering refresh calls across every mutation of the remotes table.
        appScope.launch {
            RemoteDatabase.getInstance(this@MainApplication).remoteDao()
                .observeActive()
                .distinctUntilChangedBy { it?.url }
                .collect { refreshWidgets() }
        }
    }
}
