# Pixel Snooze

Pixel Snooze 是一款本地优先、低侵入性的 Android 辅助工具，用于在不修改系统时钟应用的前提下，通过通知监听自动跳过节假日闹钟。

项目属于原点系列，当前版本用于验证核心方案可行性：识别 Google/AOSP
时钟的“即将响铃”通知，命中指定闹钟关键词后，根据本地节假日数据判断今天是否为休息日。如果今天是休息日，应用会尝试调用通知中的关闭操作，静默关闭该次闹钟实例。

## 功能状态

- 基于 `NotificationListenerService` 监听闹钟通知。
- 仅处理 `com.google.android.deskclock` 和 `com.android.deskclock` 的通知。
- 默认关键词为 `节假日闹钟`。
- 使用内置 `holiday_2026.json` 作为本地节假日数据源。
- 节假日判断使用内存集合，查询路径不访问网络。
- 关闭动作匹配 `dismiss`、`在此关闭`、`关闭`、`取消`。
- 首页提供通知监听权限状态、关键词和内置日历状态展示。
- 首页提供跳转系统通知监听设置的入口。

## 使用方式

1. 在系统时钟中创建需要自动跳过的闹钟。
2. 将闹钟标签设置为 `节假日闹钟`。
3. 打开 Pixel Snooze。
4. 点击“打开通知监听设置”。
5. 在系统设置中授予 Pixel Snooze 通知监听权限。

当目标时钟应用发布“即将响铃”通知时，Pixel Snooze 会执行以下流程：

1. 先检查通知包名，非目标包名立即忽略。
2. 包名命中后读取通知标题和正文。
3. 判断标题或正文是否包含关键词。
4. 命中关键词后判断今天是否为休息日。
5. 如果今天是休息日，查找通知中的关闭操作并发送 `PendingIntent`。

## 当前限制

- 当前版本只内置 2026 年示例节假日数据。
- 内置数据用于方案验证，不承诺覆盖完整官方调休安排。
- 当前版本不包含云端更新、文件导入、数据库或后台同步。
- 当前版本不修改系统时钟应用，也不创建、编辑或删除闹钟。
- 方案依赖时钟应用是否提供可用的通知关闭操作，不同 ROM 或时钟版本可能表现不同。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Koin
- Android `NotificationListenerService`
- Android Gradle Plugin 9.2.1
- Kotlin 2.3.21
- compileSdk 37
- targetSdk 37
- minSdk 31

## 项目结构

```text
app/src/main/java/vip/mystery0/pixel/snooze
├── PixelSnoozeApplication.kt
├── MainActivity.kt
├── di
│   └── AppModule.kt
├── holiday
│   ├── AssetHolidayDataSource.kt
│   ├── HolidayCalendar.kt
│   ├── HolidayDataSource.kt
│   └── HolidayRepository.kt
├── notification
│   ├── AlarmDismissActionFinder.kt
│   ├── AlarmNotificationParser.kt
│   ├── DeskClockPackages.kt
│   └── PixelSnoozeNotificationListenerService.kt
├── preferences
│   └── UserPreferencesRepository.kt
└── ui
    ├── home
    │   └── HomeScreen.kt
    └── theme
```

## 核心模块

`PixelSnoozeNotificationListenerService` 是通知监听入口。它在 `onNotificationPosted`
的第一步进行包名过滤，避免对无关通知读取 `extras`。

`AlarmNotificationParser` 负责读取通知标题和正文，并匹配用户关键词。

`AlarmDismissActionFinder` 负责从通知 actions 中寻找关闭操作。

`HolidayRepository` 在初始化时加载节假日数据，并提供 `isHoliday()`。查询逻辑只读取内存集合。

`AssetHolidayDataSource` 从 `app/src/main/assets/holiday_2026.json` 读取内置数据。

`UserPreferencesRepository` 使用 `SharedPreferences` 保存闹钟关键词。

## 节假日数据格式

当前内置数据文件位于：

```text
app/src/main/assets/holiday_2026.json
```

格式如下：

```json
{
  "year": 2026,
  "holidays": ["2026-01-01"]
}
```

判断规则：

- 日期在 `holidays` 中时，视为休息日。
- 日期不在 `holidays` 中时，视为非休息日。
- 当前版本不需要 `workdays`、调休补班日或工作日数据。

## 构建

Debug 构建：

```powershell
.\gradlew.bat :app:assembleDebug
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

## 后续方向

- 将内置 JSON 替换为云端数据下载后的本地文件缓存。
- 云端数据源可使用 GitHub 仓库提供静态 JSON。
- 后台同步应放在受限条件下低频执行，通知监听热路径仍保持纯本地。
- 增加关键词配置和节假日数据状态管理。
