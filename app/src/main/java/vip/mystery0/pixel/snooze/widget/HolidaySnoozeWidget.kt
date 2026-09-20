package vip.mystery0.pixel.snooze.widget

import android.app.AlarmManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 桌面作息与闹钟预告小部件
 *
 * 支持两个响应式挡位：
 * 1. 2x3（宽3格高2格）：信息丰富的完整卡片布局，包含日期、作息状态、核心闹钟结论、原因说明与右侧大开关。
 * 2. 1x1（宽1格高1格）：紧凑精炼的双层布局，上半部分展示核心作息与响铃状态（点击打开应用），下半部分为独立的交互开关胶囊。
 */
class HolidaySnoozeWidget : GlanceAppWidget() {

    companion object {
        val SMALL_1X1 = DpSize(60.dp, 60.dp)
        val MEDIUM_3X2 = DpSize(180.dp, 100.dp)
        val KEY_LAST_UPDATE = longPreferencesKey("widget_last_update_timestamp")
    }

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_1X1, MEDIUM_3X2)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                // 监听 Glance State 中的时间戳更新，确保每次外部触发 update 时百分之百执行 Recomposition
                val prefs = currentState<Preferences>()
                val _timestamp = prefs[KEY_LAST_UPDATE]

                val data = buildHolidayWidgetData(context)
                val size = LocalSize.current

                // 尺寸判定：当尺寸为 1x1 规格时渲染专属紧凑布局，否则渲染 2x3（3宽2高）完整布局
                if (size.width < 140.dp || size.height < 90.dp) {
                    WidgetContent1x1(data = data)
                } else {
                    WidgetContent3x2(data = data)
                }
            }
        }
    }
}

/**
 * 小部件渲染所需的核心数据模型
 */
data class HolidayWidgetData(
    val dateTitle: String,
    val badgeTag: String,
    val statusIconRes: Int,
    val primaryStatusText: String,
    val reasonDetailText: String,
    val willSkipTodayAlarm: Boolean,
    val isAttentionNeeded: Boolean,
    val isTemporaryRestActive: Boolean,
    val btnStateText: String,
    val alarmTimeSummary: String?
)

/**
 * 构建小部件数据
 *
 * 遵循项目核心语义：
 * 1. Pixel Snooze 运行时只判断今天是否是休息日，绝不把作息逻辑移至明天。
 * 2. 优先级：临时休息 > 调休上班日 > 节假日休息日 > 用户排班规则。
 * 3. 闹钟时间信息仅用于辅助提示。
 */
