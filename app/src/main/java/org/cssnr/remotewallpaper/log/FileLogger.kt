package org.cssnr.remotewallpaper.log

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    private const val MAX_LOG_SIZE_BYTES = 5L * 1024 * 1024
    private const val MAX_LOG_LINES = 5000

    private val logMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clearGeneration = AtomicLong(0)
    @Volatile
    private var debugEnabled = false

    private val timestampFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss", Locale.US)

    fun initialize(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        debugEnabled = preferences.getBoolean("enable_debug_logs", false)
    }

    fun logFile(context: Context): File = File(context.filesDir, LOG_FILE_NAME)

    fun log(context: Context, message: String) {
        if (!debugEnabled) return
        val logFile = logFile(context)
        val timeStamp = LocalDateTime.now().format(timestampFormatter)
        val logMessage = "$timeStamp - $message\n"
        val generation = clearGeneration.get()

        scope.launch {
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

    private fun truncateIfNeeded(logFile: File) {
        if (!logFile.exists() || logFile.length() <= MAX_LOG_SIZE_BYTES) return
        val lines = ArrayDeque<String>(MAX_LOG_LINES)
        logFile.bufferedReader(Charsets.UTF_8).useLines { sequence ->
            sequence.forEach { line ->
                if (lines.size == MAX_LOG_LINES) lines.removeFirst()
                lines.addLast(line)
            }
        }

        val trimmed = lines.joinToString("\n", postfix = if (lines.isEmpty()) "" else "\n")
        OutputStreamWriter(
            FileOutputStream(logFile, false), Charsets.UTF_8
        ).buffered().use { it.write(trimmed) }
        Log.i(TAG, "Log truncated to ${lines.size} lines")
    }
}

fun Context.debugLog(message: String) {
    DebugFileLogger.initialize(this)
    DebugFileLogger.log(this, message)
}
