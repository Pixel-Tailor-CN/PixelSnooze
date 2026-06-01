# Pixel Snooze 本地 JSON 首版 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 Pixel Snooze 首版可验证闭环：内置节假日 JSON、通知监听拦截、O(1) 本地判定、基础权限 UI。

**Architecture:** 核心逻辑先做成可单元测试的 Kotlin 类，再由 Android `NotificationListenerService` 编排调用。节假日数据通过 `HolidayDataSource` 抽象读取，首版使用 `AssetHolidayDataSource`，后续可替换为 GitHub 下载后的文件缓存，不引入数据库。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、Koin 4.1.x、Android `NotificationListenerService`、JUnit 4。

---

## 文件结构

- 修改 `gradle/libs.versions.toml`：补充 Kotlin Android 插件、Koin 依赖和 AndroidX 测试 core。
- 修改 `app/build.gradle.kts`：应用 Kotlin Android 插件，接入 Koin。
- 修改 `app/src/main/AndroidManifest.xml`：注册 Application 和通知监听服务。
- 创建 `app/src/main/java/vip/mystery0/pixel/snooze/PixelSnoozeApplication.kt`：启动 Koin。
- 创建 `app/src/main/java/vip/mystery0/pixel/snooze/di/AppModule.kt`：集中注册依赖。
- 创建 `app/src/main/java/vip/mystery0/pixel/snooze/notification/*`：通知包名、文本解析、关闭动作匹配、NLS 服务。
- 创建 `app/src/main/java/vip/mystery0/pixel/snooze/holiday/*`：日期模型、数据源、仓库。
- 创建 `app/src/main/java/vip/mystery0/pixel/snooze/preferences/UserPreferencesRepository.kt`：首版关键词存储。
- 创建 `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt`：基础首页。
- 修改 `app/src/main/java/vip/mystery0/pixel/snooze/MainActivity.kt`：接入首页。
- 创建 `app/src/main/assets/holiday_2026.json`：内置首版示例日历。
- 创建 `app/src/test/java/vip/mystery0/pixel/snooze/...`：核心逻辑单元测试。

Koin 版本选择说明：Koin 官方文档标注 4.2.x 需要 Kotlin 2.3+，当前项目 Kotlin 为 2.2.10，因此计划使用 4.1.x 系列。

---

### Task 1: 配置 Gradle 依赖

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 修改版本目录**

将 `gradle/libs.versions.toml` 更新为包含以下新增项，保留现有版本号：

```toml
[versions]
agp = "9.3.0-alpha09"
coreKtx = "1.18.0"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
lifecycleRuntimeKtx = "2.10.0"
activityCompose = "1.13.0"
kotlin = "2.2.10"
composeBom = "2026.02.01"
koin = "4.1.1"
androidxTestCore = "1.5.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
koin-android = { group = "io.insert-koin", name = "koin-android", version.ref = "koin" }
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTestCore" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 2: 修改 app Gradle 配置**

在 `app/build.gradle.kts` 中应用 Kotlin Android 插件，并添加依赖：

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 3: 验证 Gradle 配置**

Run: `.\gradlew.bat :app:tasks --quiet`

Expected: 命令成功返回，能看到 `assemble`、`test` 等任务。

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "chore: configure Kotlin and Koin dependencies"
```

---

### Task 2: 实现节假日核心模型和仓库

**Files:**
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/holiday/HolidayCalendar.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/holiday/HolidayDataSource.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/holiday/HolidayRepository.kt`
- Test: `app/src/test/java/vip/mystery0/pixel/snooze/holiday/HolidayRepositoryTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
package vip.mystery0.pixel.snooze.holiday

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayRepositoryTest {
    @Test
    fun `holiday set has priority over weekday rule`() {
        val repository = HolidayRepository(
            dataSource = FakeHolidayDataSource(
                HolidayCalendar(
                    year = 2026,
                    holidays = setOf(LocalDate.parse("2026-01-01")),
                    workdays = emptySet()
                )
            ),
            todayProvider = { LocalDate.parse("2025-12-31") }
        )

        assertTrue(repository.isHolidayTomorrow())
    }

    @Test
    fun `workday set has priority over weekend rule`() {
        val repository = HolidayRepository(
            dataSource = FakeHolidayDataSource(
                HolidayCalendar(
                    year = 2026,
                    holidays = emptySet(),
                    workdays = setOf(LocalDate.parse("2026-02-14"))
                )
            ),
            todayProvider = { LocalDate.parse("2026-02-13") }
        )

        assertFalse(repository.isHolidayTomorrow())
    }

    @Test
    fun `weekend is holiday when not overridden`() {
        val repository = HolidayRepository(
            dataSource = FakeHolidayDataSource(
                HolidayCalendar(
                    year = 2026,
                    holidays = emptySet(),
                    workdays = emptySet()
                )
            ),
            todayProvider = { LocalDate.parse("2026-01-02") }
        )

        assertTrue(repository.isHolidayTomorrow())
    }
}

