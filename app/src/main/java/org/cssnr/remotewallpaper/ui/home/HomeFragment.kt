package org.cssnr.remotewallpaper.ui.home

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.databinding.FragmentHomeBinding
import org.cssnr.remotewallpaper.db.HistoryDatabase
import org.cssnr.remotewallpaper.db.HistoryItem
import org.cssnr.remotewallpaper.db.RemoteDatabase
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var latest: HistoryItem? = null

    companion object {
        const val LOG_TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "onViewCreated: savedInstanceState: ${savedInstanceState?.size()}")

        val updateWallpaper = arguments?.getBoolean("update_wallpaper") == true
        Log.i(LOG_TAG, "updateWallpaper: $updateWallpaper")

        val ctx = requireContext()

        lifecycleScope.launch {
            ctx.updateData()
            if (updateWallpaper) {
                Log.i(LOG_TAG, "Loading Wallpaper")
                arguments?.remove("update_wallpaper")
                ctx.reloadWallpaper()
            }
        }

        //// TODO: Copied to onResume - Make an update function...
        //lifecycleScope.launch {
        //    val dao = HistoryDatabase.getInstance(ctx).historyDao()
        //    latest = withContext(Dispatchers.IO) { dao.getLast() }
        //    Log.d(LOG_TAG, "latest ${latest?.url}")
        //    binding.textView.text = latest?.url ?: "URL Not Found!"
        //}
        //
        //val imageFile = File(ctx.filesDir, "wallpaper.img")
        //if (imageFile.exists()) {
        //    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        //    binding.imageView.setImageBitmap(bitmap)
        //}

        binding.openBtn.setOnClickListener {
            Log.d(LOG_TAG, "setOnClickListener")
            if (latest?.url != null) {
                val uri = latest?.url?.toUri()
                Log.d(LOG_TAG, "uri: $uri")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } else {
                Toast.makeText(ctx, "No Image URL!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loadSingleBgn.setOnClickListener {
            Log.d(LOG_TAG, "setOnClickListener")
            ctx.showAddDialog()
        }

        binding.reloadBtn.setOnClickListener {
            Log.d(LOG_TAG, "setOnClickListener")
            lifecycleScope.launch { ctx.reloadWallpaper() }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "onResume")
        lifecycleScope.launch { requireContext().updateData() }
        // TODO: Copied from onCreate - Make an update function...
        //lifecycleScope.launch {
        //    val dao = HistoryDatabase.getInstance(requireContext()).historyDao()
        //    latest = withContext(Dispatchers.IO) { dao.getLast() }
        //    Log.d(LOG_TAG, "latest ${latest?.url}")
        //    binding.textView.text = latest?.url ?: "URL Not Found!"
        //}
        //
        //val imageFile = File(ctx.filesDir, "wallpaper.img")
        //if (imageFile.exists()) {
        //    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        //    binding.imageView.setImageBitmap(bitmap)
        //}
    }

    suspend fun Context.reloadWallpaper() {
        binding.loadingOverlay.visibility = View.VISIBLE
        if (updateWallpaper()) {
            updateData()
            Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No Remotes.", Toast.LENGTH_SHORT).show()
        }
        binding.loadingOverlay.visibility = View.GONE
    }

    suspend fun Context.updateData() {
        val dao = HistoryDatabase.getInstance(this).historyDao()
        latest = withContext(Dispatchers.IO) { dao.getLastSuccess() }
        Log.d(LOG_TAG, "latest ${latest?.url}")
        binding.textView.text = latest?.url ?: "Image URL Not Found!"

        val imageFile = File(filesDir, "wallpaper.img")
        if (imageFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            binding.imageView.setImageBitmap(bitmap)
        }
    }
}

// TODO: This is shared with RemotesFragment but will most likely not be used here in the end
fun Context.showAddDialog() {
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
            if (url.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        downloadImage(url)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@showAddDialog, "Done.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@showAddDialog, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                sendButton.isEnabled = true
                input.error = "URL is Required"
            }
        }
    }

    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Set Image") { _, _ -> }
    dialog.show()
}

// TODO: updateWallpaper is used globally to update the wallpaper and should be a package
//  The rest of the functions are only used by updateWallpaper and are internal to updateWallpaper
suspend fun Context.updateWallpaper(): Boolean {
    // TODO: This version is testing historyDao vs above version. It will all be refactored...
    val historyDao = HistoryDatabase.getInstance(this).historyDao()
    val history = HistoryItem()
    try {
        val dao = RemoteDatabase.getInstance(this).remoteDao()
        val remote = withContext(Dispatchers.IO) { dao.getActive() }
        Log.d("updateWallpaper", "remote: $remote")
        if (remote != null) {
            history.remote = remote.url
            val response = withContext(Dispatchers.IO) { downloadImage(remote.url) }
            history.status = response.code
            history.url = response.request.url.toString()
            Log.d("updateWallpaper", "response: $response")
            // TODO: Replace timestamp with history.timestamp
            val timestamp: String =
                ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            Log.d("updateWallpaper", "timestamp: $timestamp")
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            preferences.edit {
                putString("last_update", timestamp)
            }
            Log.d("updateWallpaper", "history: $history")
            withContext(Dispatchers.IO) { historyDao.add(history) }
            return true
        }
        Log.d("updateWallpaper", "history: $history")
        withContext(Dispatchers.IO) { historyDao.add(history) }
        return false
    } catch (e: Exception) {
        Log.e("updateWallpaper", "updateWallpaper: Exception: $e")
        history.error = e.message
        Log.d("updateWallpaper", "history: $history")
        withContext(Dispatchers.IO) { historyDao.add(history) }
        return false
    }
}

