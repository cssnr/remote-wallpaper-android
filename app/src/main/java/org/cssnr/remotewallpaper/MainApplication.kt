package org.cssnr.remotewallpaper

import android.app.Application
import android.content.Context
import android.widget.Toast
import org.acra.config.httpSender
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender

class MainApplication : Application() {
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
}
