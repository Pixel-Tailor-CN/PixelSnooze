package vip.mystery0.pixel.snooze.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.snooze.schedule.CustomMonthlySchedule
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomScheduleCalendarDialog(
    initialSchedule: CustomMonthlySchedule,
    onSave: (CustomMonthlySchedule) -> Unit,
    onDismiss: () -> Unit
) {
    var workDates by remember(initialSchedule) { mutableStateOf(initialSchedule.workDates) }
    var restDates by remember(initialSchedule) { mutableStateOf(initialSchedule.restDates) }
    val month = initialSchedule.month
    val dates = remember(month) { month.datesInMonth() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${month.format(monthFormatter)} 自定义") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "未设置日期默认不休息，不自动跳过。",
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dates.forEach { date ->
                        val stateText = when (date) {
                            in restDates -> "${date.dayOfMonth} 休"
                            in workDates -> "${date.dayOfMonth} 班"
                            else -> "${date.dayOfMonth}"
                        }
                        FilterChip(
                            selected = date in restDates || date in workDates,
                            onClick = {
                                when (date) {
                                    in restDates -> {
                                        restDates = restDates - date
                                        workDates = workDates + date
                                    }

                                    in workDates -> {
                                        workDates = workDates - date
                                    }

                                    else -> {
                                        restDates = restDates + date
                                    }
                                }
                            },
                            label = { Text(stateText) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("休息：${restDates.size} 天")
                    Text("上班：${workDates.size} 天")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        CustomMonthlySchedule(
                            month = month,
                            workDates = workDates,
                            restDates = restDates
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun YearMonth.datesInMonth(): List<LocalDate> {
    return (1..lengthOfMonth()).map { day -> atDay(day) }
}

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
