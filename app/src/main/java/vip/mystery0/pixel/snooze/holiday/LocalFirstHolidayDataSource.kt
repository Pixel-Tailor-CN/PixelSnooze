package vip.mystery0.pixel.snooze.holiday

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import java.time.format.DateTimeParseException
import java.util.concurrent.Executors
import org.json.JSONException

class LocalFirstHolidayDataSource(
    private val context: Context,
    private val assetName: String = "holiday.json",
    private val cacheFileName: String = "holiday.json",
    private val remoteUrlProvider: () -> String = { HolidayDataConfig.DEFAULT_REMOTE_URL }
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
        refreshFromRemoteUrl(remoteUrlProvider()) { result ->
            onComplete(result.success)
        }
    }

    override fun refreshFromRemoteUrl(
        remoteUrl: String,
        onComplete: (HolidayRefreshResult) -> Unit
    ) {
        executor.execute {
            val result = runCatching {
                val content = downloadRemoteHolidayJson(remoteUrl)
                AssetHolidayDataSource.parse(content)
                writeCache(content)
            }.onFailure { error ->
                Log.w(TAG, "Remote holiday update failed", error)
            }.fold(
                onSuccess = { HolidayRefreshResult(success = true) },
                onFailure = { error ->
                    HolidayRefreshResult(
                        success = false,
                        errorMessage = error.toHolidayRefreshErrorMessage()
                    )
                }
            )
            onComplete(result)
        }
    }

    override fun clearCache() {
        val target = cacheFile()
        if (target.isFile && !target.delete()) {
            Log.w(TAG, "Failed to delete cached holiday data")
        }
    }

    private fun downloadRemoteHolidayJson(remoteUrl: String): String {
        val connection = URL(remoteUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MILLIS
        connection.readTimeout = NETWORK_TIMEOUT_MILLIS
        connection.requestMethod = "GET"
        return connection.use {
            if (responseCode !in 200..299) {
                throw UnexpectedHolidayResponseException(responseCode)
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
    }
}

private class UnexpectedHolidayResponseException(
    val responseCode: Int
) : IllegalStateException("Unexpected holiday response code: $responseCode")

private fun Throwable.toHolidayRefreshErrorMessage(): String {
    return when (this) {
        is UnknownHostException -> "无法连接到数据源，请检查 URL 或网络连接"
        is SocketTimeoutException -> "连接数据源超时，请稍后重试"
        is UnexpectedHolidayResponseException -> "数据源响应异常（HTTP $responseCode）"
        is JSONException -> "响应内容不是有效的 Pixel Snooze 节假日 JSON"
        is DateTimeParseException -> "响应内容包含不符合 yyyy-MM-dd 格式的日期"
        is IOException -> "无法读取云端节假日数据"
        else -> "节假日数据不可用，请检查 URL 和 JSON 格式"
    }
}

private inline fun <T : HttpURLConnection, R> T.use(block: T.() -> R): R {
    return try {
        block()
    } finally {
        disconnect()
    }
}