fun Context.downloadImage(url: String): Response {
    val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    val request = Request.Builder()
        .url(url)
        .build()

    val response = client.newCall(request).execute()

    response.use {
        if (!it.isSuccessful) throw Exception("Failed to download image: $it")
        val body = it.body ?: throw Exception("Empty response body")
        val imageFile = File(filesDir, "wallpaper.img")

        body.byteStream().use { input ->
            FileOutputStream(imageFile).use { output ->
                input.copyTo(output)
            }
        }

        setAutoCroppedWallpaper(imageFile)
    }
    return response
}

fun Context.setAutoCroppedWallpaper(imageFile: File) {
    val wallpaperManager = WallpaperManager.getInstance(this)
    val targetWidth = wallpaperManager.desiredMinimumWidth
    val targetHeight = wallpaperManager.desiredMinimumHeight

    val original = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return
    val scaled = scaleAndCropCenter(original, targetWidth, targetHeight)

    wallpaperManager.setBitmap(scaled)

    original.recycle()
    scaled.recycle()
}

fun Context.scaleAndCropCenter(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    // targetWidth: WallpaperManager.desiredMinimumWidth
    // targetHeight: WallpaperManager.desiredMinimumHeight
    Log.d("Cropper", "target W=$targetWidth H=$targetHeight")

    val srcWidth = src.width
    val srcHeight = src.height
    Log.d("Cropper", "src W=$srcWidth H=$srcHeight")

    val scale = maxOf(
        targetWidth.toFloat() / srcWidth,
        targetHeight.toFloat() / srcHeight
    )
    Log.d("Cropper", "scale: $scale")

    val scaledWidth = (srcWidth * scale).toInt()
    val scaledHeight = (srcHeight * scale).toInt()
    Log.d("Cropper", "scaled W=$scaledWidth  H=$scaledHeight")

    val scaledBitmap = src.scale(scaledWidth, scaledHeight)

    val x = (scaledWidth - targetWidth) / 2
    val y = (scaledHeight - targetHeight) / 2
    Log.d("Cropper", "x=$x  y=$y")

    return Bitmap.createBitmap(scaledBitmap, x, y, targetWidth, targetHeight)
}


//val historyDao = HistoryDatabase.getInstance(this).historyDao()
//val history = HistoryItem(remote = remote.url)
//historyDao.add(history)


//val displayMetrics = resources.displayMetrics
//val dpWidth = displayMetrics.widthPixels / displayMetrics.density
//val dpHeight = displayMetrics.heightPixels / displayMetrics.density
//Log.d("Cropper", "dp W=$dpWidth H=$dpHeight")


//val contentType =
//    response.header("Content-Type") ?: throw Exception("No Content-Type header")
//val extension = when {
//    contentType.contains("jpeg") -> "jpg"
//    contentType.contains("png") -> "png"
//    contentType.contains("gif") -> "gif"
//    contentType.contains("webp") -> "webp"
//    contentType.contains("bmp") -> "bmp"
//    else -> "img"
//}

//Log.d(LOG_TAG, "response.request.url: ${response.request.url}")
//Log.d(LOG_TAG, "url.encodedPath: ${response.request.url.encodedPath}")
//Log.d(LOG_TAG, "url.pathSegments: ${response.request.url.pathSegments}")
//val fullFileName = response.request.url.pathSegments.lastOrNull() ?: ""
//Log.d(LOG_TAG, "fullFileName: $fullFileName")
//val fileName = fullFileName.substringBeforeLast('.', missingDelimiterValue = "wallpaper")
//Log.d(LOG_TAG, "fileName: $fileName")
//val fileExt = fullFileName.substringAfterLast('.', missingDelimiterValue = extension)
//Log.d(LOG_TAG, "fileExt: $fileExt")
//val imageFile = File(filesDir, "${fileName}.${fileExt}")
//Log.d(LOG_TAG, "imageFile: .$imageFile")


//val dao = RemoteDatabase.getInstance(ctx).remoteDao()
//val remote = withContext(Dispatchers.IO) { dao.getActive() }
//Log.d(LOG_TAG, "remote: ${remote?.url}")
//if (remote != null) {
//    binding.loadingOverlay.visibility = View.VISIBLE
//    withContext(Dispatchers.IO) { ctx.downloadImage(remote.url) }
//    binding.loadingOverlay.visibility = View.GONE
//    Toast.makeText(ctx, "Done.", Toast.LENGTH_SHORT).show()
//} else {
//    Toast.makeText(ctx, "No Remotes.", Toast.LENGTH_SHORT).show()
//}
