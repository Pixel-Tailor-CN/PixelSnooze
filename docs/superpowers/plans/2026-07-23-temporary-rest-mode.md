# 临时休息模式实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Pixel Snooze 增加首页临时休息状态卡、快捷设置磁贴、开启状态通知和应用图标长按快捷方式，并确保关闭或到期后无损恢复既有休息日规则。

**Architecture:** 使用独立的 `TemporaryRestPreferencesRepository` 保存临时覆盖状态，`TemporaryRestManager` 统一处理状态变更以及通知和磁贴同步，`RestDayRepository` 只在判断今天时优先读取该覆盖层。首页、磁贴、通知操作和快捷方式均调用同一个 Manager，不修改原有排班配置。

**Tech Stack:** Kotlin、Android SDK 31–37、Jetpack Compose Material 3、SharedPreferences、TileService、NotificationManager、Koin。

## Global Constraints

- 与用户沟通、代码注释和项目文档使用中文，运行日志使用英文。
- Pixel Snooze 运行时只判断今天是否是休息日。
- 节假日数据仍然只使用 `holidays` 集合，不增加 `workdays`。
- 不修改或备份既有休息日规则，不操作系统闹钟。
- 不引入数据库、云同步、前台服务、精确闹钟、Root、Xposed 或反编译能力。
- 通知监听热路径只允许轻量本地读取。
- 依照仓库要求不新增单元测试；使用分阶段编译、最终构建、lint 和人工检查验证。

---

### Task 1: 临时休息状态与持久化

**Files:**
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/temporaryrest/TemporaryRestState.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/temporaryrest/TemporaryRestPreferencesRepository.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestDayRepository.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/di/AppModule.kt`

**Interfaces:**
- Produces: `TemporaryRestState`, `TemporaryRestState.isActive(LocalDate): Boolean`
- Produces: `TemporaryRestPreferencesRepository.currentState()` 与 `updateState(state)`
- Consumes: `RestDayRepository.isRestDay()` 在现有规则前读取临时状态

- [ ] **Step 1: 定义状态模型**

创建 `Disabled`、`UntilDate(endDate)`、`UntilDisabled`，实现仅依据传入“今天”的 `isActive`，并提供首页、磁贴和通知需要的中文摘要。

- [ ] **Step 2: 实现 SharedPreferences 持久化**

使用独立文件 `pixel_snooze_temporary_rest_preferences` 保存模式和 ISO 日期。无效模式、缺失日期或日期解析失败统一返回 `Disabled`。

- [ ] **Step 3: 接入今天是否休息的判断**

在 `RestDayRepository.isRestDay()` 最前面执行：

```kotlin
val date = todayProvider()
if (temporaryRestPreferencesRepository.currentState().isActive(date)) {
    return true
}
```

随后保留现有自定义排班、节假日和本地规则逻辑。

- [ ] **Step 4: 注册依赖并编译**

在 Koin 中注册持久化仓库并传给 `RestDayRepository`。

Run: `.\gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`

### Task 2: 状态通知与统一 Manager

**Files:**
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/temporaryrest/TemporaryRestStatusNotification.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/temporaryrest/TemporaryRestManager.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/temporaryrest/TemporaryRestActionReceiver.kt`
- Create: `app/src/main/res/drawable/ic_temporary_rest.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/di/AppModule.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/PixelSnoozeApplication.kt`

**Interfaces:**
- Consumes: `TemporaryRestPreferencesRepository`
- Produces: `TemporaryRestManager.currentState()`, `enableToday()`, `enableUntil(date)`, `enableUntilDisabled()`, `disable()` 与 `refreshSurfaces()`
- Produces: 通知操作广播 `ACTION_DISABLE_TEMPORARY_REST`

- [ ] **Step 1: 创建静默状态通知**

创建低打扰渠道 `temporary_rest_status`。模式有效且已有 `POST_NOTIFICATIONS` 权限时发布常驻通知，包含“关闭”和“修改时长”；有结束日期时使用 `setTimeoutAfter()` 在结束日期次日零点自动消失。

- [ ] **Step 2: 实现统一状态变更**

Manager 写入状态后调用通知同步，并通过 `TileService.requestListeningState()` 请求刷新磁贴。读取状态时把已过期值清理成 `Disabled`；`enableToday()` 不缩短已有的更长状态。

- [ ] **Step 3: 实现通知关闭操作**

