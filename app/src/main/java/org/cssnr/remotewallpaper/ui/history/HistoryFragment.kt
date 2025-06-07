package org.cssnr.remotewallpaper.ui.history

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.remotewallpaper.MainActivity.Companion.LOG_TAG
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.databinding.FragmentHistoryBinding
import org.cssnr.remotewallpaper.db.HistoryDatabase
import org.cssnr.remotewallpaper.db.HistoryItem

//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProvider

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

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "RemotesFragment - onViewCreated: ${savedInstanceState?.size()}")

        val ctx = requireContext()
        //val remotesViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        fun onClick(view: View, data: HistoryItem) {
            Log.i(LOG_TAG, "onClick: $data")
            ctx.showItemContextMenu(view, data)
            //lifecycleScope.launch {
            //    if (!data.active) {
            //        val dao = HistoryDatabase.getInstance(ctx).historyDao()
            //        Log.d(LOG_TAG, "Activating: ${data.url}")
            //        val remotes = withContext(Dispatchers.IO) {
            //            dao.activate(data)
            //            dao.getAll()
            //        }
            //        Log.d(LOG_TAG, "remotes: $remotes")
            //        adapter.updateData(remotes)
            //    }
            //}
        }

        fun onLongClick(data: HistoryItem) {
            Log.d(LOG_TAG, "onLongClick: $data")
            fun callback(item: HistoryItem) {
                Log.d(LOG_TAG, "callback: item: $item")
                lifecycleScope.launch {
                    val dao = HistoryDatabase.getInstance(ctx).historyDao()
                    Log.i(LOG_TAG, "DELETING: ${data.url}")
                    val remotes = withContext(Dispatchers.IO) {
                        dao.delete(data)
                        dao.getAll()
                    }
                    adapter.updateData(remotes)
                }
            }
            ctx.deleteConfirmDialog(data, ::callback)
        }

        // Initialize Adapter
        if (!::adapter.isInitialized) {
            Log.i(LOG_TAG, "INITIALIZE: HistoryAdapter")
            adapter = HistoryAdapter(emptyList(), ::onClick, ::onLongClick)
        }
        binding.remotesList.layoutManager = LinearLayoutManager(ctx)
        if (binding.remotesList.adapter == null) {
            Log.i(LOG_TAG, "INITIALIZE: remotesList.adapter")
            binding.remotesList.adapter = adapter
        }

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
        binding.swiperefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh() {
                Log.d(LOG_TAG, "setOnRefreshListener: onRefresh")
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { ctx.updateData() }
                    Toast.makeText(ctx, "History Reloaded", Toast.LENGTH_SHORT).show()
                    binding.swiperefresh.isRefreshing = false
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "RemotesFragment - onDestroyView")
        _binding = null
    }

    private suspend fun Context.updateData() {
        val dao = HistoryDatabase.getInstance(this).historyDao()
        val remotes = withContext(Dispatchers.IO) { dao.getAll() }
        Log.d(LOG_TAG, "remotes.size ${remotes.size}")
        withContext(Dispatchers.Main) { adapter.updateData(remotes) }
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

    private fun Context.deleteConfirmDialog(
        item: HistoryItem,
        callback: (item: HistoryItem) -> Unit,
    ) {
        Log.d("deleteConfirmDialog", "item: $item")
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Delete Item ${item.id}?")
            .setIcon(R.drawable.md_delete_24px)
            .setMessage(item.url)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> callback(item) }
            .show()
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

            itemStatus.text = data.status.toString()

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
            Toast.makeText(this, msg ?: "Copied to Clipboard", Toast.LENGTH_SHORT).show()
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
