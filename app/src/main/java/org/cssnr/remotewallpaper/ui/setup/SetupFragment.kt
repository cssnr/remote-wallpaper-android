package org.cssnr.remotewallpaper.ui.setup

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.remotewallpaper.MainActivity
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.databinding.FragmentSetupBinding
import org.cssnr.remotewallpaper.db.Remote
import org.cssnr.remotewallpaper.db.RemoteDatabase
import org.cssnr.remotewallpaper.work.enqueueWorkRequest
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    private val viewModel: SetupViewModel by viewModels()

    private var mainActivity: MainActivity? = null

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

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "onDestroyView")
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "onViewCreated - SetupFragment: ${savedInstanceState?.size()}")

        val ctx = requireContext()

        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")
        binding.appVersion.text = getString(R.string.version_string, versionName)

        //binding.workIntervalSpinner.setOnTouchListener { _, _ ->
        //    Log.d(LOG_TAG, "workIntervalSpinner.setOnTouchListener")
        //    //binding.workIntervalBorder.background = null
        //    //binding.setScreensBorder.setBackgroundResource(R.drawable.item_border_highlighted)
        //    false
        //}

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
                    preferences.edit { putString("work_interval", selectedValue) }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    Log.w(LOG_TAG, "workIntervalSpinner: No Item Selected")
                }
            }

        //binding.setScreensSpinner.setOnTouchListener { _, _ ->
        //    Log.d(LOG_TAG, "setScreensSpinner.setOnTouchListener")
        //    //binding.setScreensBorder.background = null
        //    //binding.initialProvider.setBackgroundResource(R.drawable.item_border_highlighted)
        //    false
        //}

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
                    preferences.edit { putString("set_screens", selectedValue) }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    Log.w(LOG_TAG, "setScreensSpinner: No Item Selected")
                }
            }

        // Initial Remote Spinner
        Log.d(LOG_TAG, "Setting up Initial Remote Spinner")
        val urls = RemoteDatabase.defaultData.map { it.url }
        val spinnerAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, urls)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.initialRemoteSpinner.adapter = spinnerAdapter
        Log.d(LOG_TAG, "Initial Remote Spinner Items: $urls")
        binding.initialRemoteSpinner.setSelection(0)
        Log.d(LOG_TAG, "Initial Remote Spinner Selection: ${binding.initialRemoteSpinner.selectedItem}")

        val startAppListener: (View) -> Unit = { view ->
            Log.d(LOG_TAG, "startAppListener: view: $view")

            binding.btnStart.isEnabled = false
            binding.btnDownload.isEnabled = false

            val workInterval = preferences.getString("work_interval", null) ?: "0"
            Log.d(LOG_TAG, "startAppListener: workInterval: $workInterval")
            if (workInterval != "0") {
                ctx.enqueueWorkRequest(workInterval)
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
            val selectedText = binding.initialRemoteSpinner.selectedItem?.toString() ?: ""
            Log.d(LOG_TAG, "startAppListener: selectedText: $selectedText")

            viewLifecycleOwner.lifecycleScope.launch {
                Log.d(LOG_TAG, "startAppListener: lifecycleScope.launch")
                withContext(Dispatchers.IO) {
                    val dao = RemoteDatabase.getInstance(ctx).remoteDao()
                    val active = dao.getActive()
                    Log.d(LOG_TAG, "startAppListener: active: $active")
                    if (selectedText.isNotEmpty() && active?.url != selectedText) {
                        Log.d(LOG_TAG, "startAppListener: activating: $selectedText")
                        val remote = dao.getByUrl(selectedText) ?: Remote(selectedText).also {
                            dao.addOrUpdate(it)
                        }
                        Log.d(LOG_TAG, "startAppListener: remote: $remote")
                        dao.activate(remote)
                    }
                }

                // Navigate Home
                val navController = findNavController()
                navController.navigate(
                    R.id.nav_action_setup_home, bundle, NavOptions.Builder()
                        .setPopUpTo(navController.graph.id, true)
                        .build()
                )
            }
        }
        binding.btnDownload.setOnClickListener(startAppListener)
        binding.btnStart.setOnClickListener(startAppListener)

        // Create Database (defaultData is seeded during onCreate)
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(LOG_TAG, "Creating Initial Data")
            val dao = RemoteDatabase.getInstance(ctx).remoteDao()
            dao.getAll()
        }

        if (viewModel.confettiShown.value != true) {
            viewModel.confettiShown.value = true
            dropConfetti()
        }
    }

    private fun dropConfetti() {
        Log.d(LOG_TAG, "dropConfetti")
        val party = Party(
            speed = 0f,
            maxSpeed = 15f,
            damping = 0.9f,
            angle = Angle.BOTTOM,
            spread = Spread.SMALL,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)),
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(40),
            timeToLive = 3000L,
        )
        binding.konfettiView.start(party)
    }

    override fun onStart() {
        super.onStart()
        Log.i(LOG_TAG, "onStart - SetupFragment - Hide UI - Lock Drawer")
        mainActivity = (activity as? MainActivity)
        mainActivity?.findViewById<Toolbar>(R.id.toolbar)?.visibility = View.GONE
        mainActivity?.findViewById<BottomNavigationView>(R.id.bottom_nav)?.visibility = View.GONE
        mainActivity?.setDrawerLockMode(false)
        mainActivity?.setStatusDecor(true)
    }

    override fun onStop() {
        Log.i(LOG_TAG, "onStop - SetupFragment - Show UI - Unlock Drawer")
        mainActivity?.findViewById<Toolbar>(R.id.toolbar)?.visibility = View.VISIBLE
        mainActivity?.findViewById<BottomNavigationView>(R.id.bottom_nav)?.visibility = View.VISIBLE
        mainActivity?.setDrawerLockMode(true)
        mainActivity?.setStatusDecor(false)
        super.onStop()
    }
}
