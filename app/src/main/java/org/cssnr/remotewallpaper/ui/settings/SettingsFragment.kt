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
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.api.FeedbackApi
import org.cssnr.remotewallpaper.work.enqueueWorkRequest

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val LOG_TAG = "Settings"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d("SettingsFragment", "rootKey: $rootKey")
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val ctx = requireContext()

        // Widget Settings
        findPreference<Preference>("open_widget_settings")?.setOnPreferenceClickListener {
            Log.d("open_widget_settings", "setOnPreferenceClickListener")
            //findNavController().navigate(R.id.nav_action_widget_settings)
            // TODO: This is the only place SafeArgs is used...
            val action = SettingsFragmentDirections.navActionWidgetSettings()
            findNavController().navigate(action)
            false
        }

        // Screens to Update
        val updateType = findPreference<ListPreference>("set_screens")
        updateType?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

        // Update Interval
        val workInterval = findPreference<ListPreference>("work_interval")
        updateWorkIntervalSettings(workInterval?.value)
        workInterval?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        workInterval?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("work_interval", "newValue: ${newValue as String}")
            updateWorkIntervalSettings(newValue)
            ctx.updateWorkManager(newValue, workInterval.value)
        }

        // Updates on Metered Connection
        val workMeteredPref = findPreference<SwitchPreferenceCompat>("work_metered")
        workMeteredPref?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("work_metered", "newValue: $newValue")
            val result = newValue as Boolean
            Log.d("work_metered", "result: $result")
            workMeteredPref.isChecked = result
            ctx.enqueueWorkRequest()
            false
        }

        // Application Information
        findPreference<Preference>("app_info")?.setOnPreferenceClickListener {
            Log.d("app_info", "showAppInfoDialog")
            ctx.showAppInfoDialog()
            false
        }

        // Open Android Settings
        findPreference<Preference>("android_settings")?.setOnPreferenceClickListener {
            Log.d("android_settings", "setOnPreferenceClickListener")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
            }
            startActivity(intent)
            false
        }

        // Send Feedback
        findPreference<Preference>("send_feedback")?.setOnPreferenceClickListener {
            Log.d("sendFeedback", "setOnPreferenceClickListener")
            ctx.showFeedbackDialog()
            false
        }
    }

    private fun updateWorkIntervalSettings(selectedValue: String?) {
        Log.d("updateWorkIntervalSettings", "selectedValue: $selectedValue")
        if (selectedValue != null) {
            val enabled = selectedValue != "0"
            Log.d("updateWorkIntervalSettings", "enabled: $enabled")
            findPreference<SwitchPreferenceCompat>("work_metered")?.isEnabled = enabled
        }
    }

    private fun Context.updateWorkManager(newValue: String?, curValue: String? = null): Boolean {
        Log.i("updateWorkManager", "newValue: $newValue - curValue: $curValue")
        if (newValue.isNullOrEmpty()) {
            Log.w("updateWorkManager", "newValue.isNullOrEmpty() - false")
            return false
        } else if (curValue == newValue) {
            Log.i("updateWorkManager", "curValue == newValue - false")
            return false
        } else {
            Log.d("updateWorkManager", "ELSE - RESCHEDULING WORK - true")
            if (newValue == "0" || newValue.toLongOrNull() == null) {
                Log.i("updateWorkManager", "DISABLING WORK - newValue is 0 or null")
                WorkManager.getInstance(this).cancelUniqueWork("app_worker")
                return true
            } else {
                enqueueWorkRequest(newValue)
                return true
            }
        }
    }

    fun Context.showFeedbackDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_feedback, null)
        val input = view.findViewById<EditText>(R.id.feedback_input)
        val link = view.findViewById<TextView>(R.id.github_link)

        link.paint?.isUnderlineText = true
        link.setOnClickListener {
            Log.d(LOG_TAG, "link.tag: ${link.tag}")
            startActivity(Intent(Intent.ACTION_VIEW, link.tag.toString().toUri()))
        }

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
        val formattedVersion = getString(
            R.string.version_code_string,
            packageInfo.versionName,
            packageInfo.versionCode.toString()
        )
        Log.d("showAppInfoDialog", "formattedVersion: $formattedVersion")

        appId.text = this.packageName
        appVersion.text = formattedVersion

        appId.text = this.packageName
        appVersion.text = formattedVersion
        sourceLink.text = Html.fromHtml(sourceText, Html.FROM_HTML_MODE_LEGACY)
        sourceLink.movementMethod = LinkMovementMethod.getInstance()
        websiteLink.text = Html.fromHtml(websiteText, Html.FROM_HTML_MODE_LEGACY)
        websiteLink.movementMethod = LinkMovementMethod.getInstance()

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Close", null)
            .create()
            .show()
    }
}
