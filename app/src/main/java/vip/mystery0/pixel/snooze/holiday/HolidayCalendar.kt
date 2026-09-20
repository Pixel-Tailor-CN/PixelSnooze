package vip.mystery0.pixel.snooze.holiday

import java.time.LocalDate

data class HolidayCalendar(
    val years: List<HolidayYear>,
) {
    val holidays: Set<LocalDate> = years.flatMap { it.holidays }.toSet()
    val workdays: Set<LocalDate> = years.flatMap { it.workdays }.toSet()

    fun holidayCount(): Int = holidays.size

    fun workdayCount(): Int = workdays.size
}

data class HolidayYear(
    val year: Int,
    val holidays: Set<LocalDate>,
    val workdays: Set<LocalDate> = emptySet(),
) {
    fun holidayCount(): Int = holidays.size

    fun workdayCount(): Int = workdays.size
}
