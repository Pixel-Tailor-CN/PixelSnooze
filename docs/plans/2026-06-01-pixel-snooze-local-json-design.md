# Pixel Snooze 首版本地 JSON 设计

## 背景

Pixel Snooze 首版用于验证基于 `NotificationListenerService` 静默关闭 Google/AOSP 时钟“即将响铃”通知的可行性。首版必须完全本地运行，不使用数据库，不在通知监听回调链路中执行任何网络 I/O。

## 范围

首版实现以下能力：

- 监听 `com.google.android.deskclock` 和 `com.android.deskclock` 的通知。
- 在包名命中后读取通知标题和正文，匹配用户关键词。
- 从内置 JSON 加载 2026 年节假日数据到内存集合。
- 使用 O(1) 内存查询判断明天是否为休息日。
- 如果命中休息日，查找通知操作中的关闭动作并执行 `PendingIntent.send()`。
- 提供基础 Compose UI，用于查看通知监听权限状态、默认关键词和内置日历状态，并跳转系统通知监听权限页。

首版不实现以下能力：

- 不使用 Room、SQLite 或其他数据库。
- 不实现云端下载。
- 不实现 WorkManager 同步任务。
- 不修改系统时钟应用或闹钟配置。
- 不使用 Root、Xposed 或反编译能力。

## 架构

代码按职责拆分为以下模块：

- `PixelSnoozeApplication`：启动 Koin，注册依赖。
- `PixelSnoozeNotificationListenerService`：Android NLS 入口，只负责快速熔断和编排。
- `AlarmNotificationParser`：解析通知文本并判断是否命中关键词。
- `AlarmDismissActionFinder`：从通知 actions 中寻找关闭动作。
- `HolidayRepository`：对外提供 `isHolidayTomorrow()`。
- `HolidayDataSource`：节假日数据源接口。
- `AssetHolidayDataSource`：首版从 `assets/holiday_2026.json` 读取内置数据。
- `UserPreferencesRepository`：首版保存和读取关键词，使用 `SharedPreferences`。
- `HomeScreen`：基础 Material 3 UI。

后续云端版本只需要新增远程数据源和同步流程，例如 `RemoteHolidayDataSource` 与受限条件下运行的 WorkManager，同步结果可以写入文件缓存。NLS 仍只依赖 `HolidayRepository` 的内存快照，不直接访问网络。

## 数据格式

内置 JSON 位于 `app/src/main/assets/holiday_2026.json`，格式如下：

```json
{
  "year": 2026,
  "holidays": ["2026-01-01"],
  "workdays": ["2026-02-14"]
}
```

- `holidays` 表示休息日，包括法定节假日和周末调休休息日。
- `workdays` 表示调休补班日，用于从默认周末休息规则中排除。
- 判断逻辑为：如果日期在 `holidays` 中则为休息日；如果在 `workdays` 中则为工作日；否则周六、周日为休息日，周一到周五为工作日。

## 通知处理流程

`onNotificationPosted` 的第一步只读取 `StatusBarNotification.packageName`：

1. 非目标包名直接返回。
2. 包名命中后再读取 `sbn.notification` 和 `extras`。
3. 解析 `Notification.EXTRA_TITLE` 与 `Notification.EXTRA_TEXT`。
4. 文本命中关键词后调用 `HolidayRepository.isHolidayTomorrow()`。
5. 明天不是休息日则返回。
6. 明天是休息日则查找关闭动作。
7. 找到后执行 `PendingIntent.send()`。
8. 所有外部调用异常只记录英文 Logcat 日志，服务不崩溃。

## 性能约束

- 包名过滤前不读取 `notification.extras`。
- `isHolidayTomorrow()` 只访问内存集合和当前日期。
- NLS 回调中不执行网络请求。
- 首次加载 JSON 发生在仓库初始化路径中，NLS 查询路径不解析 JSON。

## UI

首版 UI 保持克制，只提供验证所需入口：

- 显示应用名称和本地优先状态。
- 显示通知监听权限是否已启用。
- 提供跳转系统通知监听设置的按钮。
- 显示当前关键词，默认值为 `节假日闹钟`。
- 显示内置日历年份与休息日数量。

UI 使用 Jetpack Compose 与 Material 3，继续使用动态取色。

## 测试

首版至少覆盖以下单元测试：

- 目标包名白名单判断。
- 通知文本关键词匹配。
- 关闭动作标题匹配。
- `HolidayRepository` 对 `holidays`、`workdays` 和默认周末规则的判断。

## 日志

日志内容使用英文，避免本地化文本影响排查：

- 包名命中但通知解析失败。
- 关键词命中但明天不是休息日。
- 成功发送 dismiss action。
- `PendingIntent.send()` 抛出 `CanceledException` 或其他异常。

## 可行性结论

该方案可以验证核心链路是否可行，同时保持首版实现轻量。内置 JSON 不会进入 NLS 热路径的解析阶段，后续改为 GitHub 云端下载时也不需要改动通知拦截模块，只需替换或扩展数据源与缓存加载流程。