private class FakeHolidayDataSource(
    private val calendar: HolidayCalendar
) : HolidayDataSource {
    override fun loadCalendar(): HolidayCalendar = calendar
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "vip.mystery0.pixel.snooze.holiday.HolidayRepositoryTest"`

Expected: FAIL，原因是 `HolidayRepository`、`HolidayCalendar` 或 `HolidayDataSource` 未定义。

- [ ] **Step 3: 实现模型和仓库**

```kotlin
package vip.mystery0.pixel.snooze.holiday

import java.time.LocalDate

data class HolidayCalendar(
    val year: Int,
    val holidays: Set<LocalDate>,
    val workdays: Set<LocalDate>
) {
    fun holidayCount(): Int = holidays.size
}
```

```kotlin
package vip.mystery0.pixel.snooze.holiday

interface HolidayDataSource {
    fun loadCalendar(): HolidayCalendar
}
```

```kotlin
package vip.mystery0.pixel.snooze.holiday

import java.time.DayOfWeek
import java.time.LocalDate

class HolidayRepository(
    dataSource: HolidayDataSource,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    private val calendar: HolidayCalendar = dataSource.loadCalendar()

    fun isHolidayTomorrow(): Boolean {
        val tomorrow = todayProvider().plusDays(1)
        return isHoliday(tomorrow)
    }

    fun currentCalendar(): HolidayCalendar = calendar

    private fun isHoliday(date: LocalDate): Boolean {
        if (date in calendar.holidays) return true
        if (date in calendar.workdays) return false
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "vip.mystery0.pixel.snooze.holiday.HolidayRepositoryTest"`

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/vip/mystery0/pixel/snooze/holiday app/src/test/java/vip/mystery0/pixel/snooze/holiday
git commit -m "feat: add local holiday repository"
```

---

### Task 3: 实现内置 JSON 数据源

**Files:**
- Create: `app/src/main/assets/holiday_2026.json`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/holiday/AssetHolidayDataSource.kt`
- Test: `app/src/test/java/vip/mystery0/pixel/snooze/holiday/AssetHolidayDataSourceTest.kt`

- [ ] **Step 1: 写内置 JSON**

```json
{
  "year": 2026,
  "holidays": [
    "2026-01-01",
    "2026-02-16",
    "2026-02-17",
    "2026-02-18",
    "2026-02-19",
    "2026-02-20",
    "2026-04-06",
    "2026-05-01",
    "2026-06-19",
    "2026-09-25",
    "2026-10-01",
    "2026-10-02",
    "2026-10-05"
  ],
  "workdays": []
}
```

说明：该 JSON 只用于方案验证，不承诺覆盖 2026 年完整官方调休安排。后续云端版本会替换该数据源。

- [ ] **Step 2: 写 JSON 解析测试**

```kotlin
package vip.mystery0.pixel.snooze.holiday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

class AssetHolidayDataSourceTest {
    @Test
    fun `parse embedded holiday json`() {
        val json = """
            {
              "year": 2026,
              "holidays": ["2026-01-01"],
              "workdays": ["2026-02-14"]
            }
        """.trimIndent()

        val calendar = AssetHolidayDataSource.parse(json.byteInputStream())

        assertEquals(2026, calendar.year)
        assertTrue(LocalDate.parse("2026-01-01") in calendar.holidays)
        assertTrue(LocalDate.parse("2026-02-14") in calendar.workdays)
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "vip.mystery0.pixel.snooze.holiday.AssetHolidayDataSourceTest"`

Expected: FAIL，原因是 `AssetHolidayDataSource` 未定义。

- [ ] **Step 4: 实现 Asset 数据源**

```kotlin
package vip.mystery0.pixel.snooze.holiday

import android.content.Context
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDate

class AssetHolidayDataSource(
    private val context: Context,
    private val assetName: String = "holiday_2026.json"
) : HolidayDataSource {
    override fun loadCalendar(): HolidayCalendar {
        return context.assets.open(assetName).use(::parse)
    }

    companion object {
        fun parse(inputStream: InputStream): HolidayCalendar {
            val json = JSONObject(inputStream.bufferedReader().use { it.readText() })
            return HolidayCalendar(
                year = json.getInt("year"),
                holidays = json.getJSONArray("holidays").toDateSet(),
                workdays = json.getJSONArray("workdays").toDateSet()
            )
        }
    }
}

private fun org.json.JSONArray.toDateSet(): Set<LocalDate> {
    return buildSet {
        for (index in 0 until length()) {
            add(LocalDate.parse(getString(index)))
        }
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "vip.mystery0.pixel.snooze.holiday.AssetHolidayDataSourceTest"`

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/assets/holiday_2026.json app/src/main/java/vip/mystery0/pixel/snooze/holiday/AssetHolidayDataSource.kt app/src/test/java/vip/mystery0/pixel/snooze/holiday/AssetHolidayDataSourceTest.kt
git commit -m "feat: load holidays from bundled json"
```

---

### Task 4: 实现通知解析和关闭动作匹配

**Files:**
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/notification/DeskClockPackages.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/notification/AlarmNotificationParser.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/notification/AlarmDismissActionFinder.kt`
- Test: `app/src/test/java/vip/mystery0/pixel/snooze/notification/DeskClockPackagesTest.kt`
- Test: `app/src/test/java/vip/mystery0/pixel/snooze/notification/AlarmNotificationParserTest.kt`
- Test: `app/src/test/java/vip/mystery0/pixel/snooze/notification/AlarmDismissActionFinderTest.kt`

- [ ] **Step 1: 写白名单测试**

```kotlin
package vip.mystery0.pixel.snooze.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeskClockPackagesTest {
    @Test
    fun `only desk clock packages are accepted`() {
        assertTrue(DeskClockPackages.isTarget("com.google.android.deskclock"))
        assertTrue(DeskClockPackages.isTarget("com.android.deskclock"))
        assertFalse(DeskClockPackages.isTarget("com.example.other"))
    }
}
```

- [ ] **Step 2: 写关键词解析测试**

```kotlin
package vip.mystery0.pixel.snooze.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmNotificationParserTest {
    @Test
    fun `matches keyword from title or text`() {
        val parser = AlarmNotificationParser()

        assertTrue(parser.matchesKeyword("节假日闹钟", "即将响铃", "节假日闹钟"))
        assertTrue(parser.matchesKeyword("即将响铃", "节假日闹钟 07:30", "节假日闹钟"))
        assertFalse(parser.matchesKeyword("即将响铃", "工作日闹钟", "节假日闹钟"))
    }

    @Test
    fun `blank keyword never matches`() {
        val parser = AlarmNotificationParser()

        assertFalse(parser.matchesKeyword("节假日闹钟", null, " "))
    }
}
```

- [ ] **Step 3: 写关闭动作标题测试**

```kotlin
package vip.mystery0.pixel.snooze.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmDismissActionFinderTest {
    @Test
    fun `matches dismiss action titles`() {
        val finder = AlarmDismissActionFinder()

        assertTrue(finder.isDismissTitle("Dismiss"))
        assertTrue(finder.isDismissTitle("在此关闭"))
        assertTrue(finder.isDismissTitle("关闭闹钟"))
        assertTrue(finder.isDismissTitle("取消"))
        assertFalse(finder.isDismissTitle("Snooze"))
    }
}
```

- [ ] **Step 4: 运行测试确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "vip.mystery0.pixel.snooze.notification.*"`

Expected: FAIL，原因是目标类未定义。

- [ ] **Step 5: 实现通知核心类**

```kotlin
package vip.mystery0.pixel.snooze.notification

object DeskClockPackages {
    private val targetPackages = setOf(
        "com.google.android.deskclock",
        "com.android.deskclock"
    )

    fun isTarget(packageName: String): Boolean = packageName in targetPackages
}
```

```kotlin
package vip.mystery0.pixel.snooze.notification

import android.app.Notification

class AlarmNotificationParser {
    fun matchesKeyword(title: CharSequence?, text: CharSequence?, keyword: String): Boolean {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isEmpty()) return false
        return title?.contains(normalizedKeyword) == true || text?.contains(normalizedKeyword) == true
    }

    fun matchesKeyword(notification: Notification, keyword: String): Boolean {
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)
        return matchesKeyword(title, text, keyword)
    }
}
```

```kotlin
package vip.mystery0.pixel.snooze.notification

import android.app.Notification

class AlarmDismissActionFinder {
    private val dismissWords = listOf("dismiss", "在此关闭", "关闭", "取消")

    fun findDismissAction(notification: Notification): Notification.Action? {
        return notification.actions?.firstOrNull { action ->
            isDismissTitle(action.title?.toString())
        }
    }

    fun isDismissTitle(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        val normalizedTitle = title.lowercase()
        return dismissWords.any { word -> normalizedTitle.contains(word.lowercase()) }
    }
}
```

- [ ] **Step 6: 运行测试确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "vip.mystery0.pixel.snooze.notification.*"`

Expected: PASS。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/vip/mystery0/pixel/snooze/notification app/src/test/java/vip/mystery0/pixel/snooze/notification
git commit -m "feat: add alarm notification matching"
```

---

### Task 5: 接入 Koin、偏好设置和通知监听服务

**Files:**
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/PixelSnoozeApplication.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/di/AppModule.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/preferences/UserPreferencesRepository.kt`
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/notification/PixelSnoozeNotificationListenerService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 实现用户偏好仓库**

```kotlin
package vip.mystery0.pixel.snooze.preferences

import android.content.Context

class UserPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("pixel_snooze_preferences", Context.MODE_PRIVATE)

    fun keyword(): String {
        return preferences.getString(KEY_ALARM_KEYWORD, DEFAULT_KEYWORD) ?: DEFAULT_KEYWORD
    }

    fun updateKeyword(keyword: String) {
        preferences.edit().putString(KEY_ALARM_KEYWORD, keyword.trim()).apply()
    }

    companion object {
        const val DEFAULT_KEYWORD = "节假日闹钟"
        private const val KEY_ALARM_KEYWORD = "alarm_keyword"
    }
}
```

- [ ] **Step 2: 实现 Koin 模块和 Application**

```kotlin
package vip.mystery0.pixel.snooze.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import vip.mystery0.pixel.snooze.holiday.AssetHolidayDataSource
import vip.mystery0.pixel.snooze.holiday.HolidayDataSource
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.notification.AlarmDismissActionFinder
import vip.mystery0.pixel.snooze.notification.AlarmNotificationParser
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository

val appModule = module {
    single<HolidayDataSource> { AssetHolidayDataSource(androidContext()) }
    single { HolidayRepository(get()) }
    single { AlarmNotificationParser() }
    single { AlarmDismissActionFinder() }
    single { UserPreferencesRepository(androidContext()) }
}
```

```kotlin
package vip.mystery0.pixel.snooze

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import vip.mystery0.pixel.snooze.di.appModule

class PixelSnoozeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PixelSnoozeApplication)
            modules(appModule)
        }
    }
}
```

- [ ] **Step 3: 实现通知监听服务**

```kotlin
package vip.mystery0.pixel.snooze.notification

import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.koin.android.ext.android.inject
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository

class PixelSnoozeNotificationListenerService : NotificationListenerService() {
    private val parser: AlarmNotificationParser by inject()
    private val actionFinder: AlarmDismissActionFinder by inject()
    private val holidayRepository: HolidayRepository by inject()
    private val preferencesRepository: UserPreferencesRepository by inject()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!DeskClockPackages.isTarget(sbn.packageName)) return

        val notification = sbn.notification ?: run {
            Log.w(TAG, "Target notification has no notification payload")
            return
        }

        val keyword = preferencesRepository.keyword()
        if (!parser.matchesKeyword(notification, keyword)) return

        if (!holidayRepository.isHolidayTomorrow()) {
            Log.i(TAG, "Alarm keyword matched but tomorrow is not holiday")
            return
        }

        val action = actionFinder.findDismissAction(notification)
        if (action == null) {
            Log.w(TAG, "Dismiss action not found")
            return
        }

        try {
            action.actionIntent.send()
            Log.i(TAG, "Dismiss action sent")
        } catch (error: PendingIntent.CanceledException) {
            Log.w(TAG, "Dismiss action was canceled", error)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Dismiss action failed", error)
        }
    }

    companion object {
        private const val TAG = "PixelSnoozeNls"
    }
}
```

- [ ] **Step 4: 注册 Application 和 NLS**

```xml
<application
    android:name=".PixelSnoozeApplication"
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.PixelSnooze">

    <service
        android:name=".notification.PixelSnoozeNotificationListenerService"
        android:exported="true"
        android:label="@string/app_name"
        android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
        <intent-filter>
            <action android:name="android.service.notification.NotificationListenerService" />
        </intent-filter>
    </service>
