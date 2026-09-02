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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

object DebugFileLogger {

    private const val TAG = "FileLogger"
    private const val LOG_FILE_NAME = "debug_log.txt"
    private const val MAX_LOG_SIZE_BYTES = 4L * 1024 * 1024
    private const val MAX_LOG_LINES = 2000
    private const val TRUNCATE_TARGET_BYTES = (MAX_LOG_SIZE_BYTES * 0.8).toLong()

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
                    file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                        lines.toList().asReversed().joinToString("\n")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception reading logs", e)
                "Exception reading logs: ${e.message}"
            }
        }
    }

    private data class TailLine(val line: String, val size: Int)

    private fun truncateIfNeeded(logFile: File) {
        if (!logFile.exists() || logFile.length() <= MAX_LOG_SIZE_BYTES) return
        val lines = ArrayDeque<TailLine>(MAX_LOG_LINES)
        var total = 0L
        logFile.bufferedReader(Charsets.UTF_8).useLines { sequence ->
            sequence.forEach { line ->
                val entry = TailLine(line, line.encodeToByteArray().size + 1)
                lines.addLast(entry)
                total += entry.size
                while ((total > TRUNCATE_TARGET_BYTES || lines.size > MAX_LOG_LINES) && lines.size > 1) {
                    total -= lines.removeFirst().size
                }
            }
        }

        val trimmed = if (lines.isEmpty()) "" else lines.joinToString("\n") { it.line } + "\n"
        OutputStreamWriter(
            FileOutputStream(logFile, false), Charsets.UTF_8
        ).buffered().use { it.write(trimmed) }
        Log.i(TAG, "Log truncated to ${lines.size} lines")
    }
}

suspend fun Context.debugLog(message: String) {
    DebugFileLogger.initialize(this)
    DebugFileLogger.log(this, message)
}
