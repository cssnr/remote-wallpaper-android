package org.cssnr.remotewallpaper.log

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

object DebugFileLogger {

    private const val TAG = "FileLogger"
    private const val LOG_FILE_NAME = "debug_log.txt"
    private const val MAX_LOG_SIZE_BYTES = 4L * 1024 * 1024
    private const val MAX_LOG_LINES = 1000
    private const val TAIL_SLICE_BYTES = 512L * 1024

    private val logMutex = Mutex()
    private val clearGeneration = AtomicLong(0)
    @Volatile
    private var debugEnabled = false
    @Volatile
    private var initialized = false

    private val timestampFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss", Locale.US)

    fun initialize(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                    debugEnabled = preferences.getBoolean("enable_debug_logs", false)
                    initialized = true
                }
            }
        }
    }

    fun setDebugEnabled(enabled: Boolean) {
        debugEnabled = enabled
    }

    fun logFile(context: Context): File = File(context.filesDir, LOG_FILE_NAME)

    suspend fun log(context: Context, message: String) {
        if (!debugEnabled) return
        val logFile = logFile(context)
        val timeStamp = LocalDateTime.now().format(timestampFormatter)
        val logMessage = "$timeStamp - $message\n"
        val generation = clearGeneration.get()

        withContext(Dispatchers.IO) {
            logMutex.withLock {
                if (generation != clearGeneration.get()) return@withLock
                try {
                    OutputStreamWriter(
                        FileOutputStream(logFile, true), Charsets.UTF_8
                    ).buffered().use { it.write(logMessage) }
                    truncateIfNeeded(logFile)
                } catch (e: IOException) {
                    Log.e(TAG, "IOException writing log: $e")
                }
            }
        }
    }

    suspend fun clear(context: Context): Boolean = withContext(Dispatchers.IO) {
        logMutex.withLock {
            clearGeneration.incrementAndGet()
            try {
                val logFile = logFile(context)
                if (logFile.exists()) {
                    FileOutputStream(logFile, false).use { }
                }
                true
            } catch (e: IOException) {
                Log.e(TAG, "IOException clearing log: $e")
                false
            }
        }
    }

    suspend fun read(context: Context): String = withContext(Dispatchers.IO) {
        logMutex.withLock {
            try {
                val file = logFile(context)
                if (!file.exists()) {
                    ""
                } else {
                    readTailLines(file).asReversed().joinToString("\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception reading logs", e)
                "Exception reading logs: ${e.message}"
            }
        }
    }

    private fun truncateIfNeeded(logFile: File) {
        if (!logFile.exists() || logFile.length() <= MAX_LOG_SIZE_BYTES) return
        val lines = readTailLines(logFile)
        val trimmed = if (lines.isEmpty()) "" else lines.joinToString("\n") + "\n"
        OutputStreamWriter(
            FileOutputStream(logFile, false), Charsets.UTF_8
        ).buffered().use { it.write(trimmed) }
        Log.i(TAG, "Log truncated to ${lines.size} lines")
    }

    private fun readTailLines(file: File): List<String> {
        val fileLength = file.length()
        if (fileLength == 0L) return emptyList()

        val startOffset = maxOf(0L, fileLength - TAIL_SLICE_BYTES)
        val readSize = (fileLength - startOffset).toInt()
        val bytes = RandomAccessFile(file, "r").use { raf ->
            raf.seek(startOffset)
            ByteArray(readSize).also { raf.readFully(it) }
        }
        val text = String(bytes, Charsets.UTF_8)
        val sequence = text.lineSequence()
        val lines = (if (startOffset > 0L) sequence.drop(1) else sequence)
            .filter { it.isNotEmpty() }
            .toList()
        return lines.takeLast(MAX_LOG_LINES)
    }
}

suspend fun Context.debugLog(message: String) {
    DebugFileLogger.initialize(this)
    DebugFileLogger.log(this, message)
}
