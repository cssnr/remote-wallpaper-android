package org.cssnr.remotewallpaper.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.databinding.FragmentHistoryBinding
import org.cssnr.remotewallpaper.db.HistoryDatabase
import org.cssnr.remotewallpaper.db.HistoryItem
import org.cssnr.remotewallpaper.showSnackbar
import java.util.Locale

const val LOG_TAG = "History"

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
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
        Log.d(LOG_TAG, "RemotesFragment - onViewCreated: ${savedInstanceState?.size()}")

        val ctx = requireContext()
        //val remotesViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        fun onClick(view: View, data: HistoryItem) {
            Log.i(LOG_TAG, "onClick: $data")
            if (adapter.hasSelection) {
                adapter.toggleSelection(data.id)
                updateToolbarState()
                return
            }
            ctx.showItemContextMenu(view, data)
        }

        fun onLongClick(data: HistoryItem) {
            Log.d(LOG_TAG, "onLongClick: $data")
            adapter.toggleSelection(data.id)
            updateToolbarState()
        }

        // Initialize Adapter
        if (!::adapter.isInitialized) {
            Log.i(LOG_TAG, "INITIALIZE: HistoryAdapter")
            adapter = HistoryAdapter(::onClick, ::onLongClick)
        }
        binding.remotesList.layoutManager = LinearLayoutManager(ctx)
        if (binding.remotesList.adapter == null) {
            Log.i(LOG_TAG, "INITIALIZE: remotesList.adapter")
            binding.remotesList.adapter = adapter
        }

        binding.btnGoTop.setOnClickListener {
            Log.d(LOG_TAG, "btnGoTop")
            if (adapter.itemCount > 0) {
                binding.remotesList.scrollToPosition(0)
            }
        }

        binding.btnGoBottom.setOnClickListener {
            Log.d(LOG_TAG, "btnGoBottom")
            if (adapter.itemCount > 0) {
                binding.remotesList.scrollToPosition(adapter.itemCount - 1)
            }
        }

        binding.btnSelectAll.setOnClickListener {
            Log.d(LOG_TAG, "btnSelectAll")
            adapter.toggleSelectAll()
            updateToolbarState()
        }

        binding.btnDelete.setOnClickListener {
            Log.d(LOG_TAG, "btnDelete")
            val toDelete = adapter.selected
            if (toDelete.isEmpty()) {
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(ctx, R.style.AlertDialogTheme)
                .setTitle("Delete History Items?")
                .setIcon(R.drawable.md_delete_24px)
                .setMessage("${toDelete.size} item${if (toDelete.size == 1) "" else "s"} selected.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        val dao = HistoryDatabase.getInstance(ctx).historyDao()
                        val items = withContext(Dispatchers.IO) {
                            dao.deleteByIds(toDelete)
                            dao.getAll()
                        }
                        adapter.clearSelection()
                        adapter.updateData(items) { updateToolbarState() }
                        ctx.showSnackbar("History Deleted.")
                    }
                }
                .show()
        }

        updateToolbarState()

        //// Create the observer which updates the UI.
        //val stationObserver = Observer<List<Remote>> { data ->
        //    Log.d(LOG_TAG, "Observer - data.size: ${data.size}")
        //    //adapter.updateData(data)
        //}
        //// Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        //remotesViewModel.stationData.observe(requireActivity(), stationObserver)

        lifecycleScope.launch {
            //val dao = HistoryDatabase.getInstance(ctx).historyDao()
            //val remotes = withContext(Dispatchers.IO) { dao.getAll() }
            //Log.d(LOG_TAG, "remotes.size ${remotes.size}")
            //adapter.updateData(remotes)
            ctx.updateData()
        }

        // Setup refresh listener which triggers new data loading
        //binding.swiperefresh.isEnabled = false
        binding.swiperefresh.setOnRefreshListener {
            Log.d(LOG_TAG, "setOnRefreshListener: onRefresh")
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { ctx.updateData() }
                ctx.showSnackbar("History Reloaded")
                _binding?.swiperefresh?.isRefreshing = false
            }
        }
    }

    private fun updateToolbarState() {
        val viewBinding = _binding ?: return
        val hasSelection = adapter.hasSelection
        viewBinding.btnDelete.isEnabled = hasSelection
        viewBinding.btnDelete.imageTintList = ColorStateList.valueOf(
            MaterialColors.getColor(
                requireContext(),
                if (hasSelection) {
                    android.R.attr.colorError
                } else {
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                },
                0
            )
        )

        val selectActive = adapter.isAllSelected()
        viewBinding.btnSelectAll.imageTintList = ColorStateList.valueOf(
            MaterialColors.getColor(
                requireContext(),
                if (selectActive) {
                    androidx.appcompat.R.attr.colorPrimary
                } else {
                    com.google.android.material.R.attr.colorOnSurface
                },
                0
            )
        )
    }

    private suspend fun Context.updateData() {
        val dao = HistoryDatabase.getInstance(this).historyDao()
        val remotes = withContext(Dispatchers.IO) { dao.getAll() }
        Log.d(LOG_TAG, "remotes.size ${remotes.size}")
        withContext(Dispatchers.Main) { adapter.updateData(remotes) { updateToolbarState() } }
    }

    private fun Context.showItemContextMenu(view: View, data: HistoryItem) {
        val popup = PopupMenu(view.context, view).apply {
            menu.add("View Details").setOnMenuItemClickListener {
                Log.d(LOG_TAG, "VIEW: ${data.url}")
                showDetailsDialog(data)
                true
            }
            menu.add("Copy URL").setOnMenuItemClickListener {
                Log.d(LOG_TAG, "COPY: ${data.url}")
                copyToClipboard(data.url)
                true
            }
            //// TODO: This requires updating the updateWallpaper function to take a url...
            //menu.add("Set Wallpaper").setOnMenuItemClickListener {
            //    Log.d(LOG_TAG, "WALLPAPER: ${data.url}")
            //    CoroutineScope(Dispatchers.IO).launch { updateWallpaper(data.url) }
            //    true
            //}
            menu.add("Open in Browser").setOnMenuItemClickListener {
                Log.d(LOG_TAG, "OPEN: ${data.url}")
                openLink(data.url)
                true
            }
        }
        popup.show()
    }

    fun Context.showDetailsDialog(data: HistoryItem) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_history, null)
        val itemUrl = view.findViewById<TextView>(R.id.item_url)
        val itemStatus = view.findViewById<TextView>(R.id.item_status)
        val itemError = view.findViewById<TextView>(R.id.item_error)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Close", null)
            .create()

        dialog.setOnShowListener {
            //itemUrl.text = Html.fromHtml(data.url, Html.FROM_HTML_MODE_LEGACY)
            //itemUrl.movementMethod = LinkMovementMethod.getInstance()
            itemUrl.text = data.url ?: "No URL"

            itemStatus.text = String.format(Locale.getDefault(), "%d", data.status)

            if (!data.error.isNullOrBlank()) {
                itemError.text = data.error
            }
        }
        dialog.show()
    }

    fun Context.copyToClipboard(text: String?, msg: String? = null) {
        if (!text.isNullOrEmpty()) {
            val clipboard = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Text", text)
            clipboard.setPrimaryClip(clip)
            showSnackbar(msg ?: "Copied to Clipboard")
        }
    }

    fun Context.openLink(url: String?) {
        Log.d(LOG_TAG, "openLink: $url")
        if (!url.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            Log.i(LOG_TAG, "openLink: intent: $intent")
            startActivity(intent)
        }
    }
}
