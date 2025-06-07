package org.cssnr.remotewallpaper.ui.setup

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cssnr.remotewallpaper.MainActivity
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.databinding.FragmentSetupBinding
import org.cssnr.remotewallpaper.db.RemoteDatabase
import org.cssnr.remotewallpaper.work.APP_WORKER_CONSTRAINTS
import org.cssnr.remotewallpaper.work.AppWorker
import java.util.concurrent.TimeUnit

class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    companion object {
        const val LOG_TAG = "SetupFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "onViewCreated: ${savedInstanceState?.size()}")

        val ctx = requireContext()

        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(MainActivity.Companion.LOG_TAG, "versionName: $versionName")
        binding.appVersion.text = getString(R.string.version_string, versionName)

        // Update Interval Spinner
        val entries = resources.getStringArray(R.array.work_interval_entries)
        val values = resources.getStringArray(R.array.work_interval_values)
        val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, entries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.workIntervalSpinner.adapter = adapter
        binding.workIntervalSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedValue = values[position]
                    Log.d(LOG_TAG, "workIntervalSpinner: value: $selectedValue")
                    preferences.edit {
                        putString("work_interval", selectedValue)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    Log.w(LOG_TAG, "workIntervalSpinner: No Item Selected")
                }
            }

        // Update Screen Spinner
        val screenEntries = resources.getStringArray(R.array.set_screens_entries)
        val screenValues = resources.getStringArray(R.array.set_screens_values)
        val screenAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, screenEntries)
        screenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.setScreensSpinner.adapter = screenAdapter
        binding.setScreensSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedValue = screenValues[position]
                    Log.d(LOG_TAG, "setScreensSpinner: value: $selectedValue")
                    preferences.edit {
                        putString("set_screens", selectedValue)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    Log.w(LOG_TAG, "setScreensSpinner: No Item Selected")
                }
            }

        // Initial Provider Radio
        binding.initialProvider.check(R.id.option_picsum)
        //binding.optionCustom.setOnCheckedChangeListener { _, isChecked ->
        //    Log.d("RadioButton", "Checked: $isChecked")
        //    binding.customUrl.visibility = if (isChecked) View.VISIBLE else View.GONE
        //}

        val startAppListener: (View) -> Unit = { view ->
            Log.d(LOG_TAG, "startAppListener: view: $view")

            binding.btnStart.isEnabled = false
            binding.btnDownload.isEnabled = false

            // TODO: Duplication from SettingsFragment and MainActivity...
            val workInterval = preferences.getString("work_interval", null) ?: "0"
            Log.d(LOG_TAG, "startAppListener: workInterval: $workInterval")
            if (workInterval != "0") {
                val interval = workInterval.toLong()
                val newRequest =
                    PeriodicWorkRequestBuilder<AppWorker>(interval, TimeUnit.MINUTES)
                        .setInitialDelay(interval, TimeUnit.MINUTES)
                        .setConstraints(APP_WORKER_CONSTRAINTS)
                        .build()
                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    "app_worker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    newRequest
                )
            }

            // Arguments
            val bundle = bundleOf()
            when (view.id) {
                R.id.btn_download -> {
                    Log.i(LOG_TAG, "Download Button Pressed: update_wallpaper")
                    bundle.putBoolean("update_wallpaper", true)
                }
            }
            Log.d(LOG_TAG, "startAppListener: bundle: $bundle")

            // Selected Remote
            val selectedId = binding.initialProvider.checkedRadioButtonId
            Log.d(LOG_TAG, "startAppListener: selectedId: $selectedId")
            val selectedText = binding.root.findViewById<RadioButton>(selectedId).text.toString()
            Log.d(LOG_TAG, "startAppListener: selectedText: $selectedText")
            if (selectedText != "https://picsum.photos/4800/2400") {
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = RemoteDatabase.getInstance(ctx).remoteDao()
                    val remote = dao.getByUrl(selectedText)
                    Log.d(LOG_TAG, "startAppListener: remote: $remote")
                    dao.activate(remote)
                }
            }

            // Navigate Home
            findNavController().navigate(
                R.id.nav_action_setup_home,
                bundle,
                NavOptions.Builder()
                    .setPopUpTo(R.id.nav_item_setup, true)
                    .build()
            )
        }
        binding.btnDownload.setOnClickListener(startAppListener)
        binding.btnStart.setOnClickListener(startAppListener)
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(LOG_TAG, "Creating Initial Data")
            val dao = RemoteDatabase.getInstance(ctx).remoteDao()
            dao.getAll()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "onResume - Hide UI and Lock Drawer")
        val act = requireActivity()
        act.findViewById<ConstraintLayout>(R.id.content_main_layout).setPadding(0, 0, 0, 0)
        act.findViewById<Toolbar>(R.id.toolbar).visibility = View.GONE
        act.findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE
        (activity as? MainActivity)?.setDrawerLockMode(false)
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "onPause - Show UI and Unlock Drawer")
        val act = requireActivity()
        val padding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics
        ).toInt()
        act.findViewById<ConstraintLayout>(R.id.content_main_layout).setPadding(0, 0, 0, padding)
        act.findViewById<Toolbar>(R.id.toolbar).visibility = View.VISIBLE
        act.findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.VISIBLE
        (activity as? MainActivity)?.setDrawerLockMode(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "onDestroyView")
        _binding = null
    }
}