不可导出的 `TemporaryRestActionReceiver` 只接受应用内部关闭 Action，并调用 Manager。

- [ ] **Step 4: 初始化状态表面**

应用创建 Koin 后调用 `TemporaryRestManager.refreshSurfaces()`，使进程重启后恢复状态通知并校正过期数据。

- [ ] **Step 5: 更新 Manifest 并编译**

声明通知权限和 Receiver。

Run: `.\gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`

### Task 3: 快捷设置磁贴

**Files:**
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/temporaryrest/TemporaryRestTileService.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `TemporaryRestManager`
- Produces: 标签为“临时休息”的可切换系统磁贴

- [ ] **Step 1: 实现磁贴状态展示**

`onStartListening()` 读取 Manager。有效时使用 `STATE_ACTIVE` 和日期摘要，无效时使用 `STATE_INACTIVE`。

- [ ] **Step 2: 实现磁贴点击**

未开启时启用仅今天，已开启时关闭。安全锁屏下只对“开启”操作调用 `unlockAndRun()`，关闭操作直接执行。

- [ ] **Step 3: 声明磁贴**

使用 `BIND_QUICK_SETTINGS_TILE`，添加 `ACTIVE_TILE` 和 `TOGGLEABLE_TILE` 元数据；MainActivity 接收 `ACTION_QS_TILE_PREFERENCES` 作为长按设置入口。

- [ ] **Step 4: 编译**

Run: `.\gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`

### Task 4: 首页状态卡和持续时间对话框

**Files:**
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/TemporaryRestCard.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/MainActivity.kt`

**Interfaces:**
- Consumes: `TemporaryRestManager`
- Produces: 首页开关、持续时间设置、日期选择、添加磁贴入口和通知权限请求

- [ ] **Step 1: 创建状态卡**

卡片展示开关和当前摘要。开关开启默认仅今天，关闭立即生效；点击正文打开持续时间对话框。

- [ ] **Step 2: 创建持续时间对话框**

提供“仅今天”“选择结束日期”“直到手动关闭”和“关闭”。日期选择器拒绝早于今天的日期。

- [ ] **Step 3: 添加磁贴引导**

Android 13 及以上调用 `StatusBarManager.requestAddTileService()`；Android 12 通过 Toast 提示在快捷设置编辑页手动添加。

- [ ] **Step 4: 请求通知权限**

Android 13 及以上首次从首页开启且尚未授权时，请求 `POST_NOTIFICATIONS`。拒绝时不撤销临时休息状态。

- [ ] **Step 5: 接入 Activity**

MainActivity 注入 Manager，并允许磁贴长按或通知“修改时长”Action 直接打开持续时间对话框。

- [ ] **Step 6: 编译**

Run: `.\gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`

### Task 5: 应用图标长按快捷方式

**Files:**
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/temporaryrest/TemporaryRestActions.kt`
- Create: `app/src/main/res/xml/shortcuts.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/MainActivity.kt`

**Interfaces:**
- Consumes: `TemporaryRestManager`
- Produces: “临时休息一天”“关闭临时休息”“管理临时休息”三个快捷方式

- [ ] **Step 1: 声明显式快捷方式**

三个静态 shortcut 都明确指向 MainActivity，不导出额外的广播或 Service。

- [ ] **Step 2: 处理快捷方式 Action**

MainActivity 在 `onCreate()` 和 `onNewIntent()` 中处理 Action。直接操作后仍显示首页最新状态；管理 Action 打开持续时间对话框。

- [ ] **Step 3: 编译**

Run: `.\gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`

### Task 6: 文档、完整验证与提交

**Files:**
- Modify: `README.md`
- Modify: `PRIVACY.md`

- [ ] **Step 1: 更新功能与隐私说明**

说明临时休息只覆盖今天、原规则不变、状态保存在本地、状态通知权限可拒绝，以及磁贴和快捷方式入口。

- [ ] **Step 2: 检查资源和 Manifest**

Run: `.\gradlew.bat :app:processDebugResources`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 完整构建**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Lint**

Run: `.\gradlew.bat :app:lintDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 人工静态检查**

确认所有状态变更都经过 Manager、通知热路径没有新增网络 I/O、日志为英文、文档未引入 `workdays` 或未来日期判断描述。

- [ ] **Step 6: 审阅差异并提交**

Run: `git diff --check`

Expected: 无输出。

提交实现与文档，保留设计文档的既有独立提交。
