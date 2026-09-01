package org.cssnr.remotewallpaper.ui.settings

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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.databinding.FragmentDebugBinding
import org.cssnr.remotewallpaper.log.DebugFileLogger

class DebugFragment : Fragment() {

    companion object {
        const val LOG_TAG = "DebugFragment"
    }

    private var _binding: FragmentDebugBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDebugBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "savedInstanceState: ${savedInstanceState?.size()}")

        val appCtx = requireContext().applicationContext

        lifecycleScope.launch {
            if (!isAdded || _binding == null) return@launch
            binding.textView.text = appCtx.readLogFile()
        }

        binding.copyLogs.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            Log.d(LOG_TAG, "copyLogs")
            val text = binding.textView.text.toString().trim()
            if (text.isNotEmpty()) appCtx.copyToClipboard(text, "Logs Copied")
        }

        binding.shareLogs.setOnClickListener {
            Log.d(LOG_TAG, "shareLogs")
            val text = binding.textView.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, binding.textView.text)
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }

        binding.reloadLogs.setOnClickListener {
            Log.d(LOG_TAG, "reloadLogs")
            lifecycleScope.launch {
                if (!isAdded || _binding == null) return@launch
                binding.textView.text = appCtx.readLogFile()
                Toast.makeText(appCtx, "Logs Reloaded.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.clearLogs.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            Log.d(LOG_TAG, "clearLogs")
            val text = binding.textView.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            val activityCtx = requireContext()
            MaterialAlertDialogBuilder(activityCtx, R.style.AlertDialogTheme)
                .setIcon(R.drawable.md_delete_24px)
                .setTitle("Confirm")
                .setMessage("Delete All Logs?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    if (_binding == null) return@setPositiveButton
                    lifecycleScope.launch {
                        val cleared = DebugFileLogger.clear(appCtx)
                        if (!isAdded || _binding == null) return@launch
                        if (cleared) {
                            binding.textView.text = ""
                            Toast.makeText(appCtx, "Logs Cleared.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(appCtx, "Unable to clear logs.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }

        binding.swiperefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh() {
                Log.d(LOG_TAG, "setOnRefreshListener: onRefresh")
                lifecycleScope.launch {
                    if (!isAdded || _binding == null) return@launch
                    binding.textView.text = appCtx.readLogFile()
                    Toast.makeText(appCtx, "Logs Reloaded.", Toast.LENGTH_SHORT).show()
                    binding.swiperefresh.isRefreshing = false
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "DebugFragment - onResume")
        lifecycleScope.launch {
            if (!isAdded || _binding == null) return@launch
            val ctx = context ?: return@launch
            binding.textView.text = ctx.readLogFile()
        }
    }

    suspend fun Context.readLogFile(): String = DebugFileLogger.read(this)

    fun Context.copyToClipboard(text: String, msg: String? = null) {
        val clipboard = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, msg ?: "Copied to Clipboard", Toast.LENGTH_SHORT).show()
    }
}