</application>
```

保留现有 `<activity>` 节点，只在同一个 `<application>` 内新增 `android:name` 和 `<service>`。

- [ ] **Step 5: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: BUILD SUCCESSFUL。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/vip/mystery0/pixel/snooze/PixelSnoozeApplication.kt app/src/main/java/vip/mystery0/pixel/snooze/di app/src/main/java/vip/mystery0/pixel/snooze/preferences app/src/main/java/vip/mystery0/pixel/snooze/notification/PixelSnoozeNotificationListenerService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: wire notification listener service"
```

---

### Task 6: 实现基础 Compose 首页

**Files:**
- Create: `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 实现首页 UI**

```kotlin
package vip.mystery0.pixel.snooze.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.notification.PixelSnoozeNotificationListenerService
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository

@Composable
fun HomeScreen(
    holidayRepository: HolidayRepository,
    preferencesRepository: UserPreferencesRepository
) {
    val context = LocalContext.current
    var listenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    val calendar = remember { holidayRepository.currentCalendar() }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pixel Snooze",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "本地优先的节假日闹钟跳过工具",
                style = MaterialTheme.typography.bodyLarge
            )

            StatusRow("通知监听", if (listenerEnabled) "已启用" else "未启用")
            StatusRow("关键词", preferencesRepository.keyword())
            StatusRow("内置日历", "${calendar.year} 年，${calendar.holidayCount()} 个休息日")

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    listenerEnabled = isNotificationListenerEnabled(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开通知监听设置")
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    if (enabledListeners.isNullOrBlank()) return false

    val expectedName = ComponentName(
        context,
        PixelSnoozeNotificationListenerService::class.java
    ).flattenToString()

    return TextUtils.split(enabledListeners, ":").any { it.equals(expectedName, ignoreCase = true) }
}
```

- [ ] **Step 2: 接入 MainActivity**

```kotlin
package vip.mystery0.pixel.snooze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository
import vip.mystery0.pixel.snooze.ui.home.HomeScreen
import vip.mystery0.pixel.snooze.ui.theme.PixelSnoozeTheme

