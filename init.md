# 项目初始化文档 (init.md)

## 1. 项目概述

* **应用名称**: Pixel Snooze
* **应用包名**: `vip.mystery0.pixel.snooze`
* **所属系列**: 原点系列 (Origin Series)
* **核心定位**: 一款纯本地 (Local-first)、极低侵入性的 Android 辅助工具。旨在不修改原生时钟应用 (
  Google DeskClock) 的前提下，通过系统通知监听机制，实现符合国内“法定节假日和调休”逻辑的自动跳过闹钟功能。
* **设计哲学**: 将原生时钟的“Snooze (贪睡)”概念宏观化，一次拦截，贪睡整个节假日；坚守本地优先与隐私保护，核心执行路径完全不依赖网络。

## 2. 核心技术方案 (NotificationListenerService)

本项目严禁使用 Root、Xposed 或反编译手段，必须基于纯粹的 Android API `NotificationListenerService` (
NLS) 实现。

### 2.1 监听与拦截链路

1. **目标包名白名单**: 仅处理包名为 `com.google.android.deskclock` (Pixel/Google 全家桶) 和
   `com.android.deskclock` (AOSP/类原生) 的通知。
2. **通知识别**: Google 时钟会在闹钟响铃前（通常提前 2 小时）发送“即将响铃 (Upcoming alarm)”通知。
3. **标签匹配**: 读取通知载荷 `Notification.EXTRA_TITLE` 或 `Notification.EXTRA_TEXT`
   ，精准匹配用户设定的关键词（如：**“节假日闹钟”**）。
4. **节假日判定**: 命中关键词后，调用本地静态缓存的节假日历数据（需实现毫秒级判断明天是否为休息日）。
5. **执行静默关闭**: 若判断结果为“休息”，遍历通知的 `actions`，匹配标题包含多语言字典
   `["dismiss", "在此关闭", "关闭", "取消"]` 的操作，提取其 `PendingIntent` 并直接执行 `.send()`
   ，完成闹钟实例的后台静默销毁。

## 3. 性能与耗电控制（极客级要求）

鉴于设备可能同时运行高频刷新状态栏的应用，NLS 回调可能被频繁触发。必须在代码层面做到极致防御：

* **极速熔断 (Fast-Fail)**: 在 `onNotificationPosted` 回调的首行进行包名比对。非目标包名直接 `return`
  ，严禁在包名过滤前调用 `sbn.notification.extras`，避免不必要的反序列化开销、内存抖动和 GC 掉帧。
* **纯本地 O(1) 判定**: 拦截逻辑通常发生于凌晨 Doze（深度睡眠）模式下，`isHolidayTomorrow()`
  方法必须是纯本地、且查询时间复杂度为 O(1) 的内存读取。
* **严禁网络 I/O 阻塞**: NLS 的回调执行链路上**绝对不允许**存在任何形式的网络请求（Network
  I/O），杜绝强制持有 Wakelock 导致系统耗电雪崩。

## 4. 技术栈与架构设计

* **开发语言**: Kotlin
* **UI 框架与设计语言**: Jetpack Compose 结合 **Material You (Material Design 3)**，支持动态取色，完美契合原生
  Pixel 的视觉体验与原点系列的设计规范。
* **依赖注入方案**: **Koin** (轻量级、Kotlin 原生友好，避免 Hilt/Dagger
  在小型辅助工具中产生过度重型化与额外的编译耗时)。
* **数据来源策略 (Local-First)**:
    * 核心判定依赖本地缓存的年度放假安排数据（JSON / 数据库）。
    * 提供基础 UI，支持直接导入静态数据或简单的手动补班/休息日微调配置。
    * 若包含联网更新功能，必须使用 `WorkManager` 在设备满足 `充电中` 且 `连接 Wi-Fi` 等限制条件下进行低频后台同步。

## 5. 编码规范约束

* 遵循 SOLID 原则，通过 Koin 将“通知解析逻辑”、“本地日历判定模块”、“UI 表现层”和“持久化存储层”严格模块化并解耦。
* 代码需具备良好的健壮性，使用 `try-catch` 妥善包裹 `PendingIntent.send()` 等外部调用，防止因系统抛出
  `CanceledException` 导致后台服务崩溃。
* 输出规范且清晰的 Logcat 日志记录拦截行为，便于后续调试与用户反馈追踪。