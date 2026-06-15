package vip.mystery0.pixel.snooze.schedule

import java.lang.Math.floorMod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class RestScheduleMode {
    HOLIDAY_AND_WEEKEND,
    HOLIDAY_AND_SINGLE_DAY_OFF,
    HOLIDAY_AND_ALTERNATING_WEEK,
    HOLIDAY_AND_CYCLE,
    CUSTOM
}

enum class AlternatingWeekType {
    LARGE,
    SMALL;

    fun opposite(): AlternatingWeekType {
        return when (this) {
            LARGE -> SMALL
            SMALL -> LARGE
        }
    }
}

data class CustomMonthlySchedule(
    val month: YearMonth,
    val workDates: Set<LocalDate> = emptySet(),
    val restDates: Set<LocalDate> = emptySet()
) {
    init {
        require(workDates.none { YearMonth.from(it) != month }) {
            "Custom work dates must belong to $month"
        }
        require(restDates.none { YearMonth.from(it) != month }) {
            "Custom rest dates must belong to $month"
        }
        require(workDates.intersect(restDates).isEmpty()) {
            "Custom work dates and rest dates must not overlap"
        }
    }
}

sealed interface RestScheduleRule {
    val mode: RestScheduleMode

    data object HolidayAndWeekend : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.HOLIDAY_AND_WEEKEND
    }

    data class HolidayAndSingleDayOff(
        val restDayOfWeek: DayOfWeek
    ) : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF
    }

    data class HolidayAndAlternatingWeek(
        val largeWeekRestDays: Set<DayOfWeek>,
        val smallWeekRestDays: Set<DayOfWeek>,
        val anchorWeekStartDate: LocalDate,
        val anchorWeekType: AlternatingWeekType
    ) : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK

        init {
            require(largeWeekRestDays.isNotEmpty()) { "Large week rest days must not be empty" }
            require(smallWeekRestDays.isNotEmpty()) { "Small week rest days must not be empty" }
            require(anchorWeekStartDate.dayOfWeek == DayOfWeek.MONDAY) {
                "Anchor week start date must be Monday"
            }
        }
    }

    data class HolidayAndCycle(
        val workDays: Int,
        val restDays: Int,
        val anchorDate: LocalDate,
        val anchorDayIndex: Int
    ) : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.HOLIDAY_AND_CYCLE

        init {
            require(workDays >= 1) { "Work days must be positive" }
            require(restDays >= 1) { "Rest days must be positive" }
            require(anchorDayIndex in 1..(workDays + restDays)) {
                "Anchor day index must be within the cycle"
            }
        }
    }

    data class Custom(
        val monthlySchedules: Map<YearMonth, CustomMonthlySchedule>
    ) : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.CUSTOM
    }
}

fun RestScheduleRule.isScheduleRestDay(date: LocalDate): Boolean {
    return when (this) {
        RestScheduleRule.HolidayAndWeekend -> {
            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        }

        is RestScheduleRule.HolidayAndSingleDayOff -> {
            date.dayOfWeek == restDayOfWeek
        }

        is RestScheduleRule.HolidayAndAlternatingWeek -> {
            val currentWeekStart = date.startOfWeek()
            val weeksBetween = ChronoUnit.WEEKS.between(anchorWeekStartDate, currentWeekStart)
            val currentWeekType = if (floorMod(weeksBetween, 2L) == 0L) {
                anchorWeekType
            } else {
                anchorWeekType.opposite()
            }
            val restDays = when (currentWeekType) {
                AlternatingWeekType.LARGE -> largeWeekRestDays
                AlternatingWeekType.SMALL -> smallWeekRestDays
            }
            date.dayOfWeek in restDays
        }

        is RestScheduleRule.HolidayAndCycle -> {
            val cycleLength = workDays + restDays
            val daysBetween = ChronoUnit.DAYS.between(anchorDate, date)
            val currentIndex = floorMod(anchorDayIndex - 1L + daysBetween, cycleLength.toLong()) + 1
            currentIndex > workDays.toLong()
        }

        is RestScheduleRule.Custom -> {
            val month = YearMonth.from(date)
            date in monthlySchedules[month]?.restDates.orEmpty()
        }
    }
}

fun RestScheduleRule.summaryText(): String {
    return when (this) {
        RestScheduleRule.HolidayAndWeekend -> "节假日 + 双休"
        is RestScheduleRule.HolidayAndSingleDayOff -> "节假日 + 单休（${restDayOfWeek.displayText()}）"
        is RestScheduleRule.HolidayAndAlternatingWeek -> {
            "节假日 + 大小周（本周锚点：${anchorWeekType.displayText()}）"
        }
        is RestScheduleRule.HolidayAndCycle -> "节假日 + 上 $workDays 休 $restDays"
        is RestScheduleRule.Custom -> "完全自定义"
    }
}

fun LocalDate.startOfWeek(): LocalDate {
    return with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

fun DayOfWeek.displayText(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}

fun AlternatingWeekType.displayText(): String {
    return when (this) {
        AlternatingWeekType.LARGE -> "大周"
        AlternatingWeekType.SMALL -> "小周"
    }
}
