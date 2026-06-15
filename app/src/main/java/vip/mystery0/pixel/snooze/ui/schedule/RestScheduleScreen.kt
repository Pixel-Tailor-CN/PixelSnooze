package vip.mystery0.pixel.snooze.ui.schedule

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.snooze.schedule.AlternatingWeekType
import vip.mystery0.pixel.snooze.schedule.RestScheduleMode
import vip.mystery0.pixel.snooze.schedule.RestSchedulePreferencesRepository
import vip.mystery0.pixel.snooze.schedule.RestScheduleRule
import vip.mystery0.pixel.snooze.schedule.displayText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RestScheduleScreen(
    schedulePreferencesRepository: RestSchedulePreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val currentRule = remember { schedulePreferencesRepository.currentRule() }
    val futureDates = remember(today) { (0L until 30L).map { today.plusDays(it) } }
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
                ?: setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        )
    }
    var smallWeekRestDays by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndAlternatingWeek)?.smallWeekRestDays
                ?: setOf(DayOfWeek.SUNDAY)
        )
    }
    var anchorWeekType by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndAlternatingWeek)?.anchorWeekType
                ?: AlternatingWeekType.LARGE
        )
    }
    var cycleDaysText by remember(currentRule) {
        mutableStateOf((currentRule as? RestScheduleRule.HolidayAndCycle)?.cycleDays?.toString() ?: "5")
    }
    var todayIndexText by remember(currentRule) {
        mutableStateOf((currentRule as? RestScheduleRule.HolidayAndCycle)?.anchorDayIndex?.toString() ?: "1")
    }
    var cycleRestIndexes by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndCycle)?.restDayIndexes
                ?: setOf(4, 5)
        )
    }
    var customRestDates by remember(futureDates) {
        mutableStateOf(schedulePreferencesRepository.futureCustomRestDates(futureDates))
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭"
                        )
                    }
                },
                title = {
                    Text(
                        text = "休息日规则",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    errorText = null
                    val saved = when (selectedMode) {
                        RestScheduleMode.HOLIDAY_AND_WEEKEND -> {
                            schedulePreferencesRepository.updateHolidayAndWeekend()
                            true
                        }

                        RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF -> {
                            schedulePreferencesRepository.updateSingleDayOff(singleRestDay)
                            true
                        }

                        RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK -> {
                            schedulePreferencesRepository.updateAlternatingWeek(
                                largeWeekRestDays = largeWeekRestDays,
                                smallWeekRestDays = smallWeekRestDays,
                                anchorWeekType = anchorWeekType
                            )
                            true
                        }

                        RestScheduleMode.HOLIDAY_AND_CYCLE -> {
                            val cycleDays = cycleDaysText.toIntOrNull()
                            val todayIndex = todayIndexText.toIntOrNull()
                            if (cycleDays == null || todayIndex == null) {
                                errorText = "请填写完整的数字"
                                false
                            } else if (cycleDays < 1 || todayIndex !in 1..cycleDays) {
                                errorText = "今天是第几天必须在 1 到 $cycleDays 之间"
                                false
                            } else {
                                schedulePreferencesRepository.updateCycle(
                                    cycleDays = cycleDays,
                                    restDayIndexes = cycleRestIndexes.filter { it in 1..cycleDays }.toSet(),
                                    todayIndex = todayIndex
                                )
                                true
                            }
                        }

                        RestScheduleMode.CUSTOM -> {
                            schedulePreferencesRepository.updateCustomMode()
                            schedulePreferencesRepository.updateFutureCustomRestDates(
                                futureDates = futureDates,
                                restDates = customRestDates
                            )
                            true
                        }
                    }
                    if (saved) {
                        Toast.makeText(context, "休息日规则已保存", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("确定")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "mode_list") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ScheduleModeRow(
                        title = "节假日 + 双休",
                        summary = "周六、周日和节假日",
                        selected = selectedMode == RestScheduleMode.HOLIDAY_AND_WEEKEND,
                        onClick = {
                            selectedMode = RestScheduleMode.HOLIDAY_AND_WEEKEND
                            errorText = null
                        }
                    )
                    ScheduleModeRow(
                        title = "节假日 + 单休",
                        summary = "每周 ${singleRestDay.displayText()} 和节假日",
                        selected = selectedMode == RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF,
                        onClick = {
                            selectedMode = RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF
                            errorText = null
                        }
                    )
                    ScheduleModeRow(
                        title = "节假日 + 大小周",
                        summary = "本周按${anchorWeekType.displayText()}计算",
                        selected = selectedMode == RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK,
                        onClick = {
                            selectedMode = RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK
                            errorText = null
                        }
                    )
                    ScheduleModeRow(
                        title = "周期排班",
                        summary = "${cycleDaysText.ifBlank { "0" }} 天一个周期",
                        selected = selectedMode == RestScheduleMode.HOLIDAY_AND_CYCLE,
                        onClick = {
                            selectedMode = RestScheduleMode.HOLIDAY_AND_CYCLE
                            errorText = null
                        }
                    )
                    ScheduleModeRow(
                        title = "完全自定义",
                        summary = "未来 30 天",
                        selected = selectedMode == RestScheduleMode.CUSTOM,
                        onClick = {
                            selectedMode = RestScheduleMode.CUSTOM
                            errorText = null
                        }
                    )
                }
            }

            item(key = "selected_settings") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider()
                    when (selectedMode) {
                        RestScheduleMode.HOLIDAY_AND_WEEKEND -> {
                            Text(
                                text = "选择后，今天命中节假日数据、周六或周日时视为休息日。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF -> {
                            Text(text = "每周休息日", style = MaterialTheme.typography.titleSmall)
                            DayOfWeekSelector(
                                selectedDays = setOf(singleRestDay),
                                onToggle = { singleRestDay = it }
                            )
                        }

                        RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK -> {
                            Text(text = "大周休息日", style = MaterialTheme.typography.titleSmall)
                            DayOfWeekSelector(
                                selectedDays = largeWeekRestDays,
                                onToggle = { day ->
                                    largeWeekRestDays = largeWeekRestDays.toggle(day).ifEmpty { setOf(day) }
                                }
                            )
                            Text(text = "小周休息日", style = MaterialTheme.typography.titleSmall)
                            DayOfWeekSelector(
                                selectedDays = smallWeekRestDays,
                                onToggle = { day ->
                                    smallWeekRestDays = smallWeekRestDays.toggle(day).ifEmpty { setOf(day) }
                                }
                            )
                            Text(text = "本周类型", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WeekTypeChip(
                                    text = "大周",
                                    selected = anchorWeekType == AlternatingWeekType.LARGE,
                                    onClick = { anchorWeekType = AlternatingWeekType.LARGE }
                                )
                                WeekTypeChip(
                                    text = "小周",
                                    selected = anchorWeekType == AlternatingWeekType.SMALL,
                                    onClick = { anchorWeekType = AlternatingWeekType.SMALL }
                                )
                            }
                        }

                        RestScheduleMode.HOLIDAY_AND_CYCLE -> {
                            NumericTextField(
                                value = cycleDaysText,
                                onValueChange = { cycleDaysText = it },
                                label = "周期天数"
                            )
                            NumericTextField(
                                value = todayIndexText,
                                onValueChange = { todayIndexText = it },
                                label = "今天是周期第几天"
                            )
                        }

                        RestScheduleMode.CUSTOM -> {
                            Text(
                                text = "选择要休息的日期",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                    errorText?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (selectedMode == RestScheduleMode.HOLIDAY_AND_CYCLE) {
                val cycleDays = cycleDaysText.toIntOrNull()
                val todayIndex = todayIndexText.toIntOrNull()
                if (cycleDays != null && todayIndex != null && cycleDays >= 1 && todayIndex in 1..cycleDays) {
                    val cycleDates = (0L until cycleDays.toLong()).map { today.plusDays(it) }
                    items(
                        items = cycleDates,
                        key = { "cycle_$it" }
                    ) { date ->
                        val cycleIndex = date.cycleIndex(
                            today = today,
                            todayIndex = todayIndex,
                            cycleDays = cycleDays
                        )
                        ScheduleDateRow(
                            date = date,
                            today = today,
                            summary = "周期第 $cycleIndex 天",
                            checked = cycleIndex in cycleRestIndexes,
                            checkedText = "休息",
                            uncheckedText = "上班",
                            onCheckedChange = { checked ->
                                cycleRestIndexes = if (checked) {
                                    cycleRestIndexes + cycleIndex
                                } else {
                                    cycleRestIndexes - cycleIndex
                                }
                            }
                        )
                    }
                }
            }

            if (selectedMode == RestScheduleMode.CUSTOM) {
                items(
                    items = futureDates,
                    key = { "custom_$it" }
                ) { date ->
                    ScheduleDateRow(
                        date = date,
                        today = today,
                        summary = if (date in customRestDates) "已标记为休息日" else "未标记",
                        checked = date in customRestDates,
                        checkedText = "休息",
                        uncheckedText = "上班",
                        onCheckedChange = { checked ->
                            customRestDates = if (checked) {
                                customRestDates + date
                            } else {
                                customRestDates - date
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleModeRow(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

@Composable
private fun NumericTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ScheduleDateRow(
    date: LocalDate,
    today: LocalDate,
    summary: String,
    checked: Boolean,
    checkedText: String,
    uncheckedText: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date.displayDate(today),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (checked) checkedText else uncheckedText,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun LocalDate.cycleIndex(today: LocalDate, todayIndex: Int, cycleDays: Int): Int {
    val daysBetween = ChronoUnit.DAYS.between(today, this)
    return floorMod(todayIndex - 1L + daysBetween, cycleDays.toLong()).toInt() + 1
}

private fun LocalDate.displayDate(today: LocalDate): String {
    return if (this == today) {
        "今天"
    } else {
        format(dateFormatter)
    }
}

private fun Set<DayOfWeek>.toggle(day: DayOfWeek): Set<DayOfWeek> {
    return if (day in this) this - day else this + day
}

private fun floorMod(x: Long, y: Long): Long = ((x % y) + y) % y

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M 月 d 日，EEEE", Locale.CHINA)
