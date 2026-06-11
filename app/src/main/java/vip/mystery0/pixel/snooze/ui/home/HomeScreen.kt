package vip.mystery0.pixel.snooze.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import vip.mystery0.pixel.snooze.history.AlarmHistoryRepository
import vip.mystery0.pixel.snooze.history.AlarmHistorySnapshot
import vip.mystery0.pixel.snooze.history.AlarmNotificationExecutionEvent
import vip.mystery0.pixel.snooze.history.AlarmSkipEvent
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.notification.PixelSnoozeNotificationListenerService
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository
import java.time.Instant
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

            StatusRow("通知监听", if (listenerEnabled) "已启用" else "未启用")
            StatusRow("关键词", preferencesRepository.keyword())
            StatusRow("内置日历", "${calendar.year} 年，${calendar.holidayCount()} 个休息日")

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开通知监听设置")
            }

            AlarmHistoryContent(historySnapshot)
        }
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
private fun StatusRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
