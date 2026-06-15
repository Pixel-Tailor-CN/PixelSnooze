# Pixel Snooze

Pixel Snooze 是一款本地优先、低侵入的 Android 辅助工具，用于在休息日自动跳过目标时钟应用的闹钟通知。

应用不会创建、编辑或删除系统闹钟，也不修改系统时钟应用。它通过 Android
`NotificationListenerService` 监听目标时钟应用通知，在通知内容命中用户配置的关键词、且“今天”命中当前休息日规则时，尝试触发通知里已有的跳过或关闭操作。

## 功能特性

- 仅判断今天是否为休息日，不判断明天或其他日期。
- 支持多种本地休息日规则：节假日 + 双休、节假日 + 单休、节假日 + 大小周、节假日 + 上 x 休 y、完全自定义。
- 节假日数据仍只来自 `holiday.json` 中的 `holidays` 集合。
- 完全自定义模式只使用用户手动标记的休息日，不叠加节假日数据。
- 仅处理 Google/AOSP 时钟应用通知：
    - `com.google.android.deskclock`
    - `com.android.deskclock`
- 支持自定义闹钟通知关键词。
- 支持自定义跳过按钮文本匹配规则。
- 支持休息日规则配置、调休日历查看和手动更新。
- 应用内置一份 `assets/holiday.json`，首次运行或本地无缓存时使用内置数据。
- 用户手动更新调休日历后，应用会优先读取本地缓存数据。
- 启动时不会在后台自动拉取云端调休日历。
- 主页面展示自动跳过闹钟记录和最近的闹钟通知处理记录。
- 主页面右上角提供 GitHub 仓库入口和设置入口。
- 设置页面使用 `zhanghai/ComposePreference` 实现，目前包含“关于”信息。

## 使用方式

1. 在系统时钟应用中创建需要跳过的闹钟。
2. 让目标闹钟通知的标题或正文包含 Pixel Snooze 中配置的关键词。
3. 打开 Pixel Snooze。
4. 点击“打开通知监听设置”，在系统设置中授予 Pixel Snooze 通知监听权限。
5. 根据需要点击主页面的“关键词”“跳过按钮文本”“休息日规则”或“调休日历”进行配置。

当目标时钟应用发布通知时，Pixel Snooze 会执行以下流程：

1. 检查通知包名，非目标时钟应用直接忽略。
2. 读取目标通知的标题和正文。
3. 判断标题或正文是否包含用户配置的关键词。
4. 命中关键词后，根据当前休息日规则判断今天是否休息。
5. 如果今天是休息日，从通知操作中查找匹配的跳过或关闭按钮。
6. 找到可用操作后发送对应 `PendingIntent`。
7. 将自动跳过或忽略通知的结果保存到本地执行记录。

## 调休日历

应用实际读取的调休日历数据来源按优先级排列如下：

1. 本地缓存：用户在应用中手动更新后保存到应用私有目录的 `holiday.json`。
2. 内置数据：`app/src/main/assets/holiday.json`。

云端调休日历地址：

```text
https://raw.githubusercontent.com/Pixel-Tailor-CN/PixelSnooze/refs/heads/main/json/holiday.json
```

应用只有在用户手动点击调休日历弹窗中的“更新”按钮时才会请求该地址，不会在启动时自动后台拉取。

仓库中的 `json/holiday.json` 由 GitHub Actions 更新。workflow 每天北京时间 03:00 执行，也支持手动触发。执行时会从
`NateScarlet/holiday-cn` 获取去年、今年和明年的 `yyyy.json`，提取其中的休息日并写入本仓库的
`json/holiday.json`。如果文件内容有变化，workflow 会自动提交。

### 数据格式

`holiday.json` 外层是数组，每一项表示一个年份：

```json
[
  {
    "year": 2026,
    "holidays": [
      "2026-01-01"
    ]
  }
]
```

判断规则：

- 日期存在于任一年份项的 `holidays` 中时，视为节假日休息日。
- 用户排班规则保存在本地，和 `holiday.json` 分离。
- `holiday.json` 不包含其他日期分类。

## 主页面

主页面由 `MainActivity` 承载，主要包含：

- 通知监听状态。
- 关键词状态行，点击后弹出输入对话框。
- 跳过按钮文本状态行，点击后弹出输入对话框。
- 休息日规则状态行，点击后配置双休、单休、大小周、上 x 休 y 或完全自定义。
- 调休日历状态行，点击后弹出可滚动的日历详情弹窗。
- 调休日历弹窗中的手动更新按钮。
- 通知监听设置入口，已开启通知监听时按钮会禁用并显示已开启状态。
- 自动跳过闹钟记录，当前最多保留最近 10 条。
- 闹钟通知处理记录，当前最多保留最近 5 条。

