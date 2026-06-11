package vip.mystery0.pixel.snooze.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import vip.mystery0.pixel.snooze.R
import vip.mystery0.pixel.snooze.history.AlarmHistoryRepository
import vip.mystery0.pixel.snooze.history.AlarmHistorySnapshot
import vip.mystery0.pixel.snooze.history.AlarmNotificationExecutionEvent
import vip.mystery0.pixel.snooze.history.AlarmSkipEvent
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.notification.PixelSnoozeNotificationListenerService
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    holidayRepository: HolidayRepository,
    preferencesRepository: UserPreferencesRepository,
    historyRepository: AlarmHistoryRepository,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var listenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var historySnapshot by remember { mutableStateOf(historyRepository.snapshot()) }
    var keyword by remember { mutableStateOf(preferencesRepository.keyword()) }
    var dismissWordsText by remember { mutableStateOf(preferencesRepository.dismissWordsText()) }
    var showKeywordDialog by remember { mutableStateOf(false) }
    var showDismissWordsDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    val calendar = remember { holidayRepository.currentCalendar() }

    DisposableEffect(lifecycleOwner, context, historyRepository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = isNotificationListenerEnabled(context)
                historySnapshot = historyRepository.snapshot()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pixel Snooze",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            context.openUrl("https://github.com/Pixel-Tailor-CN/PixelSnooze")
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = "GitHub"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "设置"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "本地优先的节假日闹钟跳过工具",
                style = MaterialTheme.typography.bodyLarge
            )

            StatusRow(
                label = "通知监听",
                value = if (listenerEnabled) "已启用" else "未启用",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )
            StatusRow(
                label = "关键词",
                value = keyword,
                onClick = { showKeywordDialog = true }
            )
            StatusRow(
                label = "跳过按钮文本",
                value = dismissWordsText.toSingleLineSummary(),
                onClick = { showDismissWordsDialog = true }
            )
            StatusRow(
                label = "调休日历",
                value = "${calendar.year} 年，${calendar.holidayCount()} 个休息日",
                onClick = { showCalendarDialog = true }
            )

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !listenerEnabled
            ) {
                Text(if (listenerEnabled) "通知监听已开启" else "打开通知监听设置")
            }

            AlarmHistoryContent(historySnapshot)
        }
    }

    if (showKeywordDialog) {
        KeywordEditDialog(
            initialKeyword = keyword,
            onDismiss = { showKeywordDialog = false },
            onSave = { input ->
                preferencesRepository.updateKeyword(input)
                keyword = preferencesRepository.keyword()
                showKeywordDialog = false
                Toast.makeText(context, "关键词已保存", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDismissWordsDialog) {
        DismissWordsEditDialog(
            initialText = dismissWordsText,
            onDismiss = { showDismissWordsDialog = false },
            onSave = { input ->
                preferencesRepository.updateDismissWords(input)
                dismissWordsText = preferencesRepository.dismissWordsText()
                showDismissWordsDialog = false
                Toast.makeText(context, "跳过按钮文本已保存", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCalendarDialog) {
        HolidayCalendarDialog(
            year = calendar.year,
            holidays = calendar.holidays.sorted(),
            onDismiss = { showCalendarDialog = false }
        )
    }
}

@Composable
private fun AlarmHistoryContent(snapshot: AlarmHistorySnapshot) {
    HistorySection(
        title = "自动跳过闹钟记录",
        emptyText = "暂无自动跳过记录",
        isEmpty = snapshot.skipEvents.isEmpty()
    ) {
        snapshot.skipEvents.forEach { event ->
            HistoryItem(
                title = "${event.timestamp.formatHistoryTime()} 自动跳过",
                summary = event.notificationSummary()
            )
        }
    }

    HistorySection(
        title = "最近 ${AlarmHistoryRepository.MAX_NOTIFICATION_EVENT_COUNT} 次闹钟通知",
        emptyText = "暂无闹钟通知记录",
        isEmpty = snapshot.notificationExecutionEvents.isEmpty()
    ) {
        snapshot.notificationExecutionEvents.forEach { event ->
            HistoryItem(
                title = "${event.timestamp.formatHistoryTime()} ${event.action}",
                summary = "${event.reason} · ${event.notificationSummary()}"
            )
        }
    }
}

@Composable
private fun HistorySection(
    title: String,
    emptyText: String,
    isEmpty: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
            if (isEmpty) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(title: String, summary: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Surface(
        modifier = modifier,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun KeywordEditDialog(
    initialKeyword: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var input by remember(initialKeyword) { mutableStateOf(initialKeyword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关键词") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "当闹钟通知标题或正文命中这里的内容时，应用会继续尝试执行通知里的跳过操作。",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("关键词") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(input) }) {
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

@Composable
private fun DismissWordsEditDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var input by remember(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳过按钮文本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "应用会在闹钟通知的按钮标题中查找这些文本，匹配到任意一项后执行对应操作。",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("每行一个文本") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(input) }) {
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

@Composable
private fun HolidayCalendarDialog(
    year: Int,
    holidays: List<LocalDate>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调休日历") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$year 年，共 ${holidays.size} 个休息日",
                    style = MaterialTheme.typography.bodyMedium
                )
                holidays.forEach { date ->
                    Text(
                        text = date.format(calendarDateFormatter),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    if (enabledListeners.isNullOrBlank()) return false

    val expectedName = ComponentName(
        context,
        PixelSnoozeNotificationListenerService::class.java
    ).flattenToString()

    return TextUtils.split(enabledListeners, ":").any { it.equals(expectedName, ignoreCase = true) }
}

private fun AlarmSkipEvent.notificationSummary(): String {
    return listOfNotNull(title, text).firstOrNull { it.isNotBlank() } ?: packageName
}

private fun AlarmNotificationExecutionEvent.notificationSummary(): String {
    return listOfNotNull(title, text).firstOrNull { it.isNotBlank() } ?: packageName
}

private fun Long.formatHistoryTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(historyTimeFormatter)
}

private val historyTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")

private val calendarDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun String.toSingleLineSummary(): String {
    return lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("、")
}

private fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
