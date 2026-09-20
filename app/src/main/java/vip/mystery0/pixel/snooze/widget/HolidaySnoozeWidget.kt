package vip.mystery0.pixel.snooze.widget

import android.app.AlarmManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.koin.mp.KoinPlatform
import vip.mystery0.pixel.snooze.MainActivity
import vip.mystery0.pixel.snooze.R
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.schedule.RestSchedulePreferencesRepository
import vip.mystery0.pixel.snooze.schedule.RestScheduleRule
import vip.mystery0.pixel.snooze.schedule.isScheduleRestDay
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestManager
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestState
import vip.mystery0.pixel.snooze.temporaryrest.isActive
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HolidaySnoozeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = KoinPlatform.getKoin()
        val holidayRepository = koin.get<HolidayRepository>()
        val scheduleRepository = koin.get<RestSchedulePreferencesRepository>()
        val temporaryRestManager = koin.get<TemporaryRestManager>()

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val nextAlarm = alarmManager?.nextAlarmClock

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val nextAlarmDateTime = nextAlarm?.let {
            Instant.ofEpochMilli(it.triggerTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }
        val nextAlarmDate = nextAlarmDateTime?.toLocalDate()

        val isAlarmToday = nextAlarmDate == today
        val targetDate = if (isAlarmToday) today else tomorrow

        val datePrefix = if (targetDate == today) "今天" else "明天"
        val weekText = targetDate.dayOfWeek.toChineseText()
        val dateTitle = "$datePrefix ${targetDate.monthValue}月${targetDate.dayOfMonth}日 $weekText"
        val calendar = holidayRepository.currentCalendar()
        val rule = scheduleRepository.currentRule()

        val isMakeupWorkday = targetDate in calendar.workdays
        val isHoliday = targetDate in calendar.holidays
        val isRuleRestDay = if (rule is RestScheduleRule.Custom) {
            rule.isScheduleRestDay(targetDate)
        } else {
            !isMakeupWorkday && (isHoliday || rule.isScheduleRestDay(targetDate))
        }

        val currentRestState = temporaryRestManager.currentState()
        val isCurrentRestActive = currentRestState !is TemporaryRestState.Disabled
        val isTemporaryRestActive = currentRestState.isActive(targetDate)
        val willSkipAlarm = isTemporaryRestActive || isRuleRestDay

        val hasTargetAlarm = nextAlarmDateTime != null && nextAlarmDate == targetDate
        val alarmTimeStr = if (hasTargetAlarm) {
            nextAlarmDateTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else if (nextAlarmDateTime != null && nextAlarmDate != null) {
            val prefix = when (nextAlarmDate) {
                today -> "今天"
                tomorrow -> "明天"
                else -> "${nextAlarmDate.monthValue}/${nextAlarmDate.dayOfMonth}"
            }
            "$prefix ${nextAlarmDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        } else {
            null
        }

        val badgeTag: String
        val primaryStatusText: String
        val reasonDetailText: String
        val isAttentionNeeded: Boolean

        when {
            isTemporaryRestActive -> {
                badgeTag = "临时休息"
                primaryStatusText = "已跳过闹钟"
                reasonDetailText = if (alarmTimeStr != null) "手动开启 · 原 $alarmTimeStr 跳过" else "临时休息生效 · 闹钟不响"
                isAttentionNeeded = true
            }
            isHoliday -> {
                badgeTag = "法定节假日"
                primaryStatusText = "自动跳过闹钟"
                reasonDetailText = if (alarmTimeStr != null) "法定假期 · 原 $alarmTimeStr 跳过" else "法定假日放假 · 闹钟不响"
                isAttentionNeeded = true
            }
            isMakeupWorkday -> {
                badgeTag = "调休上班"
                primaryStatusText = "闹钟正常响铃"
                reasonDetailText = if (alarmTimeStr != null) "周末调休 · 闹钟 $alarmTimeStr 响" else "周末调休补班 · 请注意作息"
                isAttentionNeeded = true
            }
            isRuleRestDay -> {
                badgeTag = "休息日"
                primaryStatusText = "休息日不响"
                reasonDetailText = if (alarmTimeStr != null) "常规周末 · 原 $alarmTimeStr 跳过" else "常规周末休息 · 无需早起"
                isAttentionNeeded = false
            }
            else -> {
                badgeTag = "工作日"
                primaryStatusText = "正常响铃"
                reasonDetailText = if (alarmTimeStr != null) "常规工作日 · 闹钟 $alarmTimeStr" else "常规工作日 · 暂无闹钟"
                isAttentionNeeded = false
            }
        }

        // 核心图标映射：临时休息开启时展示专属休息图标；其余跳过展示 alarm_off；正常响铃展示 alarm
        val statusIconRes = when {
            isTemporaryRestActive -> R.drawable.ic_temporary_rest
            willSkipAlarm -> R.drawable.ic_alarm_off
            else -> R.drawable.ic_alarm
        }

        // 开关按钮可用性逻辑：
        // 1. 如果当前已经处于开启状态，必须允许点击关闭！
        // 2. 如果当前处于关闭状态，只有下一个闹钟在今天或明天时才允许开启
        val hasNearAlarm = nextAlarmDate != null && (nextAlarmDate == today || nextAlarmDate == tomorrow)
        val isSwitchEnabled = isCurrentRestActive || hasNearAlarm

        val btnStateText = when {
            isCurrentRestActive -> "已开启"
            !isSwitchEnabled -> "不可用"
            else -> "已关闭"
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    dateTitle = dateTitle,
                    badgeTag = badgeTag,
                    statusIconRes = statusIconRes,
                    primaryStatusText = primaryStatusText,
                    reasonDetailText = reasonDetailText,
                    willSkipAlarm = willSkipAlarm,
                    isAttentionNeeded = isAttentionNeeded,
                    isSwitchEnabled = isSwitchEnabled,
                    isTemporaryRestActive = isCurrentRestActive,
                    btnStateText = btnStateText
                )
            }
        }
    }
}
@Composable
private fun WidgetContent(
    dateTitle: String,
    badgeTag: String,
    statusIconRes: Int,
    primaryStatusText: String,
    reasonDetailText: String,
    willSkipAlarm: Boolean,
    isAttentionNeeded: Boolean,
    isSwitchEnabled: Boolean,
    isTemporaryRestActive: Boolean,
    btnStateText: String
) {
    val colors = GlanceTheme.colors

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // 左侧：以“明天闹钟会不会响”为绝对核心，信息不空旷、层次分明
            Column(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .defaultWeight()
            ) {
                // 1. 顶部：日期与作息属性
                Row(
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = dateTitle,
                        style = TextStyle(
                            color = colors.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Box(
                        modifier = GlanceModifier
                            .background(
                                if (isAttentionNeeded) colors.primaryContainer
                                else colors.secondaryContainer
                            )
                            .cornerRadius(6.dp)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badgeTag,
                            style = TextStyle(
                                color = if (isAttentionNeeded) colors.onPrimaryContainer
                                else colors.onSecondaryContainer,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 2. 中间：第一核心结论（字号突出，带对应场景精确图标点缀）
                val statusTextColor = when {
                    isAttentionNeeded && willSkipAlarm -> colors.primary
                    isAttentionNeeded && !willSkipAlarm -> colors.onSurface
                    else -> colors.onSurfaceVariant
                }

                Row(
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(statusIconRes),
                        contentDescription = null,
                        modifier = GlanceModifier.size(24.dp),
                        colorFilter = ColorFilter.tint(statusTextColor)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = primaryStatusText,
                        style = TextStyle(
                            color = statusTextColor,
                            fontSize = 25.sp,
                            fontWeight = if (isAttentionNeeded) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 3. 底部：详细解释原因与闹钟时间
                Box(
                    modifier = GlanceModifier
                        .background(colors.surfaceVariant)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = reasonDetailText,
                        style = TextStyle(
                            color = colors.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))
            // 右侧：实体质感 M3 开关按钮
            val btnBg = when {
                !isSwitchEnabled -> colors.surfaceVariant
                isTemporaryRestActive -> colors.primary
                else -> colors.surfaceVariant
            }

            val btnIconCircleBg = when {
                !isSwitchEnabled -> colors.surfaceVariant
                isTemporaryRestActive -> colors.primaryContainer
                else -> colors.secondaryContainer
            }

            val btnIconTint = when {
                !isSwitchEnabled -> colors.outline
                isTemporaryRestActive -> colors.onPrimaryContainer
                else -> colors.onSecondaryContainer
            }

            val btnTitleColor = when {
                !isSwitchEnabled -> colors.outline
                isTemporaryRestActive -> colors.onPrimary
                else -> colors.onSurfaceVariant
            }

            val pillBg = when {
                !isSwitchEnabled -> colors.surfaceVariant
                isTemporaryRestActive -> colors.onPrimary
                else -> colors.primaryContainer
            }

            val pillTextColor = when {
                !isSwitchEnabled -> colors.outline
                isTemporaryRestActive -> colors.primary
                else -> colors.onPrimaryContainer
            }

            val buttonModifier = GlanceModifier
                .width(80.dp)
                .fillMaxHeight()
                .background(btnBg)
                .cornerRadius(18.dp)
                .padding(vertical = 12.dp, horizontal = 4.dp)

            val finalModifier = if (isSwitchEnabled) {
                buttonModifier.clickable(actionRunCallback<ToggleTemporaryRestAction>())
            } else {
                buttonModifier
            }

            Box(
                modifier = finalModifier,
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(btnIconCircleBg)
                            .cornerRadius(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_temporary_rest),
                            contentDescription = null,
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = ColorFilter.tint(btnIconTint)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Text(
                        text = "临时休息",
                        style = TextStyle(
                            color = btnTitleColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Box(
                        modifier = GlanceModifier
                            .background(pillBg)
                            .cornerRadius(10.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = btnStateText,
                            style = TextStyle(
                                color = pillTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class ToggleTemporaryRestAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val koin = KoinPlatform.getKoin()
        val temporaryRestManager = koin.get<TemporaryRestManager>()

        // 1. 如果临时休息模式已开启，点击的核心意图就是直接关闭它！无条件关闭并立即刷新！
        if (temporaryRestManager.currentState() !is TemporaryRestState.Disabled) {
            temporaryRestManager.disable()
            HolidaySnoozeWidget().updateAll(context)
            return
        }

        // 2. 如果未开启，则只有当下一个闹钟在今天或明天时才允许开启
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val nextAlarm = alarmManager?.nextAlarmClock ?: return

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val alarmDateTime = Instant.ofEpochMilli(nextAlarm.triggerTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val alarmDate = alarmDateTime.toLocalDate()

        if (alarmDate != today && alarmDate != tomorrow) {
            return
        }

        if (alarmDate == tomorrow) {
            temporaryRestManager.enableUntil(tomorrow)
        } else {
            temporaryRestManager.enableToday()
        }

        HolidaySnoozeWidget().updateAll(context)
    }
}

private fun DayOfWeek.toChineseText(): String {
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
