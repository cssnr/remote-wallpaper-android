package org.cssnr.remotewallpaper.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.api.FeedbackApi
import org.cssnr.remotewallpaper.work.APP_WORKER_CONSTRAINTS
import org.cssnr.remotewallpaper.work.AppWorker
import java.util.concurrent.TimeUnit

//import com.google.firebase.analytics.ktx.analytics
//import com.google.firebase.ktx.Firebase

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val LOG_TAG = "Settings"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d("SettingsFragment", "rootKey: $rootKey")
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val ctx = requireContext()

        // Background Update Interval
        val workInterval = findPreference<ListPreference>("work_interval")
        workInterval?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        workInterval?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("work_interval", "newValue: $newValue")
            ctx.updateWorkManager(workInterval, newValue)
        }

        // Update Screens
        val updateType = findPreference<ListPreference>("set_screens")
        updateType?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

        // Widget Settings
        findPreference<Preference>("open_widget_settings")?.setOnPreferenceClickListener {
            Log.d("open_widget_settings", "setOnPreferenceClickListener")
            //findNavController().navigate(R.id.nav_action_widget_settings)
            // TODO: This is the only place SafeArgs is used...
            val action = SettingsFragmentDirections.navActionWidgetSettings()
            findNavController().navigate(action)
            false
        }

        //// Toggle Analytics
        //val analyticsEnabled = findPreference<SwitchPreferenceCompat>("analytics_enabled")
        //analyticsEnabled?.setOnPreferenceChangeListener { _, newValue ->
        //    Log.d("analyticsEnabled", "analytics_enabled: $newValue")
        //    ctx.toggleAnalytics(analyticsEnabled, newValue)
        //    false
        //}

        // Send Feedback
        findPreference<Preference>("send_feedback")?.setOnPreferenceClickListener {
            Log.d("sendFeedback", "setOnPreferenceClickListener")
            ctx.showFeedbackDialog()
            false
        }

        // Show App Info
        findPreference<Preference>("app_info")?.setOnPreferenceClickListener {
            Log.d("app_info", "showAppInfoDialog")
            ctx.showAppInfoDialog()
            false
        }

        // Open App Settings
        findPreference<Preference>("android_settings")?.setOnPreferenceClickListener {
            Log.d("android_settings", "setOnPreferenceClickListener")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
            }
            startActivity(intent)
            false
        }
    }

    fun Context.updateWorkManager(listPref: ListPreference, newValue: Any): Boolean {
        Log.d("updateWorkManager", "listPref: ${listPref.value} - newValue: $newValue")
        val value = newValue as? String
        Log.d("updateWorkManager", "String value: $value")
        if (value.isNullOrEmpty()) {
            Log.w("updateWorkManager", "NULL OR EMPTY - false")
            return false
        } else if (listPref.value == value) {
            Log.i("updateWorkManager", "NO CHANGE - false")
            return false
        } else {
            Log.i("updateWorkManager", "RESCHEDULING WORK - true")
            val interval = value.toLongOrNull()
            Log.i("updateWorkManager", "interval: $interval")
            if (interval == null || interval == 0L) {
                Log.i("updateWorkManager", "DISABLING WORK")
                WorkManager.getInstance(this).cancelUniqueWork("app_worker")
                return true
            } else {
                val newRequest =
                    PeriodicWorkRequestBuilder<AppWorker>(interval, TimeUnit.MINUTES)
                        .setInitialDelay(interval, TimeUnit.MINUTES)
                        .setConstraints(APP_WORKER_CONSTRAINTS)
                        .build()
                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "app_worker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    newRequest
                )
                return true
            }
        }
    }

    //fun Context.toggleAnalytics(switchPreference: SwitchPreferenceCompat, newValue: Any) {
    //    Log.d("toggleAnalytics", "newValue: $newValue")
    //    if (newValue as Boolean) {
    //        Log.d("toggleAnalytics", "ENABLE Analytics")
    //        Firebase.analytics.setAnalyticsCollectionEnabled(true)
    //        switchPreference.isChecked = true
    //    } else {
    //        MaterialAlertDialogBuilder(this)
    //            .setTitle("Please Reconsider")
    //            .setMessage("Analytics are only used to fix bugs and make improvements.")
    //            .setPositiveButton("Disable Anyway") { _, _ ->
    //                Log.d("toggleAnalytics", "DISABLE Analytics")
    //                Firebase.analytics.logEvent("disable_analytics", null)
    //                Firebase.analytics.setAnalyticsCollectionEnabled(false)
    //                switchPreference.isChecked = false
    //            }
    //            .setNegativeButton("Cancel", null)
    //            .show()
    //    }
    //}

    fun Context.showFeedbackDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_feedback, null)
        val input = view.findViewById<EditText>(R.id.feedback_input)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send", null)
            .create()

        dialog.setOnShowListener {
            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            sendButton.setOnClickListener {
                sendButton.isEnabled = false
                val message = input.text.toString().trim()
                Log.d("showFeedbackDialog", "message: $message")
                if (message.isNotEmpty()) {
                    val api = FeedbackApi(this)
                    lifecycleScope.launch {
                        val response = withContext(Dispatchers.IO) { api.sendFeedback(message) }
                        Log.d("showFeedbackDialog", "response: $response")
                        val msg = if (response.isSuccessful) {
                            findPreference<Preference>("send_feedback")?.isEnabled = false
                            dialog.dismiss()
                            "Feedback Sent. Thank You!"
                        } else {
                            sendButton.isEnabled = true
                            //val params = Bundle().apply {
                            //    putString("message", response.message())
                            //    putString("code", response.code().toString())
                            //}
                            //Firebase.analytics.logEvent("send_feedback_failed", params)
                            "Error: ${response.code()}"
                        }
                        Log.d("showFeedbackDialog", "msg: $msg")
                        Toast.makeText(this@showFeedbackDialog, msg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    sendButton.isEnabled = true
                    input.error = "Feedback is Required"
                }
            }

            input.requestFocus()

            val link = view.findViewById<TextView>(R.id.github_link)
            Log.d(LOG_TAG, "link: $link")
            Log.d(LOG_TAG, "link.tag: ${link.tag}")
            val linkText = getString(R.string.github_link, link.tag)
            link.text = Html.fromHtml(linkText, Html.FROM_HTML_MODE_LEGACY)
            link.movementMethod = LinkMovementMethod.getInstance()

            //val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Send") { _, _ -> }
        dialog.show()
    }

    fun Context.showAppInfoDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_app_info, null)
        val appId = view.findViewById<TextView>(R.id.app_identifier)
        val appVersion = view.findViewById<TextView>(R.id.app_version)
        val sourceLink = view.findViewById<TextView>(R.id.source_link)
        val websiteLink = view.findViewById<TextView>(R.id.website_link)

        val sourceText = getString(R.string.github_link, sourceLink.tag)
        Log.d(LOG_TAG, "sourceText: $sourceText")

        val websiteText = getString(R.string.website_link, websiteLink.tag)
        Log.d(LOG_TAG, "websiteText: $websiteText")

        val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")

        val formattedVersion = getString(R.string.version_string, versionName)
        Log.d(LOG_TAG, "formattedVersion: $formattedVersion")

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Close", null)
            .create()

        dialog.setOnShowListener {
            appId.text = this.packageName
            appVersion.text = formattedVersion
            sourceLink.text = Html.fromHtml(sourceText, Html.FROM_HTML_MODE_LEGACY)
            sourceLink.movementMethod = LinkMovementMethod.getInstance()
            websiteLink.text = Html.fromHtml(websiteText, Html.FROM_HTML_MODE_LEGACY)
            websiteLink.movementMethod = LinkMovementMethod.getInstance()
        }
        dialog.show()
    }
}
