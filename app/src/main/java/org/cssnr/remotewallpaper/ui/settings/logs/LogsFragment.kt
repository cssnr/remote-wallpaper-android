package org.cssnr.remotewallpaper.ui.settings.logs

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.databinding.FragmentLogsBinding
import org.cssnr.remotewallpaper.log.DebugLogger
import org.cssnr.remotewallpaper.log.LogExportResult

class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LogsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("LogsFragment", "onViewCreated")

        val ctx = requireContext()

        adapter = LogsAdapter(emptyList())
        binding.logsList.layoutManager = LinearLayoutManager(ctx)
        binding.logsList.adapter = adapter

        binding.btnCopy.setOnClickListener {
            Log.d("LogsFragment", "btnCopy")
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { DebugLogger.exportAsText(ctx) }
                when (result) {
                    LogExportResult.Error -> Toast.makeText(ctx, "Failed to export logs", Toast.LENGTH_SHORT).show()
                    LogExportResult.Empty -> Toast.makeText(ctx, "No Logs to Copy", Toast.LENGTH_SHORT).show()
                    is LogExportResult.Success -> ctx.copyToClipboard(result.text)
                }
            }
        }

        binding.btnShare.setOnClickListener {
            Log.d("LogsFragment", "btnShare")
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { DebugLogger.exportAsText(ctx) }
                when (result) {
                    LogExportResult.Error -> Toast.makeText(ctx, "Failed to export logs", Toast.LENGTH_SHORT).show()
                    LogExportResult.Empty -> Toast.makeText(ctx, "No Logs to Share", Toast.LENGTH_SHORT).show()
                    is LogExportResult.Success -> ctx.shareLogs(result.text)
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            Log.d("LogsFragment", "btnDelete")
            MaterialAlertDialogBuilder(ctx, R.style.AlertDialogTheme)
                .setTitle("Delete Logs?")
                .setIcon(R.drawable.md_delete_24px)
                .setMessage("This will remove all stored log entries.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { DebugLogger.clear(ctx) }
                        Toast.makeText(ctx, "Logs Deleted", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        binding.swiperefresh.setOnRefreshListener {
            Log.d("LogsFragment", "onRefresh")
            binding.swiperefresh.isRefreshing = false
        }

        lifecycleScope.launch {
            DebugLogger.getLogs(ctx).collectLatest { logs ->
                Log.d("LogsFragment", "collectLatest: ${logs.size}")
                adapter.updateData(logs)
                binding.emptyState.visibility =
                    if (logs.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun Context.copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Logs", text))
        Toast.makeText(this, "Logs Copied to Clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun Context.shareLogs(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Remote Wallpaper Logs")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share Logs"))
    }
}
