package org.cssnr.remotewallpaper.ui.remotes

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.databinding.FragmentRemotesBinding
import org.cssnr.remotewallpaper.db.Remote
import org.cssnr.remotewallpaper.db.RemoteDatabase
import java.net.URL

//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProvider

const val LOG_TAG = "Remotes"

class RemotesFragment : Fragment() {

    private var _binding: FragmentRemotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RemotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemotesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "onDestroyView")
        _binding = null
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "RemotesFragment - onViewCreated: ${savedInstanceState?.size()}")

        val ctx = requireContext()
        //val remotesViewModel = ViewModelProvider(this)[RemotesViewModel::class.java]

        fun onClick(data: Remote) {
            Log.i(LOG_TAG, "onClick: $data")
            lifecycleScope.launch {
                if (!data.active) {
                    val dao = RemoteDatabase.getInstance(ctx).remoteDao()
                    Log.d(LOG_TAG, "Activating: ${data.url}")
                    val remotes = withContext(Dispatchers.IO) {
                        dao.activate(data)
                        dao.getAll()
                    }
                    Log.d(LOG_TAG, "remotes: $remotes")
                    adapter.updateData(remotes)
                }

                //val api = WeatherApi(ctx)
                //val response = api.getLatest(data.stationId)
                //Log.d(LOG_TAG, "response.isSuccessful: ${response.isSuccessful}")
                //val latest = response.body()
                //Log.d(LOG_TAG, "latest: $latest")
            }
        }

        fun onLongClick(data: Remote) {
            Log.d(LOG_TAG, "onLongClick: $data")
            fun callback(station: Remote) {
                Log.d(LOG_TAG, "callback: ${data.url}")

                lifecycleScope.launch {
                    val dao = RemoteDatabase.getInstance(ctx).remoteDao()
                    Log.i(LOG_TAG, "DELETING: ${data.url}")
                    val remotes = withContext(Dispatchers.IO) {
                        dao.delete(station)
                        if (station.active) {
                            Log.d(LOG_TAG, "activateFirst")
                            dao.activateFirst()
                        }
                        dao.getAll()
                    }
                    adapter.updateData(remotes)
                    //remotesViewModel.stationData.value = remotes
                }
            }
            ctx.deleteConfirmDialog(data, ::callback)
        }

        // Initialize Adapter
        if (!::adapter.isInitialized) {
            Log.i(LOG_TAG, "INITIALIZE: RemotesAdapter")
            adapter = RemotesAdapter(emptyList(), ::onClick, ::onLongClick)
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
            val dao = RemoteDatabase.getInstance(ctx).remoteDao()
            val remotes = withContext(Dispatchers.IO) { dao.getAll() }
            Log.d(LOG_TAG, "remotes.size ${remotes.size}")
            adapter.updateData(remotes)
            //remotesViewModel.stationData.value = remotes
        }

        //setFragmentResultListener("remotes_updated") { _, bundle ->
        //    val stationId = bundle.getString("stationId")
        //    Log.d("setFragmentResultListener", "stationId: $stationId")
        //    if (stationId != null) {
        //        Log.i("setFragmentResultListener", "Added stationId: $stationId")
        //        lifecycleScope.launch {
        //            val dao = RemoteDatabase.getInstance(ctx).remoteDao()
        //            val remotes = withContext(Dispatchers.IO) { dao.getAll() }
        //            Log.d(LOG_TAG, "remotes.size: ${remotes.size}")
        //            //remotesViewModel.stationData.value = remotes
        //            withContext(Dispatchers.Main) { adapter.updateData(remotes) }
        //        }
        //    }
        //}

        binding.addStation.setOnClickListener { view ->
            Log.d(LOG_TAG, "binding.appBarMain.fab.setOnClickListener")
            ////Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            ////    .setAction("Action", null)
            ////    .setAnchorView(R.id.fab).show()
            //val newFragment = AddDialogFragment()
            //newFragment.show(parentFragmentManager, "AddDialogFragment")
            ctx.showAddDialog(adapter)
        }
    }


    private fun Context.showAddDialog(adapter: RemotesAdapter) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_add_url, null)
        val input = view.findViewById<EditText>(R.id.image_url)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()

        dialog.setOnShowListener {
            input.requestFocus()
            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            sendButton.setOnClickListener {
                sendButton.isEnabled = false
                val url = input.text.toString().trim()
                Log.d("showAddDialog", "url: $url")
                if (url.isEmpty()) {
                    sendButton.isEnabled = true
                    input.error = "URL is Required"
                } else if (!isURL(url)) {
                    sendButton.isEnabled = true
                    input.error = "Invalid URL"
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = RemoteDatabase.getInstance(this@showAddDialog).remoteDao()
                        // TODO: Make a @Transaction to handle this...
                        dao.addOrUpdate(Remote(url = url))
                        val active = dao.getActive()
                        if (active == null) {
                            val remote = dao.getByUrl(url)
                            Log.i("showAddDialog", "dao.activate: $remote")
                            dao.activate(remote!!)
                        }
                        val remotes = dao.getAll()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@showAddDialog, "URL Added.", Toast.LENGTH_SHORT)
                                .show()
                            adapter.updateData(remotes)
                            dialog.dismiss()
                        }
                    }
                }
            }
        }

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Add") { _, _ -> }
        dialog.show()
    }

    private fun Context.deleteConfirmDialog(
        remote: Remote,
        callback: (station: Remote) -> Unit,
    ) {
        Log.d("deleteConfirmDialog", "remote: $remote")
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Delete Remote?")
            .setIcon(R.drawable.md_delete_24px)
            .setMessage(remote.url)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> callback(remote) }
            .show()
    }

    fun isURL(url: String): Boolean {
        return try {
            URL(url)
            Log.d("isURL", "TRUE")
            true
        } catch (_: Exception) {
            Log.d("isURL", "FALSE")
            false
        }
    }
}
