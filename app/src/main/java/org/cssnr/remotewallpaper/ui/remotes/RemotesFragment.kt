package org.cssnr.remotewallpaper.ui.remotes

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.databinding.FragmentRemotesBinding
import org.cssnr.remotewallpaper.db.Remote
import org.cssnr.remotewallpaper.db.RemoteDatabase
import org.cssnr.remotewallpaper.showSnackbar
import org.cssnr.remotewallpaper.ui.dialogs.showKeyboard

const val LOG_TAG = "Remotes"

class RemotesFragment : Fragment() {

    private var _binding: FragmentRemotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RemotesAdapter

    private val viewModel: RemotesViewModel by viewModels()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "RemotesFragment - onViewCreated: ${savedInstanceState?.size()}")

        val ctx = requireContext()
        //val remotesViewModel = ViewModelProvider(this)[RemotesViewModel::class.java]

        fun onClick(data: Remote) {
            Log.i(LOG_TAG, "onClick: $data")
            if (adapter.hasSelection) {
                adapter.toggleSelection(data.url)
                updateToolbarState()
                return
            }
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
            }
        }

        fun onLongClick(data: Remote) {
            Log.d(LOG_TAG, "onLongClick: $data")
            adapter.toggleSelection(data.url)
            updateToolbarState()
        }

        // Initialize Adapter
        if (!::adapter.isInitialized) {
            Log.i(LOG_TAG, "INITIALIZE: RemotesAdapter")
            adapter = RemotesAdapter(viewModel.selectedUrls, ::onClick, ::onLongClick)
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
                .setTitle("Delete Remotes?")
                .setIcon(R.drawable.md_delete_24px)
                .setMessage("${toDelete.size} remote${if (toDelete.size == 1) "" else "s"} selected.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        val dao = RemoteDatabase.getInstance(ctx).remoteDao()
                        val remotes = withContext(Dispatchers.IO) {
                            val all = dao.getAll()
                            val selected = all.filter { it.url in toDelete }
                            val deletedActive = selected.any { it.active }
                            dao.deleteByUrls(toDelete)
                            if (deletedActive) {
                                Log.d(LOG_TAG, "activateFirst")
                                dao.activateFirst()
                            }
                            dao.getAll()
                        }
                        adapter.clearSelection()
                        adapter.updateData(remotes) { updateToolbarState() }
                        ctx.showSnackbar("Remotes Deleted.")
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
            val dao = RemoteDatabase.getInstance(ctx).remoteDao()
            val remotes = withContext(Dispatchers.IO) { dao.getAll() }
            Log.d(LOG_TAG, "remotes.size ${remotes.size}")
            adapter.updateData(remotes) { updateToolbarState() }
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

        binding.addStation.setOnClickListener {
            Log.d(LOG_TAG, "binding.appBarMain.fab.setOnClickListener")
            ////Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            ////    .setAction("Action", null)
            ////    .setAnchorView(R.id.fab).show()
            //val newFragment = AddDialogFragment()
            //newFragment.show(parentFragmentManager, "AddDialogFragment")
            ctx.showAddDialog(adapter, requireActivity().lifecycleScope)
        }

        if (arguments?.getBoolean("add_remote", false) == true) {
            arguments?.remove("add_remote")
            ctx.showAddDialog(adapter, requireActivity().lifecycleScope)
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

        viewBinding.btnSelectAll.setImageResource(
            if (hasSelection) R.drawable.md_playlist_remove_24px else R.drawable.md_data_check_24px
        )
    }

    private fun Context.showAddDialog(adapter: RemotesAdapter, scope: CoroutineScope) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_add_url, null)
        val input = view.findViewById<EditText>(R.id.image_url)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()

        dialog.setOnShowListener {
            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            sendButton.setOnClickListener {
                sendButton.isEnabled = false
                val url = input.text.toString().trim()
                Log.d("showAddDialog", "url: $url")
                if (url.isEmpty()) {
                    sendButton.isEnabled = true
                    input.error = "URL is Required"
                } else if (!isStringUrl(url)) {
                    sendButton.isEnabled = true
                    input.error = "Invalid URL"
                } else {
                    scope.launch {
                        try {
                            val remotes = withContext(Dispatchers.IO) {
                                val dao = RemoteDatabase.getInstance(this@showAddDialog).remoteDao()
                                // TODO: Make a @Transaction to handle this...
                                dao.addOrUpdate(Remote(url = url))
                                val active = dao.getActive()
                                if (active == null) {
                                    val remote = dao.getByUrl(url)
                                    Log.i("showAddDialog", "dao.activate: $remote")
                                    dao.activate(remote!!)
                                }
                                dao.getAll()
                            }
                            adapter.updateData(remotes) { updateToolbarState() }
                            dialog.dismiss()
                            this@showAddDialog.showSnackbar("URL Added.")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            sendButton.isEnabled = true
                            input.error = e.message ?: "Unknown Error"
                        }
                    }
                }
            }
        }

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Add") { _, _ -> }

        dialog.showKeyboard()
        input.requestFocus()
        dialog.show()
    }

    private fun isStringUrl(input: String): Boolean {
        val url = input.toHttpUrlOrNull() ?: return false
        // if (input != url.toString()) return false
        if (url.scheme !in listOf("http", "https")) return false
        if (url.host.isBlank()) return false
        if (url.toString().length > 2048) return false
        return true
    }
}
