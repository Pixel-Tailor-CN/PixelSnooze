# Pixel Snooze 本地 JSON 首版 Implementation Plan

> 当前文档记录首版本地 JSON 方案的最终实现约束。旧版计划中关于“判断明天”、`workdays` 和单元测试的内容已不再适用。

## 目标

构建 Pixel Snooze 首版可验证闭环：内置休息日 JSON、通知监听拦截、O(1) 本地判定、基础权限 UI。

## 当前约束

- 运行时只判断今天是否是休息日。
- 节假日数据只需要 `holidays` 休息日集合。
- 不需要 `workdays`、调休补班日或工作日数据。
- 项目不要求单元测试，也不保留单元测试计划。
- 验证以 Android 构建、lint 和必要的人工检查为准。

## 架构

- `PixelSnoozeApplication`：启动 Koin，注册依赖。
- `PixelSnoozeNotificationListenerService`：Android 通知监听入口，只负责快速熔断和编排。
- `AlarmNotificationParser`：解析通知标题和正文，判断是否命中用户关键词。
- `AlarmDismissActionFinder`：从通知 actions 中寻找关闭动作。
- `HolidayRepository`：加载内置日历，并通过 `isHoliday()` 判断今天是否是休息日。
- `HolidayDataSource`：节假日数据源接口。
- `AssetHolidayDataSource`：从 `assets/holiday_2026.json` 读取内置数据。
- `UserPreferencesRepository`：保存和读取关键词，使用 `SharedPreferences`。
- `HomeScreen`：基础 Material 3 UI，展示通知监听权限、关键词和内置日历状态。

## 数据格式

内置 JSON 位于 `app/src/main/assets/holiday_2026.json`，格式如下：

```json
{
  "year": 2026,
  "holidays": ["2026-01-01"]
}
```

`holidays` 表示休息日。今天在 `holidays` 中时视为休息日，否则视为非休息日。

## 通知处理流程

1. 先检查 `StatusBarNotification.packageName`，非目标包名直接返回。
2. 包名命中后读取 `sbn.notification` 和 `extras`。
3. 解析 `Notification.EXTRA_TITLE` 与 `Notification.EXTRA_TEXT`。
4. 文本命中关键词后调用 `HolidayRepository.isHoliday()`。
5. 今天不是休息日则返回。
6. 今天是休息日则查找关闭动作。
7. 找到后执行 `PendingIntent.send()`。
8. 所有外部调用异常只记录英文 Logcat 日志，服务不崩溃。

## 验证

推荐验证命令：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

Release 包需要验证时运行：

```powershell
.\gradlew.bat :app:assembleRelease
```

## 自审结果

- 文档语义已统一为“判断今天是否是休息日”。
- 数据格式已统一为仅使用 `holidays`。
- 文档不再要求 `workdays` 或工作日数据。
- 文档不再要求单元测试。
