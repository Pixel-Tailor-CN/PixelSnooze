package vip.mystery0.pixel.snooze.temporaryrest

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface TemporaryRestState {
    data object Disabled : TemporaryRestState

    data class UntilDate(
        val endDate: LocalDate
    ) : TemporaryRestState

    data object UntilDisabled : TemporaryRestState
}

fun TemporaryRestState.isActive(today: LocalDate = LocalDate.now()): Boolean {
    return when (this) {
        TemporaryRestState.Disabled -> false
        is TemporaryRestState.UntilDate -> !today.isAfter(endDate)
        TemporaryRestState.UntilDisabled -> true
    }
}

fun TemporaryRestState.summaryText(today: LocalDate = LocalDate.now()): String {
    return when {
        !isActive(today) -> "已关闭"
        this is TemporaryRestState.UntilDate && endDate == today -> "仅今天有效"
        this is TemporaryRestState.UntilDate -> "有效至 ${endDate.format(dateFormatter)}（含当天）"
        else -> "直到手动关闭"
    }
}

fun TemporaryRestState.shortSummaryText(today: LocalDate = LocalDate.now()): String {
    return when {
        !isActive(today) -> "已关闭"
        this is TemporaryRestState.UntilDate && endDate == today -> "仅今天"
        this is TemporaryRestState.UntilDate -> "至 ${endDate.format(shortDateFormatter)}"
        else -> "手动关闭"
    }
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M 月 d 日", Locale.CHINA)

private val shortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M/d", Locale.CHINA)
