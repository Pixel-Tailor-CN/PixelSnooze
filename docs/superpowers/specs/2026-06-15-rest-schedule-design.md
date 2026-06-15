# Pixel Snooze 休息日规则策略层设计

## 背景

Pixel Snooze 当前只根据本地调休日历中的 `holidays` 集合判断今天是否休息。这个模型适合默认双休用户，但不能覆盖单休、大小周、上 x 休 y 或完全自定义排班。

本设计保持项目现有约束：

- 运行时只判断今天是否休息。
- 节假日 JSON 仍只包含 `year` 和 `holidays`。
- 不向节假日 JSON 引入 `workdays`、调休补班日或默认周末规则。
- 通知监听热路径不做网络 I/O，也不做重型解析。
- 所有用户排班配置保存在本地，不引入数据库或云端同步。

## 目标

新增一个独立的休息日判定策略层，让应用支持以下模式：

1. 节假日数据 + 双休。
2. 节假日数据 + 单休。
3. 节假日数据 + 大小周。
4. 节假日数据 + 上 x 休 y。
5. 完全自定义上班和休息日期。

前三种和上 x 休 y 都叠加节假日数据：只要今天在 `holidays` 中，就视为休息日。完全自定义模式不叠加节假日数据，只使用用户手动配置。

## 非目标

- 不改变 `holiday.json` 数据格式。
- 不新增工作日数据源。
- 不计算明天或任意闹钟触发日期，只判断今天。
- 不在通知监听回调中更新节假日数据。
- 不引入单元测试作为该功能的强制要求。

## 推荐架构

采用独立策略层，不扩展 `HolidayRepository` 的职责。

新增包：

```text
schedule/
  RestDayRepository.kt
  RestSchedulePreferencesRepository.kt
  RestScheduleRule.kt
```

职责划分：

- `HolidayRepository`：继续只负责加载和刷新节假日 `holidays` 数据。
- `RestSchedulePreferencesRepository`：负责读写用户选择的休息日规则。
- `RestDayRepository`：组合节假日数据和用户规则，提供 `isRestDay()`。
- `PixelSnoozeNotificationListenerService`：从调用 `holidayRepository.isHoliday()` 改为调用 `restDayRepository.isRestDay()`。

通知监听链路保持为：

```text
目标包名过滤
读取通知文本
关键词匹配
判断今天是否休息
查找跳过动作
发送 PendingIntent
记录结果
```

## 模式模型

建议定义：

```kotlin
enum class RestScheduleMode {
    HOLIDAY_AND_WEEKEND,
    HOLIDAY_AND_SINGLE_DAY_OFF,
    HOLIDAY_AND_ALTERNATING_WEEK,
    HOLIDAY_AND_CYCLE,
    CUSTOM
}
```

规则配置建议定义为密封类或数据类组合：

```kotlin
sealed interface RestScheduleRule {
    data object HolidayAndWeekend : RestScheduleRule

    data class HolidayAndSingleDayOff(
        val restDayOfWeek: DayOfWeek
    ) : RestScheduleRule

    data class HolidayAndAlternatingWeek(
        val largeWeekRestDays: Set<DayOfWeek>,
        val smallWeekRestDays: Set<DayOfWeek>,
        val anchorWeekStartDate: LocalDate,
        val anchorWeekType: AlternatingWeekType
    ) : RestScheduleRule

    data class HolidayAndCycle(
        val workDays: Int,
        val restDays: Int,
        val anchorDate: LocalDate,
        val anchorDayIndex: Int
    ) : RestScheduleRule

    data class Custom(
        val monthlySchedules: Map<YearMonth, CustomMonthlySchedule>
    ) : RestScheduleRule
}
```

其中 `AlternatingWeekType` 表示锚点周是大周还是小周：

```kotlin
enum class AlternatingWeekType {
    LARGE,
    SMALL
}
```

## 统一判断规则

`RestDayRepository.isRestDay()` 只判断今天：

```text
date = LocalDate.now()
rule = preferences.currentRule()

如果 rule 是 CUSTOM:
  返回 date 是否在用户自定义休息日集合中

否则:
  如果 date 在 holidayRepository.currentCalendar().holidays 中，返回 true
  否则返回 date 是否命中当前 rule 的本地排班规则
```

日期不在 `holidays` 中时，不代表工作日，只代表节假日数据没有把它标为休息日。最终是否休息由当前模式继续判断。

## 双休模式

双休模式不需要额外配置。

规则：

```text
今天在 holidays 中，休息
否则今天是周六或周日，休息
否则不休息
```

这是当前行为的兼容模式，也是新功能的默认模式。

## 单休模式

单休模式需要用户指定每周哪一天休息。

配置：

- `restDayOfWeek`：周一到周日之一。

规则：

```text
今天在 holidays 中，休息
否则今天的 DayOfWeek 等于 restDayOfWeek，休息
否则不休息
```

## 大小周模式

大小周模式需要用户指定大周休息日、小周休息日，以及本周属于大周还是小周。

配置：