fun buildHolidayWidgetData(context: Context): HolidayWidgetData {
    val koin = KoinPlatform.getKoin()
    val holidayRepository = koin.get<HolidayRepository>()
    val scheduleRepository = koin.get<RestSchedulePreferencesRepository>()
    val temporaryRestManager = koin.get<TemporaryRestManager>()

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val dateTitle = "今天 ${today.monthValue}月${today.dayOfMonth}日 ${today.dayOfWeek.toChineseText()}"

    val calendar = holidayRepository.currentCalendar()
    val rule = scheduleRepository.currentRule()
    val currentRestState = temporaryRestManager.currentState()

    // 核心语义判断：只判断今天是否是休息日
    val isTodayTemporaryRest = currentRestState.isActive(today)
    val isTodayMakeupWorkday = today in calendar.workdays
    val isTodayHoliday = today in calendar.holidays

    val isTodayRuleRest = if (rule is RestScheduleRule.Custom) {
        rule.isScheduleRestDay(today)
    } else {
        !isTodayMakeupWorkday && (isTodayHoliday || rule.isScheduleRestDay(today))
    }

    // 判断优先级：临时休息 > 调休上班日 > 节假日休息日 > 用户排班规则
    val willSkipTodayAlarm = when {
        isTodayTemporaryRest -> true
        isTodayMakeupWorkday -> false
        isTodayHoliday -> true
        else -> isTodayRuleRest
    }

    // 获取系统下一个闹钟（用于提示时间）
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val nextAlarm = alarmManager?.nextAlarmClock
    val nextAlarmDateTime = nextAlarm?.let {
        Instant.ofEpochMilli(it.triggerTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
    val nextAlarmDate = nextAlarmDateTime?.toLocalDate()

    val alarmTimeStr = if (nextAlarmDateTime != null && nextAlarmDate != null) {
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
        isTodayTemporaryRest -> {
            badgeTag = "临时休息"
            primaryStatusText = "已跳过闹钟"
            reasonDetailText = if (alarmTimeStr != null) "手动开启 · 原 $alarmTimeStr 跳过" else "临时休息生效 · 闹钟不响"
            isAttentionNeeded = true
        }
        isTodayMakeupWorkday -> {
            badgeTag = "调休上班"
            primaryStatusText = "闹钟正常响铃"
            reasonDetailText = if (alarmTimeStr != null) "周末调休 · 闹钟 $alarmTimeStr 响" else "周末调休补班 · 请注意作息"
            isAttentionNeeded = true
        }
        isTodayHoliday -> {
            badgeTag = "法定节假日"
            primaryStatusText = "自动跳过闹钟"
            reasonDetailText = if (alarmTimeStr != null) "法定假期 · 原 $alarmTimeStr 跳过" else "法定假日放假 · 闹钟不响"
            isAttentionNeeded = true
        }
        isTodayRuleRest -> {
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

    val statusIconRes = when {
        isTodayTemporaryRest -> R.drawable.ic_temporary_rest
        willSkipTodayAlarm -> R.drawable.ic_alarm_off
        else -> R.drawable.ic_alarm
    }

    val isSwitchActive = currentRestState !is TemporaryRestState.Disabled
    val btnStateText = if (isSwitchActive) "已开启" else "已关闭"

    return HolidayWidgetData(
        dateTitle = dateTitle,
        badgeTag = badgeTag,
        statusIconRes = statusIconRes,
        primaryStatusText = primaryStatusText,
        reasonDetailText = reasonDetailText,
        willSkipTodayAlarm = willSkipTodayAlarm,
        isAttentionNeeded = isAttentionNeeded,
        isTemporaryRestActive = isSwitchActive,
        btnStateText = btnStateText,
        alarmTimeSummary = alarmTimeStr
    )
}

/**
 * 1x1 紧凑精致布局
 *
 * 设计理念：
 * - 空间高度集约（约 60~85dp 方形区域），不照搬横向分栏，而是进行上下功能解耦。
 * - 卡片背景整体动态响应（临时休息开启或跳过时显眼提亮，一眼知晓作息）。
 * - 上半部分（作息展示区）：微型徽章 + 闹钟图标 + 超大粗体核心状态（“已跳过”/“不响铃”/“正常响”），点击直接打开应用。
 * - 下半部分（独立交互开关）：设计为全宽度的实体质感 M3 胶囊按钮（Pill Button），清晰展示开关自身与开启状态，点击独立切换临时休息。
 */
@Composable
private fun WidgetContent1x1(data: HolidayWidgetData) {
    val colors = GlanceTheme.colors

    val containerBg = when {
        data.isTemporaryRestActive -> colors.primaryContainer
        data.willSkipTodayAlarm -> colors.secondaryContainer
        else -> colors.widgetBackground
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(containerBg)
            .cornerRadius(20.dp)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // 上半部分：作息与闹钟状态（点击打开应用）
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                // 顶行：作息标签徽章 + 状态图标
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Box(
                        modifier = GlanceModifier
                            .background(
                                if (data.isAttentionNeeded) colors.primary
                                else colors.surfaceVariant
                            )
                            .cornerRadius(4.dp)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = data.badgeTag,
                            style = TextStyle(
                                color = if (data.isAttentionNeeded) colors.onPrimary else colors.onSurfaceVariant,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(4.dp))

                    Image(
                        provider = ImageProvider(data.statusIconRes),
                        contentDescription = null,
                        modifier = GlanceModifier.size(13.dp),
                        colorFilter = ColorFilter.tint(
                            if (data.isAttentionNeeded) colors.primary else colors.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(3.dp))

                // 核心状态大字
                val shortStatusText = when {
                    data.isTemporaryRestActive -> "已跳过"
                    data.willSkipTodayAlarm -> "不响铃"
                    else -> "正常响"
                }

                Text(
                    text = shortStatusText,
                    style = TextStyle(
                        color = if (data.isAttentionNeeded) colors.onPrimaryContainer else colors.onSurface,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(3.dp))

            // 下半部分：开关自身（实体质感 M3 交互胶囊按钮）
            val btnBg = if (data.isTemporaryRestActive) colors.primary else colors.surfaceVariant
            val btnTextColor = if (data.isTemporaryRestActive) colors.onPrimary else colors.onSurfaceVariant

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(btnBg)
                    .cornerRadius(13.dp)
                    .clickable(actionRunCallback<ToggleTemporaryRestAction>()),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_temporary_rest),
                        contentDescription = null,
                        modifier = GlanceModifier.size(12.dp),
                        colorFilter = ColorFilter.tint(btnTextColor)
                    )
                    Spacer(modifier = GlanceModifier.width(3.dp))
                    Text(
                        text = if (data.isTemporaryRestActive) "休 · 开" else "临时休息",
                        style = TextStyle(
                            color = btnTextColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

/**
 * 2x3（宽3格高2格）完整卡片布局
 *
 * 经典左右分栏：
 * - 左侧：日期与星期、状态标签、醒目的核心闹钟结论、详细解释与时间信息。
 * - 右侧：独立的实体质感 M3 大开关按钮，包含图标底座、文本与药丸胶囊状态。
 */
@Composable
private fun WidgetContent3x2(data: HolidayWidgetData) {
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
            // 左侧：以“今天闹钟会不会响”为核心的完整信息展示
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
                        text = data.dateTitle,
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
                                if (data.isAttentionNeeded) colors.primaryContainer
                                else colors.secondaryContainer
                            )
                            .cornerRadius(6.dp)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = data.badgeTag,
                            style = TextStyle(
                                color = if (data.isAttentionNeeded) colors.onPrimaryContainer
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
                    data.isAttentionNeeded && data.willSkipTodayAlarm -> colors.primary
                    data.isAttentionNeeded && !data.willSkipTodayAlarm -> colors.onSurface
                    else -> colors.onSurfaceVariant
                }

                Row(
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(data.statusIconRes),
                        contentDescription = null,
                        modifier = GlanceModifier.size(24.dp),
                        colorFilter = ColorFilter.tint(statusTextColor)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = data.primaryStatusText,
                        style = TextStyle(
                            color = statusTextColor,
                            fontSize = 24.sp,
                            fontWeight = if (data.isAttentionNeeded) FontWeight.Bold else FontWeight.Medium
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
                        text = data.reasonDetailText,
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
            val btnBg = if (data.isTemporaryRestActive) colors.primary else colors.surfaceVariant
            val btnIconCircleBg = if (data.isTemporaryRestActive) colors.primaryContainer else colors.secondaryContainer
            val btnIconTint = if (data.isTemporaryRestActive) colors.onPrimaryContainer else colors.onSecondaryContainer
            val btnTitleColor = if (data.isTemporaryRestActive) colors.onPrimary else colors.onSurfaceVariant
            val pillBg = if (data.isTemporaryRestActive) colors.onPrimary else colors.primaryContainer
            val pillTextColor = if (data.isTemporaryRestActive) colors.primary else colors.onPrimaryContainer

            val buttonModifier = GlanceModifier
                .width(80.dp)
                .fillMaxHeight()
                .background(btnBg)
                .cornerRadius(18.dp)
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .clickable(actionRunCallback<ToggleTemporaryRestAction>())

            Box(
                modifier = buttonModifier,
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
                            text = data.btnStateText,
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

/**
 * 临时休息开关点击事件处理
 */
class ToggleTemporaryRestAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val koin = KoinPlatform.getKoin()
        val temporaryRestManager = koin.get<TemporaryRestManager>()

        // 1. 如果已开启，点击直接关闭
        if (temporaryRestManager.currentState() !is TemporaryRestState.Disabled) {
            temporaryRestManager.disable()
        } else {
            // 2. 如果未开启，允许用户直接开启；如果明早有闹钟则开启至明天，否则开启今天
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val nextAlarm = alarmManager?.nextAlarmClock
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val alarmDate = nextAlarm?.let {
                Instant.ofEpochMilli(it.triggerTime).atZone(ZoneId.systemDefault()).toLocalDate()
            }

            if (alarmDate == tomorrow) {
                temporaryRestManager.enableUntil(tomorrow)
            } else {
                temporaryRestManager.enableToday()
            }
        }

        // 3. 写入 Glance State 时间戳，确保触发 Recomposition，并立即更新小部件
        val now = System.currentTimeMillis()
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(HolidaySnoozeWidget::class.java)
            glanceIds.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[HolidaySnoozeWidget.KEY_LAST_UPDATE] = now
                    }
                }
                HolidaySnoozeWidget().update(context, id)
            }
        } catch (_: Exception) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[HolidaySnoozeWidget.KEY_LAST_UPDATE] = now
                }
            }
            HolidaySnoozeWidget().update(context, glanceId)
        }
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
