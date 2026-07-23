package vip.mystery0.pixel.snooze.ui.home

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.snooze.R
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestState
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestTileService
import vip.mystery0.pixel.snooze.temporaryrest.isActive
import vip.mystery0.pixel.snooze.temporaryrest.summaryText
import java.time.LocalDate

@Composable
fun TemporaryRestCard(
    state: TemporaryRestState,
    onCheckedChange: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    onAddTile: () -> Unit
) {
    val today = LocalDate.now()
    val isActive = state.isActive(today)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = if (isActive) 4.dp else 1.dp,
        color = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onConfigure)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "临时休息模式",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isActive) {
                            state.summaryText(today)
                        } else {
                            "临时请假时，将今天直接按休息日处理"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = onCheckedChange
                )
            }
            TextButton(
                onClick = onAddTile,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("添加快捷设置磁贴")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemporaryRestDurationDialog(
    state: TemporaryRestState,
    onEnableToday: () -> Unit,
    onEnableUntil: (LocalDate) -> Unit,
    onEnableUntilDisabled: () -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    var showDatePicker by remember { mutableStateOf(false) }
    var dateValidationError by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val initialDate = (state as? TemporaryRestState.UntilDate)
            ?.endDate
            ?.takeUnless { it.isBefore(today) }
            ?: today
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialDate.toEpochDay() * MILLIS_PER_DAY,
            selectableDates = remember(today) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis / MILLIS_PER_DAY >= today.toEpochDay()
                    }
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = {
                dateValidationError = false
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                            ?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY) }
                            ?: today
                        if (selectedDate.isBefore(LocalDate.now())) {
                            dateValidationError = true
                        } else {
                            onEnableUntil(selectedDate)
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dateValidationError = false
                        showDatePicker = false
                    }
                ) {
                    Text("返回")
                }
            }
        ) {
            Column {
                DatePicker(state = datePickerState)
                if (dateValidationError) {
                    Text(
                        text = "日期已经变化，请重新选择今天或之后的日期",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("临时休息模式") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "开启期间，Pixel Snooze 会将今天直接按休息日处理，原有休息日规则不会改变。",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextButton(
                    onClick = onEnableToday,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("仅今天")
                }
                TextButton(
                    onClick = {
                        dateValidationError = false
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("选择结束日期")
                }
                TextButton(
                    onClick = onEnableUntilDisabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("直到手动关闭")
                }
                if (state.isActive(today)) {
                    TextButton(
                        onClick = onDisable,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("关闭临时休息")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

fun requestTemporaryRestTile(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(
            context,
            "请下拉快捷设置并在编辑页面手动添加“临时休息”磁贴",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    val statusBarManager = context.getSystemService(StatusBarManager::class.java)
    runCatching {
        statusBarManager.requestAddTileService(
            ComponentName(context, TemporaryRestTileService::class.java),
            context.getString(R.string.temporary_rest_tile_label),
            Icon.createWithResource(context, R.drawable.ic_temporary_rest),
            context.mainExecutor
        ) { result ->
            val message = when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "快捷设置磁贴已添加"
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "快捷设置磁贴已经存在"
                else -> "未添加快捷设置磁贴"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }.onFailure {
        Toast.makeText(
            context,
            "无法请求添加磁贴，请在快捷设置编辑页面手动添加",
            Toast.LENGTH_LONG
        ).show()
    }
}

private const val MILLIS_PER_DAY = 86_400_000L