- `largeWeekRestDays`：大周休息日集合，例如仅周日。
- `smallWeekRestDays`：小周休息日集合，例如周六、周日。
- `anchorWeekStartDate`：保存配置时所在周的周一。
- `anchorWeekType`：保存配置时所在周是大周还是小周。

UI 表达：

```text
大周休息日：用户选择一个或多个星期几
小周休息日：用户选择一个或多个星期几
本周类型：大周 / 小周
```

保存时把“本周类型”转换为锚点：

```text
anchorWeekStartDate = 今天所在周的周一
anchorWeekType = 用户选择的大周或小周
```

计算时：

```text
weeksBetween = floor((date所在周周一 - anchorWeekStartDate) / 7)
如果 weeksBetween 为偶数，当前周类型 = anchorWeekType
如果 weeksBetween 为奇数，当前周类型 = anchorWeekType 的另一种

今天在 holidays 中，休息
否则今天的 DayOfWeek 在当前周类型的休息日集合中，休息
否则不休息
```

## 上 x 休 y 模式

上 x 休 y 需要用户输入：

- 上班天数 `workDays`。
- 休息天数 `restDays`。
- 今天是周期第几天 `todayIndex`。

保存时记录：

- `workDays`
- `restDays`
- `anchorDate = LocalDate.now()`
- `anchorDayIndex = todayIndex`

校验：

```text
workDays >= 1
restDays >= 1
1 <= anchorDayIndex <= workDays + restDays
```

计算：

```text
cycleLength = workDays + restDays
daysBetween = date - anchorDate
currentIndex = floorMod(anchorDayIndex - 1 + daysBetween, cycleLength) + 1

如果 currentIndex <= workDays，不休息
否则休息
```

完整规则：

```text
今天在 holidays 中，休息
否则今天命中周期休息段，休息
否则不休息
```

## 完全自定义模式

完全自定义模式不叠加节假日数据。它用于没有稳定规律、无法计算的排班。

建议按月份保存：

```kotlin
data class CustomMonthlySchedule(
    val workDates: Set<LocalDate>,
    val restDates: Set<LocalDate>
)
```

判断时只依赖 `restDates`：

```text
今天在用户自定义 restDates 中，休息
否则不休息
```

`workDates` 用于 UI 展示和避免用户误解某天是否已配置。未配置日期默认视为不休息，不自动跳过。

## 本地持久化

继续使用 `SharedPreferences`。建议将规则配置序列化为 JSON 字符串，避免新增多个互相依赖的 key。

主配置示例：

```json
{
  "mode": "HOLIDAY_AND_ALTERNATING_WEEK",
  "alternatingWeek": {
    "largeWeekRestDays": ["SUNDAY"],
    "smallWeekRestDays": ["SATURDAY", "SUNDAY"],
    "anchorWeekStartDate": "2026-06-15",
    "anchorWeekType": "LARGE"
  }
}
```

上 x 休 y 示例：

```json
{
  "mode": "HOLIDAY_AND_CYCLE",
  "cycle": {
    "workDays": 3,
    "restDays": 2,
    "anchorDate": "2026-06-15",
    "anchorDayIndex": 2
  }
}
```

自定义月份建议单独按月保存，key 形如：

```text
custom_schedule_2026-06
```

内容：

```json
{
  "month": "2026-06",
  "workDates": ["2026-06-03"],
  "restDates": ["2026-06-01", "2026-06-02"]
}
```

读取失败时使用默认规则：节假日数据 + 双休。这样配置损坏不会导致应用异常退出，也不会扩大误跳过风险。

## UI 设计

首页建议拆出两个状态行：

- `休息日规则`：展示当前模式摘要，点击进入规则配置。
- `节假日数据`：展示 `holiday.json` 年份范围和休息日数量，继续负责查看和手动更新。

规则配置入口：

- 双休：选择后直接保存。
- 单休：选择每周休息日。
- 大小周：选择大周休息日、小周休息日、本周类型。
- 上 x 休 y：输入上班天数、休息天数、今天是第几天。
- 完全自定义：进入月历视图，逐日标记上班或休息。

UI 文案应避免把 `holiday.json` 描述成完整工作日历。它只是节假日休息日数据。

## 历史记录和日志

当前历史记录中“今天不是休息日”的原因可以保留。后续如果要提升可诊断性，可以改成：

```text
今天未命中休息日规则
```

Logcat 仍使用英文，例如：

```text
Alarm keyword matched but today is not a rest day
```

## 文档更新

实现该功能时需要同步更新 README 和隐私政策：

- 应用运行时仍然只判断今天是否休息。
- `holiday.json` 仍只包含 `year` 和 `holidays`。
- 用户排班规则保存在本地。
- 前四种规则叠加节假日数据。
- 完全自定义模式不叠加节假日数据。
- 未配置的自定义日期默认不休息，不自动跳过。

## 验证建议

默认验证方式仍为 Android 构建、lint 和人工检查：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

人工检查重点：

- 双休模式保持默认行为。
- 单休能按用户选择星期几判断。
- 大小周能按锚点周交替切换。
- 上 x 休 y 能跨月、跨年正确循环。
- 完全自定义不叠加节假日数据。
- 通知监听热路径没有网络 I/O。
