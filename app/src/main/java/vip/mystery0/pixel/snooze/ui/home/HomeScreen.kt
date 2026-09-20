package vip.mystery0.pixel.snooze.ui.home

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import vip.mystery0.pixel.snooze.R
import vip.mystery0.pixel.snooze.history.AlarmHistoryRepository
import vip.mystery0.pixel.snooze.history.AlarmHistorySnapshot
import vip.mystery0.pixel.snooze.history.AlarmNotificationExecutionEvent
import vip.mystery0.pixel.snooze.history.AlarmSkipEvent
import vip.mystery0.pixel.snooze.holiday.HolidayCalendar
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.notification.PixelSnoozeNotificationListenerService
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository
import vip.mystery0.pixel.snooze.schedule.RestDayRepository
import vip.mystery0.pixel.snooze.schedule.summaryText
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestManager
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    holidayRepository: HolidayRepository,
    preferencesRepository: UserPreferencesRepository,
    restDayRepository: RestDayRepository,
    temporaryRestManager: TemporaryRestManager,
    historyRepository: AlarmHistoryRepository,
    onOpenRestSchedule: () -> Unit,
    onOpenSettings: () -> Unit,
    showTemporaryRestDialog: Boolean,
    onShowTemporaryRestDialog: () -> Unit,
    onHideTemporaryRestDialog: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var listenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var historySnapshot by remember { mutableStateOf(historyRepository.snapshot()) }
    var keyword by remember { mutableStateOf(preferencesRepository.keyword()) }
    var dismissWordsText by remember { mutableStateOf(preferencesRepository.dismissWordsText()) }
    var restRule by remember { mutableStateOf(restDayRepository.currentRule()) }
    var temporaryRestState by remember {
        mutableStateOf(temporaryRestManager.currentState())
    }
    var showKeywordDialog by remember { mutableStateOf(false) }
    var showDismissWordsDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showOnboardingGuideDialog by remember {
        mutableStateOf(!preferencesRepository.hasSeenOnboardingGuide())
    }
    var isRefreshingCalendar by remember { mutableStateOf(false) }
    var calendar by remember { mutableStateOf(holidayRepository.currentCalendar()) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        temporaryRestManager.refreshSurfaces()
        temporaryRestState = temporaryRestManager.currentState()
    }

    fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED &&
            temporaryRestManager.shouldRequestNotificationPermission()
        ) {
            temporaryRestManager.markNotificationPermissionRequested()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(
        lifecycleOwner,
        context,
        historyRepository,
        holidayRepository,
        restDayRepository,
        temporaryRestManager
    ) {
        val temporaryRestStateListener: (TemporaryRestState) -> Unit = { state ->
            mainHandler.post {
                temporaryRestState = state
            }
        }
        temporaryRestManager.addStateListener(temporaryRestStateListener)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = isNotificationListenerEnabled(context)
                historySnapshot = historyRepository.snapshot()
                restRule = restDayRepository.currentRule()
                temporaryRestState = temporaryRestManager.currentState()
                calendar = holidayRepository.currentCalendar()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            temporaryRestManager.removeStateListener(temporaryRestStateListener)
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
                    IconButton(onClick = { showOnboardingGuideDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = "使用引导"
                        )
                    }
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

            TemporaryRestCard(
                state = temporaryRestState,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        temporaryRestManager.enableToday()
                        requestNotificationPermissionIfNeeded()
                    } else {
                        temporaryRestManager.disable()
                    }
                    temporaryRestState = temporaryRestManager.currentState()
                },
                onConfigure = onShowTemporaryRestDialog,
                onAddTile = { requestTemporaryRestTile(context) }
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
                label = "休息日规则",
                value = restRule.summaryText(),
                onClick = onOpenRestSchedule
            )
            StatusRow(
                label = "调休日历",
                value = calendar.summaryText(),
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

    if (showOnboardingGuideDialog) {
        OnboardingGuideDialog(
            listenerEnabled = listenerEnabled,
            onOpenNotificationListenerSettings = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            onOpenRestSchedule = {
                onOpenRestSchedule()
            },
            onDismiss = {
                preferencesRepository.markOnboardingGuideSeen()
                showOnboardingGuideDialog = false
            }
        )
    }

    if (showTemporaryRestDialog) {
        TemporaryRestDurationDialog(
            state = temporaryRestState,
            onEnableToday = {
                temporaryRestManager.enableToday()
                temporaryRestState = temporaryRestManager.currentState()
                requestNotificationPermissionIfNeeded()
                onHideTemporaryRestDialog()
            },
            onEnableUntil = { endDate ->
                temporaryRestManager.enableUntil(endDate)
                temporaryRestState = temporaryRestManager.currentState()
                requestNotificationPermissionIfNeeded()
                onHideTemporaryRestDialog()
            },
            onEnableUntilDisabled = {
                temporaryRestManager.enableUntilDisabled()
                temporaryRestState = temporaryRestManager.currentState()
                requestNotificationPermissionIfNeeded()
                onHideTemporaryRestDialog()
            },
            onDisable = {
                temporaryRestManager.disable()
                temporaryRestState = TemporaryRestState.Disabled
                onHideTemporaryRestDialog()
            },
            onDismiss = onHideTemporaryRestDialog
        )
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
            calendar = calendar,
            isRefreshing = isRefreshingCalendar,
            onRefresh = {
                if (isRefreshingCalendar) return@HolidayCalendarDialog
                isRefreshingCalendar = true
                holidayRepository.refreshFromRemote { success ->
                    mainHandler.post {
                        isRefreshingCalendar = false
                        calendar = holidayRepository.currentCalendar()
                        Toast.makeText(
                            context,
                            if (success) "调休日历已更新" else "调休日历更新失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = { showCalendarDialog = false }
        )
    }
}