## 设置页面

设置页面由 `SettingsActivity` 承载，使用 `zhanghai/ComposePreference` 构建。

当前“关于”部分包含：

- 版本名称。
- 版本号。
- Pixel Tailor 链接。
- Telegram 频道链接。

## 隐私说明

隐私政策见 [PRIVACY.md](PRIVACY.md)。

核心行为概述：

- 通知监听权限需要用户在系统设置中手动授予。
- 应用逻辑只处理目标时钟应用通知。
- 关键词、跳过按钮文本、休息日规则、调休日历缓存和执行记录保存在本地。
- 应用不会主动上传通知内容、用户设置或执行记录。
- 手动更新调休日历时会请求 GitHub Raw 上的 `holiday.json`。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Compose Material Icons Extended
- ComposePreference
- Koin
- Android `NotificationListenerService`
- Android Gradle Plugin 9.2.1
- Kotlin 2.4.0
- compileSdk 37
- targetSdk 37
- minSdk 31
- Java 21

## 项目结构

```text
app/src/main/java/vip/mystery0/pixel/snooze
├── PixelSnoozeApplication.kt
├── MainActivity.kt
├── SettingsActivity.kt
├── di
│   └── AppModule.kt
├── history
│   └── AlarmHistoryRepository.kt
├── holiday
│   ├── AssetHolidayDataSource.kt
│   ├── HolidayCalendar.kt
│   ├── HolidayDataSource.kt
│   ├── HolidayRepository.kt
│   └── LocalFirstHolidayDataSource.kt
├── notification
│   ├── AlarmDismissActionFinder.kt
│   ├── AlarmNotificationParser.kt
│   ├── DeskClockPackages.kt
│   └── PixelSnoozeNotificationListenerService.kt
├── preferences
│   └── UserPreferencesRepository.kt
├── schedule
│   ├── RestDayRepository.kt
│   ├── RestSchedulePreferencesRepository.kt
│   └── RestScheduleRule.kt
└── ui
    ├── home
    │   ├── CustomScheduleCalendar.kt
    │   ├── HomeScreen.kt
    │   └── RestScheduleDialog.kt
    ├── settings
    │   └── SettingsScreen.kt
    └── theme
        └── Theme.kt
```

## 核心模块

- `PixelSnoozeNotificationListenerService`：通知监听入口，负责过滤目标时钟应用通知并执行跳过流程。
- `AlarmNotificationParser`：读取通知标题和正文，并判断是否命中关键词。
- `AlarmDismissActionFinder`：从通知 actions 中查找匹配的跳过或关闭操作。
- `HolidayRepository`：加载和刷新节假日休息日数据，并暴露当前调休日历数据。
- `RestDayRepository`：组合节假日休息日数据和用户本地规则，判断今天是否休息。
- `RestSchedulePreferencesRepository`：使用 `SharedPreferences` 保存用户选择的休息日规则和自定义日期。
- `LocalFirstHolidayDataSource`：优先读取本地缓存，缓存不存在或无效时回退到内置 assets 数据；手动更新时拉取云端数据并写入本地缓存。
- `AssetHolidayDataSource`：解析内置或缓存的 `holiday.json`。
- `UserPreferencesRepository`：使用 `SharedPreferences` 保存关键词和跳过按钮文本。
- `AlarmHistoryRepository`：使用 `SharedPreferences` 保存自动跳过记录和通知处理记录。

## 构建

Debug 构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

Lint 检查：

```powershell
.\gradlew.bat :app:lintDebug
```

Release 构建：

```powershell
.\gradlew.bat :app:assembleRelease
```

## 签名配置

Release 签名配置由根目录 `signing.gradle` 读取。可以在 `local.properties` 中配置：

```properties
SIGN_KEY_STORE_FILE=your-keystore.jks
SIGN_KEY_STORE_PASSWORD=your-store-password
SIGN_KEY_ALIAS=your-key-alias
SIGN_KEY_PASSWORD=your-key-password
```

也可以使用同名环境变量提供这些值。

## 当前限制

- 当前仅适配 Google/AOSP 时钟应用包名。
- 是否能跳过闹钟取决于目标时钟通知是否提供可用的通知操作。
- 不同 ROM 或不同版本时钟应用的通知按钮文案可能不同，需要通过“跳过按钮文本”配置适配。
- 调休日历只保存节假日休息日集合，用户排班规则单独保存在本地。
- 本项目当前不要求单元测试，默认验证方式为 Android 构建、lint 和必要的人工检查。
