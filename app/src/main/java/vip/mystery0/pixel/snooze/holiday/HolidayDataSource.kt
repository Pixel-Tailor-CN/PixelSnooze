package vip.mystery0.pixel.snooze.holiday

interface HolidayDataSource {
    fun loadCalendar(): HolidayCalendar
}

interface RefreshableHolidayDataSource : HolidayDataSource {
    fun refreshFromRemote(onComplete: (Boolean) -> Unit = {})
}
