package vip.mystery0.pixel.snooze.holiday

interface HolidayDataSource {
    fun loadCalendar(): HolidayCalendar
}

interface RefreshableHolidayDataSource : HolidayDataSource {
    fun refreshFromRemote(onComplete: (Boolean) -> Unit = {})
    fun refreshFromRemoteUrl(
        remoteUrl: String,
        onComplete: (HolidayRefreshResult) -> Unit = {}
    )
    fun clearCache()
}

data class HolidayRefreshResult(
    val success: Boolean,
    val errorMessage: String? = null,
)