class MainActivity : ComponentActivity() {
    private val holidayRepository: HolidayRepository by inject()
    private val preferencesRepository: UserPreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelSnoozeTheme {
                HomeScreen(
                    holidayRepository = holidayRepository,
                    preferencesRepository = preferencesRepository
                )
            }
        }
    }
}
```

- [ ] **Step 3: 更新应用名**

```xml
<resources>
    <string name="app_name">Pixel Snooze</string>
</resources>
```

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/vip/mystery0/pixel/snooze/MainActivity.kt app/src/main/java/vip/mystery0/pixel/snooze/ui/home app/src/main/res/values/strings.xml
git commit -m "feat: add local-first home screen"
```

---

### Task 7: 全量验证和收尾

**Files:**
- Verify: all modified files

- [ ] **Step 1: 运行单元测试**

Run: `.\gradlew.bat :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL。

- [ ] **Step 2: 运行 Debug 编译**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 检查 Git 状态**

Run: `git status --short`

Expected: 只剩用户已有的 `init.md` 状态，或工作区干净；不应出现未解释的源码变更。

- [ ] **Step 4: 最终提交**

如果前面每个 Task 已提交，本步骤不需要新提交。若有修正，使用：

```bash
git add <changed-files>
git commit -m "test: verify local json alarm flow"
```

---

## 自审结果

- Spec 覆盖：NLS、包名快速熔断、关键词匹配、内置 JSON、O(1) 内存判定、无数据库、无首版联网、基础 UI、英文日志均有对应任务。
- 占位符扫描：计划中没有 `TBD`、`TODO` 或未定义的后续实现要求。
- 类型一致性：`HolidayDataSource`、`HolidayRepository`、`AlarmNotificationParser`、`AlarmDismissActionFinder`、`UserPreferencesRepository` 的命名和调用关系在各任务中一致。
