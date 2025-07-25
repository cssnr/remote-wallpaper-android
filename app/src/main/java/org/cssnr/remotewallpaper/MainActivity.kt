package org.cssnr.remotewallpaper

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.cssnr.remotewallpaper.databinding.ActivityMainBinding
import org.cssnr.remotewallpaper.widget.WidgetProvider
import org.cssnr.remotewallpaper.work.APP_WORKER_CONSTRAINTS
import org.cssnr.remotewallpaper.work.AppWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        const val LOG_TAG = "RemoteWallpaper"
    }

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "onCreate: savedInstanceState: ${savedInstanceState?.size()}")
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NavHostFragment
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        //// Start Destination
        //if (savedInstanceState == null) {
        //    val navGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
        //    //val startPreference = preferences.getString("start_destination", null)
        //    //Log.d("Main[onCreate]", "startPreference: $startPreference")
        //    val startDestination = R.id.nav_home
        //    navGraph.setStartDestination(startDestination)
        //    navController.graph = navGraph
        //}

        // Bottom Navigation
        val bottomNav = binding.contentMain.appBarMain.bottomNav
        bottomNav.setupWithNavController(navController)

        // Navigation Drawer
        binding.navView.setupWithNavController(navController)

        // App Bar Configuration
        setSupportActionBar(binding.contentMain.appBarMain.toolbar)
        val topLevelItems =
            setOf(R.id.nav_home, R.id.nav_history, R.id.nav_remotes, R.id.nav_settings)
        appBarConfiguration = AppBarConfiguration(topLevelItems, binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Destinations w/ a Parent Item
        val destinationToBottomNavItem = mapOf(
            R.id.nav_item_widget_settings to R.id.nav_settings,
        )
        // Destination w/ No Parent
        val hiddenDestinations = setOf(
            R.id.nav_item_setup,
        )
        // Implement Navigation Hacks Because.......Android?
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("addOnDestinationChangedListener", "destination: ${destination.label}")
            binding.drawerLayout.closeDrawer(GravityCompat.START)

            val destinationId = destination.id

            if (destinationId in hiddenDestinations) {
                Log.d("addOnDestinationChangedListener", "Set bottomNav to Hidden Item")
                bottomNav.menu.findItem(R.id.nav_hidden).isChecked = true
                return@addOnDestinationChangedListener
            }

            val matchedItem = destinationToBottomNavItem[destinationId]
            if (matchedItem != null) {
                Log.d("addOnDestinationChangedListener", "matched nav item: $matchedItem")
                bottomNav.menu.findItem(matchedItem).isChecked = true
                val menu = binding.navView.menu
                for (i in 0 until menu.size) {
                    val item = menu[i]
                    item.isChecked = item.itemId == matchedItem
                }
            }
        }

        // Handle Custom Navigation Items
        val navLinks = mapOf(
            R.id.nav_item_github to getString(R.string.github_url),
            R.id.nav_itewm_website to getString(R.string.website_url),
        )
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawers()
            val path = navLinks[menuItem.itemId]
            if (path != null) {
                Log.d("Drawer", "path: $path")
                val intent = Intent(Intent.ACTION_VIEW, path.toUri())
                startActivity(intent)
                true
            } else {
                val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                Log.d("Drawer", "handled: $handled")
                handled
            }
        }

        // Set Default Preferences
        Log.d(LOG_TAG, "Set Default Preferences")
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_widget, false)

        // Update Status Bar
        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Update Navigation Bar
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false)
        }

        // Set Global Left/Right System Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentMain.contentMainLayout) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.i("Main[ViewCompat]", "bars: $bars")
            v.updatePadding(left = bars.left, right = bars.right)
            insets
        }

        // Set Nav Header Top Padding
        val headerView = binding.navView.getHeaderView(0)
        ViewCompat.setOnApplyWindowInsetsListener(headerView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (bars.top > 0) {
                Log.d("ViewCompat", "top: ${bars.top}")
                v.updatePadding(top = bars.top)
            }
            insets
        }
        ViewCompat.requestApplyInsets(headerView)

        // Update Header Text
        val packageInfo = packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")
        val versionTextView = headerView.findViewById<TextView>(R.id.header_version)
        val formattedVersion = getString(R.string.version_string, versionName)
        Log.d(LOG_TAG, "formattedVersion: $formattedVersion")
        versionTextView.text = formattedVersion

        // Initialize Work Manager
        val workInterval = preferences.getString("work_interval", null) ?: "0"
        Log.d(LOG_TAG, "workInterval: $workInterval")
        if (workInterval != "0") {
            val workRequest =
                PeriodicWorkRequestBuilder<AppWorker>(workInterval.toLong(), TimeUnit.MINUTES)
                    .setConstraints(APP_WORKER_CONSTRAINTS)
                    .build()
            Log.d(LOG_TAG, "workRequest: $workRequest")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "app_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } else {
            Log.d(LOG_TAG, "Ensuring Work is Disabled")
            WorkManager.getInstance(this).cancelUniqueWork("app_worker")
        }

        // Only Handel Intent Once Here after App Start
        if (savedInstanceState?.getBoolean("intentHandled") != true) {
            onNewIntent(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("intentHandled", true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_TAG, "onOptionsItemSelected: $item")
        return when (item.itemId) {
            R.id.option_add_remote -> {
                Log.i(LOG_TAG, "ADD REMOTE OPTIONS CLICK")
                // NOTE: This seems to work to navigation to a top-level desitnation with args...
                val bundle = bundleOf("add_remote" to true)
                val menuItem = binding.navView.menu.findItem(R.id.nav_remotes)
                NavigationUI.onNavDestinationSelected(menuItem, navController)
                navController.navigate(
                    R.id.nav_remotes, bundle, NavOptions.Builder()
                        .setPopUpTo(R.id.nav_remotes, true)
                        .build()
                )
                true
            }

            R.id.option_github -> {
                Log.d(LOG_TAG, "onOptionsItemSelected: option_github")
                val intent = Intent(Intent.ACTION_VIEW, getString(R.string.github_url).toUri())
                Log.d(LOG_TAG, "onOptionsItemSelected: intent: $intent")
                startActivity(intent)
                true
            }

            else -> {
                // TODO: Title is null on Menu and not destinations, so this avoids warnings...
                if (item.title != null) {
                    NavigationUI.onNavDestinationSelected(item, navController) ||
                            super.onOptionsItemSelected(item)
                } else {
                    super.onOptionsItemSelected(item)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.action
        val data = intent.data
        Log.d(LOG_TAG, "${action}: $data")

        if (action == Intent.ACTION_MAIN) {
            Log.d("onNewIntent", "ACTION_MAIN")

            binding.drawerLayout.closeDrawers()

            if (!preferences.contains("first_run_shown")) {
                Log.i(LOG_TAG, "FIRST RUN DETECTED")
                preferences.edit { putBoolean("first_run_shown", true) }
                navController.navigate(
                    R.id.nav_item_setup, null, NavOptions.Builder()
                        .setPopUpTo(navController.graph.id, true)
                        .build()
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(LOG_TAG, "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.options, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.d(LOG_TAG, "onSupportNavigateUp")
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStop() {
        Log.d(LOG_TAG, "onStop - MainActivity")
        // Update Widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, WidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        WidgetProvider().onUpdate(this, appWidgetManager, ids)
        super.onStop()
    }

    fun setDrawerLockMode(enabled: Boolean) {
        Log.d("setDrawerLockMode", "enabled: $enabled")
        val lockMode =
            if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        Log.d("setDrawerLockMode", "lockMode: $lockMode")
        binding.drawerLayout.setDrawerLockMode(lockMode)
    }

    fun setStatusDecor(system: Boolean = false) {
        Log.d("setStatusDecor", "system: $system")
        val mode = if (system) {
            val configMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            Log.d("setStatusDecor", "configMode: $configMode")
            configMode == Configuration.UI_MODE_NIGHT_NO
        } else {
            false
        }
        Log.d("setStatusDecor", "mode: $mode")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = mode
    }
}
