package vip.mystery0.pixel.snooze.holiday

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class LocalFirstHolidayDataSource(
    private val context: Context,
    private val assetName: String = "holiday.json",
    private val cacheFileName: String = "holiday.json",
    private val remoteUrl: String = DEFAULT_REMOTE_URL
) : RefreshableHolidayDataSource {
    private val assetDataSource = AssetHolidayDataSource(context, assetName)
    private val executor = Executors.newSingleThreadExecutor()

    override fun loadCalendar(): HolidayCalendar {
        val cacheFile = cacheFile()
        if (cacheFile.isFile) {
            val cachedCalendar = runCatching {
                AssetHolidayDataSource.parse(cacheFile.readText(Charsets.UTF_8))
            }.getOrElse { error ->
                Log.w(TAG, "Cached holiday data is invalid", error)
                null
            }
            if (cachedCalendar != null) return cachedCalendar
        }
        return assetDataSource.loadCalendar()
    }

    override fun refreshFromRemote(onComplete: (Boolean) -> Unit) {
        executor.execute {
            val success = runCatching {
                val content = downloadRemoteHolidayJson()
                AssetHolidayDataSource.parse(content)
                writeCache(content)
            }.onFailure { error ->
                Log.w(TAG, "Remote holiday update failed", error)
            }.isSuccess
            onComplete(success)
        }
    }

    private fun downloadRemoteHolidayJson(): String {
        val connection = URL(remoteUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MILLIS
        connection.readTimeout = NETWORK_TIMEOUT_MILLIS
        connection.requestMethod = "GET"
        return connection.use {
            if (responseCode !in 200..299) {
                error("Unexpected holiday response code: $responseCode")
            }
            inputStream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
        }
    }

    private fun writeCache(content: String) {
        val target = cacheFile()
        val temp = File(target.parentFile, "$cacheFileName.tmp")
        temp.writeText(content, Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Failed to replace holiday cache")
        }
    }

    private fun cacheFile(): File {
        return File(context.filesDir, cacheFileName)
    }

    companion object {
        private const val TAG = "HolidayDataSource"
        private const val NETWORK_TIMEOUT_MILLIS = 30_000
        private const val DEFAULT_REMOTE_URL =
            "https://raw.githubusercontent.com/Pixel-Tailor-CN/PixelSnooze/refs/heads/main/json/holiday.json"
    }
}

private inline fun <T : HttpURLConnection, R> T.use(block: T.() -> R): R {
    return try {
        block()
    } finally {
        disconnect()
    }
}
