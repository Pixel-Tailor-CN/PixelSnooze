package vip.mystery0.pixel.snooze.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.snooze.schedule.AlternatingWeekType
import vip.mystery0.pixel.snooze.schedule.RestScheduleMode
import vip.mystery0.pixel.snooze.schedule.RestScheduleRule
import vip.mystery0.pixel.snooze.schedule.displayText
import java.time.DayOfWeek

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestScheduleDialog(
    currentRule: RestScheduleRule,
    onSaveWeekend: () -> Unit,
    onSaveSingleDayOff: (DayOfWeek) -> Unit,
    onSaveAlternatingWeek: (Set<DayOfWeek>, Set<DayOfWeek>, AlternatingWeekType) -> Unit,
    onSaveCycle: (Int, Int, Int) -> Unit,
    onOpenCustomCalendar: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember(currentRule) { mutableStateOf(currentRule.mode) }
    var singleRestDay by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndSingleDayOff)?.restDayOfWeek
                ?: DayOfWeek.SUNDAY
        )
    }
    var largeWeekRestDays by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndAlternatingWeek)?.largeWeekRestDays
                ?: setOf(DayOfWeek.SUNDAY)
        )
    }
    var smallWeekRestDays by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndAlternatingWeek)?.smallWeekRestDays
                ?: setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        )
    }
    var anchorWeekType by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndAlternatingWeek)?.anchorWeekType
                ?: AlternatingWeekType.LARGE
        )
    }
    var workDaysText by remember(currentRule) {
        mutableStateOf((currentRule as? RestScheduleRule.HolidayAndCycle)?.workDays?.toString() ?: "3")
    }
    var restDaysText by remember(currentRule) {
        mutableStateOf((currentRule as? RestScheduleRule.HolidayAndCycle)?.restDays?.toString() ?: "2")
    }
    var todayIndexText by remember(currentRule) {
        mutableStateOf((currentRule as? RestScheduleRule.HolidayAndCycle)?.anchorDayIndex?.toString() ?: "1")
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("休息日规则") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScheduleModeRow("节假日 + 双休", RestScheduleMode.HOLIDAY_AND_WEEKEND, selectedMode) {
                    selectedMode = RestScheduleMode.HOLIDAY_AND_WEEKEND
                }
                ScheduleModeRow("节假日 + 单休", RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF, selectedMode) {
                    selectedMode = RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF
                }
                ScheduleModeRow("节假日 + 大小周", RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK, selectedMode) {
                    selectedMode = RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK
                }
                ScheduleModeRow("节假日 + 上 x 休 y", RestScheduleMode.HOLIDAY_AND_CYCLE, selectedMode) {
                    selectedMode = RestScheduleMode.HOLIDAY_AND_CYCLE
                }
                ScheduleModeRow("完全自定义", RestScheduleMode.CUSTOM, selectedMode) {
                    selectedMode = RestScheduleMode.CUSTOM
                }

                when (selectedMode) {
                    RestScheduleMode.HOLIDAY_AND_WEEKEND -> Text("周六、周日和节假日休息。")
                    RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF -> {
                        Text("选择每周休息日")
                        DayOfWeekSelector(
                            selectedDays = setOf(singleRestDay),
                            onToggle = { singleRestDay = it }
                        )
                    }

                    RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK -> {
                        Text("大周休息日")
                        DayOfWeekSelector(
                            selectedDays = largeWeekRestDays,
                            onToggle = { day ->
                                largeWeekRestDays = largeWeekRestDays.toggle(day).ifEmpty { setOf(day) }
                            }
                        )
                        Text("小周休息日")
                        DayOfWeekSelector(
                            selectedDays = smallWeekRestDays,
                            onToggle = { day ->
                                smallWeekRestDays = smallWeekRestDays.toggle(day).ifEmpty { setOf(day) }
                            }
                        )
                        Text("本周类型")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WeekTypeChip("大周", anchorWeekType == AlternatingWeekType.LARGE) {
                                anchorWeekType = AlternatingWeekType.LARGE
                            }
                            WeekTypeChip("小周", anchorWeekType == AlternatingWeekType.SMALL) {
                                anchorWeekType = AlternatingWeekType.SMALL
                            }
                        }
                    }

                    RestScheduleMode.HOLIDAY_AND_CYCLE -> {
                        OutlinedTextField(
                            value = workDaysText,
                            onValueChange = { workDaysText = it.filter(Char::isDigit) },
                            label = { Text("上班天数") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = restDaysText,
                            onValueChange = { restDaysText = it.filter(Char::isDigit) },
                            label = { Text("休息天数") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = todayIndexText,
                            onValueChange = { todayIndexText = it.filter(Char::isDigit) },
                            label = { Text("今天是周期第几天") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    RestScheduleMode.CUSTOM -> Text("完全自定义只使用你手动标记的休息日，不叠加节假日数据。")
                }

                errorText?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    errorText = null
                    when (selectedMode) {
                        RestScheduleMode.HOLIDAY_AND_WEEKEND -> onSaveWeekend()
                        RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF -> onSaveSingleDayOff(singleRestDay)
                        RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK -> {
                            onSaveAlternatingWeek(largeWeekRestDays, smallWeekRestDays, anchorWeekType)
                        }

                        RestScheduleMode.HOLIDAY_AND_CYCLE -> {
                            val workDays = workDaysText.toIntOrNull()
                            val restDays = restDaysText.toIntOrNull()
                            val todayIndex = todayIndexText.toIntOrNull()
                            if (workDays == null || restDays == null || todayIndex == null) {
                                errorText = "请填写完整的数字"
                                return@TextButton
                            }
                            if (workDays < 1 || restDays < 1 || todayIndex !in 1..(workDays + restDays)) {
                                errorText = "今天是第几天必须在 1 到 ${workDays + restDays} 之间"
                                return@TextButton
                            }
                            onSaveCycle(workDays, restDays, todayIndex)
                        }

                        RestScheduleMode.CUSTOM -> onOpenCustomCalendar()
                    }
                }
            ) {
                Text(if (selectedMode == RestScheduleMode.CUSTOM) "编辑" else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ScheduleModeRow(
    text: String,
    mode: RestScheduleMode,
    selectedMode: RestScheduleMode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = mode == selectedMode, onClick = onClick)
        Text(text = text, modifier = Modifier.padding(top = 12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day in selectedDays,
                onClick = { onToggle(day) },
                label = { Text(day.displayText()) }
            )
        }
    }
}

@Composable
private fun WeekTypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

private fun Set<DayOfWeek>.toggle(day: DayOfWeek): Set<DayOfWeek> {
    return if (day in this) this - day else this + day
}
