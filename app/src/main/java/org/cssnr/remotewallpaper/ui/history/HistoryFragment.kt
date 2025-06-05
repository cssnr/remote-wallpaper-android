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
            showItemContextMenu(view, data.url)
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
                //lifecycleScope.launch {
                //    val dao = HistoryDatabase.getInstance(ctx).historyDao()
                //    Log.i(LOG_TAG, "DELETING: ${data.url}")
                //    val remotes = withContext(Dispatchers.IO) {
                //        dao.delete(station)
                //        if (station.active) {
                //            Log.d(LOG_TAG, "activateFirstStation")
                //            dao.activateFirstStation()
                //        }
                //        dao.getAll()
                //    }
                //    adapter.updateData(remotes)
                //    //remotesViewModel.stationData.value = remotes
                //}
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
            val dao = HistoryDatabase.getInstance(ctx).historyDao()
            val remotes = withContext(Dispatchers.IO) { dao.getAll() }
            Log.d(LOG_TAG, "remotes.size ${remotes.size}")
            adapter.updateData(remotes)
            //remotesViewModel.stationData.value = remotes
        }

        // Setup refresh listener which triggers new data loading
        binding.swiperefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh() {
                Log.d(LOG_TAG, "setOnRefreshListener: onRefresh")
                lifecycleScope.launch {
                    Toast.makeText(ctx, "Not Yet Implemented!", Toast.LENGTH_SHORT).show()
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

    fun showItemContextMenu(view: View, url: String?) {
        val popup = PopupMenu(view.context, view).apply {
            menu.add("Open").setOnMenuItemClickListener {
                Log.d(LOG_TAG, "OPEN: $url")
                requireContext().openLink(url)
                true
            }
            menu.add("Copy").setOnMenuItemClickListener {
                Log.d(LOG_TAG, "COPY: $url")
                requireContext().copyToClipboard(url)
                true
            }
        }
        popup.show()
    }
}

private fun Context.deleteConfirmDialog(
    item: HistoryItem,
    callback: (item: HistoryItem) -> Unit,
) {
    Log.d("deleteConfirmDialog", "item: $item")
    MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
        .setTitle("Not Yet Implemented")
        .setIcon(R.drawable.md_delete_24px)
        .setMessage(item.url)
        .setNegativeButton("Cancel", null)
        .setPositiveButton("INOP") { _, _ -> callback(item) }
        .show()
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
