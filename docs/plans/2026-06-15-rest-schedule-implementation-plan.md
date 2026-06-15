# Rest Schedule Strategy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local rest-day strategy layer so Pixel Snooze can decide whether today is a rest day using holidays plus weekend, single day off, alternating weeks, x-work-y-rest cycles, or fully custom monthly schedules.

**Architecture:** Keep `HolidayRepository` focused on loading `holiday.json` and introduce `schedule` classes for user schedule configuration and rest-day evaluation. The notification listener calls one lightweight `RestDayRepository.isRestDay()` method; UI writes schedule preferences through a separate repository.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Koin, `SharedPreferences`, `org.json`, Java Time.

---

## Source Design

Approved design document:

```text
docs/plans/2026-06-15-rest-schedule-design.md
```

Repository constraints to preserve:

- Runtime only checks whether today is a rest day.
- `holiday.json` still contains only `year` and `holidays`.
- No `workdays`, adjusted workday data, database, cloud sync, Root, Xposed, or decompilation.
- Notification listener hot path must not perform network I/O or heavy parsing.
- Do not add unit tests as a project requirement; verify with Android build, lint, and manual checks.

## File Structure

Create:

- `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestScheduleRule.kt`
  Defines schedule modes, rule data, and pure date calculation helpers.
- `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestSchedulePreferencesRepository.kt`
  Reads and writes schedule configuration using `SharedPreferences` and JSON.
- `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestDayRepository.kt`
  Combines `HolidayRepository` with the current schedule rule and exposes `isRestDay()`.
- `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/RestScheduleDialog.kt`
  Provides the main schedule mode dialog for weekend, single day off, alternating week, and cycle modes.
- `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/CustomScheduleCalendar.kt`
  Provides a focused current-month custom schedule editor.

Modify:

- `app/src/main/java/vip/mystery0/pixel/snooze/di/AppModule.kt`
  Register schedule repositories.
- `app/src/main/java/vip/mystery0/pixel/snooze/notification/PixelSnoozeNotificationListenerService.kt`
  Replace direct holiday check with rest-day check.
- `app/src/main/java/vip/mystery0/pixel/snooze/MainActivity.kt`
  Inject and pass schedule repositories to `HomeScreen`.
- `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt`
  Add the `休息日规则` row and wire dialogs.
- `README.md`
  Describe schedule modes without changing `holiday.json` semantics.
- `PRIVACY.md`
  Mention local schedule preferences and custom dates.

## Task 1: Add Schedule Domain Model

**Files:**

- Create: `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestScheduleRule.kt`

- [ ] **Step 1: Create the schedule model file**

Create `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestScheduleRule.kt` with this content:

```kotlin
package vip.mystery0.pixel.snooze.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.lang.Math.floorMod

enum class RestScheduleMode {
    HOLIDAY_AND_WEEKEND,
    HOLIDAY_AND_SINGLE_DAY_OFF,
    HOLIDAY_AND_ALTERNATING_WEEK,
    HOLIDAY_AND_CYCLE,
    CUSTOM
}

enum class AlternatingWeekType {
    LARGE,
    SMALL;

    fun opposite(): AlternatingWeekType {
        return when (this) {
            LARGE -> SMALL
            SMALL -> LARGE
        }
    }
}

data class CustomMonthlySchedule(
    val month: YearMonth,
    val workDates: Set<LocalDate> = emptySet(),
    val restDates: Set<LocalDate> = emptySet()
) {
    init {
        require(workDates.none { YearMonth.from(it) != month }) {
            "Custom work dates must belong to $month"
        }
        require(restDates.none { YearMonth.from(it) != month }) {
            "Custom rest dates must belong to $month"
        }
        require(workDates.intersect(restDates).isEmpty()) {
            "Custom work dates and rest dates must not overlap"
        }
    }
}

sealed interface RestScheduleRule {
    val mode: RestScheduleMode

    data object HolidayAndWeekend : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.HOLIDAY_AND_WEEKEND
    }

    data class HolidayAndSingleDayOff(
        val restDayOfWeek: DayOfWeek
    ) : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF
    }

    data class HolidayAndAlternatingWeek(
        val largeWeekRestDays: Set<DayOfWeek>,
        val smallWeekRestDays: Set<DayOfWeek>,
        val anchorWeekStartDate: LocalDate,
        val anchorWeekType: AlternatingWeekType
    ) : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK

        init {
            require(largeWeekRestDays.isNotEmpty()) { "Large week rest days must not be empty" }
            require(smallWeekRestDays.isNotEmpty()) { "Small week rest days must not be empty" }
            require(anchorWeekStartDate.dayOfWeek == DayOfWeek.MONDAY) {
                "Anchor week start date must be Monday"
            }
        }
    }

    data class HolidayAndCycle(
        val workDays: Int,
        val restDays: Int,
        val anchorDate: LocalDate,
        val anchorDayIndex: Int
    ) : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.HOLIDAY_AND_CYCLE

        init {
            require(workDays >= 1) { "Work days must be positive" }
            require(restDays >= 1) { "Rest days must be positive" }
            require(anchorDayIndex in 1..(workDays + restDays)) {
                "Anchor day index must be within the cycle"
            }
        }
    }

    data class Custom(
        val monthlySchedules: Map<YearMonth, CustomMonthlySchedule>
    ) : RestScheduleRule {
        override val mode: RestScheduleMode = RestScheduleMode.CUSTOM
    }
}

fun RestScheduleRule.isScheduleRestDay(date: LocalDate): Boolean {
    return when (this) {
        RestScheduleRule.HolidayAndWeekend -> {
            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        }

        is RestScheduleRule.HolidayAndSingleDayOff -> {
            date.dayOfWeek == restDayOfWeek
        }

        is RestScheduleRule.HolidayAndAlternatingWeek -> {
            val currentWeekStart = date.startOfWeek()
            val weeksBetween = ChronoUnit.WEEKS.between(anchorWeekStartDate, currentWeekStart)
            val currentWeekType = if (floorMod(weeksBetween, 2L) == 0L) {
                anchorWeekType
            } else {
                anchorWeekType.opposite()
            }
            val restDays = when (currentWeekType) {
                AlternatingWeekType.LARGE -> largeWeekRestDays
                AlternatingWeekType.SMALL -> smallWeekRestDays
            }
            date.dayOfWeek in restDays
        }

        is RestScheduleRule.HolidayAndCycle -> {
            val cycleLength = workDays + restDays
            val daysBetween = ChronoUnit.DAYS.between(anchorDate, date)
            val currentIndex = floorMod(anchorDayIndex - 1L + daysBetween, cycleLength.toLong()) + 1
            currentIndex > workDays.toLong()
        }

        is RestScheduleRule.Custom -> {
            val month = YearMonth.from(date)
            date in monthlySchedules[month]?.restDates.orEmpty()
        }
    }
}

fun RestScheduleRule.summaryText(): String {
    return when (this) {
        RestScheduleRule.HolidayAndWeekend -> "节假日 + 双休"
        is RestScheduleRule.HolidayAndSingleDayOff -> "节假日 + 单休（${restDayOfWeek.displayText()}）"
        is RestScheduleRule.HolidayAndAlternatingWeek -> {
            "节假日 + 大小周（本周锚点：${anchorWeekType.displayText()}）"
        }
        is RestScheduleRule.HolidayAndCycle -> "节假日 + 上 ${workDays} 休 ${restDays}"
        is RestScheduleRule.Custom -> "完全自定义"
    }
}

fun LocalDate.startOfWeek(): LocalDate {
    return with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

fun DayOfWeek.displayText(): String {
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

fun AlternatingWeekType.displayText(): String {
    return when (this) {
        AlternatingWeekType.LARGE -> "大周"
        AlternatingWeekType.SMALL -> "小周"
    }
}
```

- [ ] **Step 2: Compile the model**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestScheduleRule.kt
git commit -m "feat: add rest schedule model"
```

## Task 2: Add Schedule Preference Persistence

**Files:**

- Create: `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestSchedulePreferencesRepository.kt`

- [ ] **Step 1: Create the preferences repository**

Create `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestSchedulePreferencesRepository.kt` with this content:

```kotlin
package vip.mystery0.pixel.snooze.schedule

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class RestSchedulePreferencesRepository(
    context: Context,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    private val preferences =
        context.getSharedPreferences("pixel_snooze_schedule_preferences", Context.MODE_PRIVATE)

    fun currentRule(): RestScheduleRule {
        val raw = preferences.getString(KEY_RULE, null)
        val parsedRule = runCatching {
            if (raw.isNullOrBlank()) RestScheduleRule.HolidayAndWeekend else raw.toRule()
        }.getOrDefault(RestScheduleRule.HolidayAndWeekend)

        return if (parsedRule.mode == RestScheduleMode.CUSTOM) {
            val month = YearMonth.from(todayProvider())
            RestScheduleRule.Custom(mapOf(month to customMonthlySchedule(month)))
        } else {
            parsedRule
        }
    }

    fun updateHolidayAndWeekend() {
        updateRule(RestScheduleRule.HolidayAndWeekend)
    }

    fun updateSingleDayOff(restDayOfWeek: DayOfWeek) {
        updateRule(RestScheduleRule.HolidayAndSingleDayOff(restDayOfWeek))
    }

    fun updateAlternatingWeek(
        largeWeekRestDays: Set<DayOfWeek>,
        smallWeekRestDays: Set<DayOfWeek>,
        anchorWeekType: AlternatingWeekType
    ) {
        updateRule(
            RestScheduleRule.HolidayAndAlternatingWeek(
                largeWeekRestDays = largeWeekRestDays,
                smallWeekRestDays = smallWeekRestDays,
                anchorWeekStartDate = todayProvider().startOfWeek(),
                anchorWeekType = anchorWeekType
            )
        )
    }

    fun updateCycle(workDays: Int, restDays: Int, todayIndex: Int) {
        updateRule(
            RestScheduleRule.HolidayAndCycle(
                workDays = workDays,
                restDays = restDays,
                anchorDate = todayProvider(),
                anchorDayIndex = todayIndex
            )
        )
    }

    fun updateCustomMode() {
        updateRule(RestScheduleRule.Custom(emptyMap()))
    }

    fun customMonthlySchedule(month: YearMonth): CustomMonthlySchedule {
        val raw = preferences.getString(customMonthKey(month), null)
        return runCatching {
            if (raw.isNullOrBlank()) {
                CustomMonthlySchedule(month = month)
            } else {
                JSONObject(raw).toCustomMonthlySchedule(month)
            }
        }.getOrDefault(CustomMonthlySchedule(month = month))
    }

    fun updateCustomMonthlySchedule(schedule: CustomMonthlySchedule) {
        preferences.edit {
            putString(customMonthKey(schedule.month), schedule.toJson().toString())
        }
    }

    private fun updateRule(rule: RestScheduleRule) {
        preferences.edit {
            putString(KEY_RULE, rule.toJson().toString())
        }
    }

    private fun String.toRule(): RestScheduleRule {
        val json = JSONObject(this)
        return when (RestScheduleMode.valueOf(json.optString("mode"))) {
            RestScheduleMode.HOLIDAY_AND_WEEKEND -> RestScheduleRule.HolidayAndWeekend
            RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF -> {
                val ruleJson = json.getJSONObject("singleDayOff")
                RestScheduleRule.HolidayAndSingleDayOff(
                    restDayOfWeek = DayOfWeek.valueOf(ruleJson.getString("restDayOfWeek"))
                )
            }
            RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK -> {
                val ruleJson = json.getJSONObject("alternatingWeek")
                RestScheduleRule.HolidayAndAlternatingWeek(
                    largeWeekRestDays = ruleJson.getJSONArray("largeWeekRestDays").toDayOfWeekSet(),
                    smallWeekRestDays = ruleJson.getJSONArray("smallWeekRestDays").toDayOfWeekSet(),
                    anchorWeekStartDate = LocalDate.parse(ruleJson.getString("anchorWeekStartDate")),
                    anchorWeekType = AlternatingWeekType.valueOf(ruleJson.getString("anchorWeekType"))
                )
            }
            RestScheduleMode.HOLIDAY_AND_CYCLE -> {
                val ruleJson = json.getJSONObject("cycle")
                RestScheduleRule.HolidayAndCycle(
                    workDays = ruleJson.getInt("workDays"),
                    restDays = ruleJson.getInt("restDays"),
                    anchorDate = LocalDate.parse(ruleJson.getString("anchorDate")),
                    anchorDayIndex = ruleJson.getInt("anchorDayIndex")
                )
            }
            RestScheduleMode.CUSTOM -> RestScheduleRule.Custom(emptyMap())
        }
    }

    private fun RestScheduleRule.toJson(): JSONObject {
        val json = JSONObject().put("mode", mode.name)
        when (this) {
            RestScheduleRule.HolidayAndWeekend -> Unit
            is RestScheduleRule.HolidayAndSingleDayOff -> {
                json.put(
                    "singleDayOff",
                    JSONObject().put("restDayOfWeek", restDayOfWeek.name)
                )
            }
            is RestScheduleRule.HolidayAndAlternatingWeek -> {
                json.put(
                    "alternatingWeek",
                    JSONObject()
                        .put("largeWeekRestDays", largeWeekRestDays.toJsonArray())
                        .put("smallWeekRestDays", smallWeekRestDays.toJsonArray())
                        .put("anchorWeekStartDate", anchorWeekStartDate.toString())
                        .put("anchorWeekType", anchorWeekType.name)
                )
            }
            is RestScheduleRule.HolidayAndCycle -> {
                json.put(
                    "cycle",
                    JSONObject()
                        .put("workDays", workDays)
                        .put("restDays", restDays)
                        .put("anchorDate", anchorDate.toString())
                        .put("anchorDayIndex", anchorDayIndex)
                )
            }
            is RestScheduleRule.Custom -> Unit
        }
        return json
    }

    private fun CustomMonthlySchedule.toJson(): JSONObject {
        return JSONObject()
            .put("month", month.toString())
            .put("workDates", workDates.map { it.toString() }.toJsonArray())
            .put("restDates", restDates.map { it.toString() }.toJsonArray())
    }

    private fun JSONObject.toCustomMonthlySchedule(expectedMonth: YearMonth): CustomMonthlySchedule {
        return CustomMonthlySchedule(
            month = expectedMonth,
            workDates = optJSONArray("workDates").toLocalDateSet(),
            restDates = optJSONArray("restDates").toLocalDateSet()
        )
    }

    private fun JSONArray.toDayOfWeekSet(): Set<DayOfWeek> {
        return buildSet {
            for (index in 0 until length()) {
                add(DayOfWeek.valueOf(getString(index)))
            }
        }
    }

    private fun Set<DayOfWeek>.toJsonArray(): JSONArray {
        return map { it.name }.toJsonArray()
    }

    private fun List<String>.toJsonArray(): JSONArray {
        val jsonArray = JSONArray()
        forEach { jsonArray.put(it) }
        return jsonArray
    }

    private fun JSONArray?.toLocalDateSet(): Set<LocalDate> {
        if (this == null) return emptySet()
        return buildSet {
            for (index in 0 until length()) {
                add(LocalDate.parse(getString(index)))
            }
        }
    }

    private fun customMonthKey(month: YearMonth): String {
        return "$KEY_CUSTOM_MONTH_PREFIX$month"
    }

    companion object {
        private const val KEY_RULE = "rest_schedule_rule"
        private const val KEY_CUSTOM_MONTH_PREFIX = "custom_schedule_"
    }
}
```

- [ ] **Step 2: Build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestSchedulePreferencesRepository.kt
git commit -m "feat: persist rest schedule preferences"
```

## Task 3: Wire Rest-Day Evaluation Into the Notification Path

**Files:**

- Create: `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestDayRepository.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/di/AppModule.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/notification/PixelSnoozeNotificationListenerService.kt`

- [ ] **Step 1: Create `RestDayRepository`**

Create `app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestDayRepository.kt`:

```kotlin
package vip.mystery0.pixel.snooze.schedule

import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import java.time.LocalDate

class RestDayRepository(
    private val holidayRepository: HolidayRepository,
    private val schedulePreferencesRepository: RestSchedulePreferencesRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    fun isRestDay(): Boolean {
        val date = todayProvider()
        val rule = schedulePreferencesRepository.currentRule()
        if (rule is RestScheduleRule.Custom) {
            return rule.isScheduleRestDay(date)
        }
        if (date in holidayRepository.currentCalendar().holidays) {
            return true
        }
        return rule.isScheduleRestDay(date)
    }

    fun currentRule(): RestScheduleRule {
        return schedulePreferencesRepository.currentRule()
    }
}
```

- [ ] **Step 2: Register schedule repositories in Koin**

Modify `app/src/main/java/vip/mystery0/pixel/snooze/di/AppModule.kt`.

Add imports:

```kotlin
import vip.mystery0.pixel.snooze.schedule.RestDayRepository
import vip.mystery0.pixel.snooze.schedule.RestSchedulePreferencesRepository
```

Add definitions:

```kotlin
single { RestSchedulePreferencesRepository(androidContext()) }
single { RestDayRepository(get(), get()) }
```

Keep `HolidayRepository` registered as it is.

- [ ] **Step 3: Use `RestDayRepository` in the notification listener**

Modify `app/src/main/java/vip/mystery0/pixel/snooze/notification/PixelSnoozeNotificationListenerService.kt`.

Replace the holiday import:

```kotlin
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
```

with:

```kotlin
import vip.mystery0.pixel.snooze.schedule.RestDayRepository
```

Replace the injected property:

```kotlin
private val holidayRepository: HolidayRepository by inject()
```

with:

```kotlin
private val restDayRepository: RestDayRepository by inject()
```

Replace:

```kotlin
if (!holidayRepository.isHoliday()) {
    historyRepository.recordIgnore(packageName, title, text, "今天不是休息日")
    Log.i(TAG, "Alarm keyword matched but today is not holiday")
    return
}
```

with:

```kotlin
if (!restDayRepository.isRestDay()) {
    historyRepository.recordIgnore(packageName, title, text, "今天未命中休息日规则")
    Log.i(TAG, "Alarm keyword matched but today is not a rest day")
    return
}
```

- [ ] **Step 4: Build and lint**

Run:

```bash
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

Expected: both commands finish successfully.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/vip/mystery0/pixel/snooze/schedule/RestDayRepository.kt \
  app/src/main/java/vip/mystery0/pixel/snooze/di/AppModule.kt \
  app/src/main/java/vip/mystery0/pixel/snooze/notification/PixelSnoozeNotificationListenerService.kt
git commit -m "feat: evaluate rest days through schedule rules"
```

## Task 4: Add Schedule Rule Entry and Calculated-Mode Dialogs

**Files:**

- Create: `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/RestScheduleDialog.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/MainActivity.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt`

- [ ] **Step 1: Create `RestScheduleDialog.kt`**

Create `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/RestScheduleDialog.kt` with composables that expose these exact callbacks:

```kotlin
package vip.mystery0.pixel.snooze.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.snooze.schedule.AlternatingWeekType
import vip.mystery0.pixel.snooze.schedule.RestScheduleMode
import vip.mystery0.pixel.snooze.schedule.RestScheduleRule
import vip.mystery0.pixel.snooze.schedule.displayText
import java.time.DayOfWeek

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestScheduleDialog(
    currentRule: RestScheduleRule,
    onSaveWeekend: () -> Unit,
    onSaveSingleDayOff: (DayOfWeek) -> Unit,
    onSaveAlternatingWeek: (Set<DayOfWeek>, Set<DayOfWeek>, AlternatingWeekType) -> Unit,
    onSaveCycle: (Int, Int, Int) -> Unit,
    onOpenCustomCalendar: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember(currentRule) { mutableStateOf(currentRule.mode) }
    var singleRestDay by remember(currentRule) {
        mutableStateOf((currentRule as? RestScheduleRule.HolidayAndSingleDayOff)?.restDayOfWeek ?: DayOfWeek.SUNDAY)
    }
    var largeWeekRestDays by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndAlternatingWeek)?.largeWeekRestDays
                ?: setOf(DayOfWeek.SUNDAY)
        )
    }
    var smallWeekRestDays by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndAlternatingWeek)?.smallWeekRestDays
                ?: setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        )
    }
    var anchorWeekType by remember(currentRule) {
        mutableStateOf(
            (currentRule as? RestScheduleRule.HolidayAndAlternatingWeek)?.anchorWeekType
                ?: AlternatingWeekType.LARGE
        )
    }
    var workDaysText by remember(currentRule) {
        mutableStateOf((currentRule as? RestScheduleRule.HolidayAndCycle)?.workDays?.toString() ?: "3")
    }
    var restDaysText by remember(currentRule) {
        mutableStateOf((currentRule as? RestScheduleRule.HolidayAndCycle)?.restDays?.toString() ?: "2")
    }
    var todayIndexText by remember(currentRule) {
        mutableStateOf((currentRule as? RestScheduleRule.HolidayAndCycle)?.anchorDayIndex?.toString() ?: "1")
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("休息日规则") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScheduleModeRow("节假日 + 双休", RestScheduleMode.HOLIDAY_AND_WEEKEND, selectedMode) {
                    selectedMode = RestScheduleMode.HOLIDAY_AND_WEEKEND
                }
                ScheduleModeRow("节假日 + 单休", RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF, selectedMode) {
                    selectedMode = RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF
                }
                ScheduleModeRow("节假日 + 大小周", RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK, selectedMode) {
                    selectedMode = RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK
                }
                ScheduleModeRow("节假日 + 上 x 休 y", RestScheduleMode.HOLIDAY_AND_CYCLE, selectedMode) {
                    selectedMode = RestScheduleMode.HOLIDAY_AND_CYCLE
                }
                ScheduleModeRow("完全自定义", RestScheduleMode.CUSTOM, selectedMode) {
                    selectedMode = RestScheduleMode.CUSTOM
                }

                when (selectedMode) {
                    RestScheduleMode.HOLIDAY_AND_WEEKEND -> Text("周六、周日和节假日休息。")
                    RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF -> {
                        Text("选择每周休息日")
                        DayOfWeekSelector(
                            selectedDays = setOf(singleRestDay),
                            onToggle = { singleRestDay = it },
                            singleSelection = true
                        )
                    }
                    RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK -> {
                        Text("大周休息日")
                        DayOfWeekSelector(
                            selectedDays = largeWeekRestDays,
                            onToggle = { day ->
                                largeWeekRestDays = largeWeekRestDays.toggle(day).ifEmpty { setOf(day) }
                            }
                        )
                        Text("小周休息日")
                        DayOfWeekSelector(
                            selectedDays = smallWeekRestDays,
                            onToggle = { day ->
                                smallWeekRestDays = smallWeekRestDays.toggle(day).ifEmpty { setOf(day) }
                            }
                        )
                        Text("本周类型")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WeekTypeChip("大周", anchorWeekType == AlternatingWeekType.LARGE) {
                                anchorWeekType = AlternatingWeekType.LARGE
                            }
                            WeekTypeChip("小周", anchorWeekType == AlternatingWeekType.SMALL) {
                                anchorWeekType = AlternatingWeekType.SMALL
                            }
                        }
                    }
                    RestScheduleMode.HOLIDAY_AND_CYCLE -> {
                        OutlinedTextField(
                            value = workDaysText,
                            onValueChange = { workDaysText = it.filter(Char::isDigit) },
                            label = { Text("上班天数") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = restDaysText,
                            onValueChange = { restDaysText = it.filter(Char::isDigit) },
                            label = { Text("休息天数") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = todayIndexText,
                            onValueChange = { todayIndexText = it.filter(Char::isDigit) },
                            label = { Text("今天是周期第几天") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    RestScheduleMode.CUSTOM -> Text("完全自定义只使用你手动标记的休息日，不叠加节假日数据。")
                }

                errorText?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    errorText = null
                    when (selectedMode) {
                        RestScheduleMode.HOLIDAY_AND_WEEKEND -> onSaveWeekend()
                        RestScheduleMode.HOLIDAY_AND_SINGLE_DAY_OFF -> onSaveSingleDayOff(singleRestDay)
                        RestScheduleMode.HOLIDAY_AND_ALTERNATING_WEEK -> {
                            onSaveAlternatingWeek(largeWeekRestDays, smallWeekRestDays, anchorWeekType)
                        }
                        RestScheduleMode.HOLIDAY_AND_CYCLE -> {
                            val workDays = workDaysText.toIntOrNull()
                            val restDays = restDaysText.toIntOrNull()
                            val todayIndex = todayIndexText.toIntOrNull()
                            if (workDays == null || restDays == null || todayIndex == null) {
                                errorText = "请填写完整的数字"
                                return@TextButton
                            }
                            if (workDays < 1 || restDays < 1 || todayIndex !in 1..(workDays + restDays)) {
                                errorText = "今天是第几天必须在 1 到 ${workDays + restDays} 之间"
                                return@TextButton
                            }
                            onSaveCycle(workDays, restDays, todayIndex)
                        }
                        RestScheduleMode.CUSTOM -> onOpenCustomCalendar()
                    }
                }
            ) {
                Text(if (selectedMode == RestScheduleMode.CUSTOM) "编辑" else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ScheduleModeRow(
    text: String,
    mode: RestScheduleMode,
    selectedMode: RestScheduleMode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = mode == selectedMode, onClick = onClick)
        Text(text = text, modifier = Modifier.padding(top = 12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    singleSelection: Boolean = false
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day in selectedDays,
                onClick = {
                    if (singleSelection) {
                        onToggle(day)
                    } else {
                        onToggle(day)
                    }
                },
                label = { Text(day.displayText()) }
            )
        }
    }
}

@Composable
private fun WeekTypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

private fun Set<DayOfWeek>.toggle(day: DayOfWeek): Set<DayOfWeek> {
    return if (day in this) this - day else this + day
}
```

- [ ] **Step 2: Inject schedule repositories into `MainActivity`**

Modify `app/src/main/java/vip/mystery0/pixel/snooze/MainActivity.kt`.

Add imports:

```kotlin
import vip.mystery0.pixel.snooze.schedule.RestDayRepository
import vip.mystery0.pixel.snooze.schedule.RestSchedulePreferencesRepository
```

Add properties:

```kotlin
private val restDayRepository: RestDayRepository by inject()
private val schedulePreferencesRepository: RestSchedulePreferencesRepository by inject()
```

Pass them into `HomeScreen`:

```kotlin
HomeScreen(
    holidayRepository = holidayRepository,
    preferencesRepository = preferencesRepository,
    schedulePreferencesRepository = schedulePreferencesRepository,
    restDayRepository = restDayRepository,
    historyRepository = historyRepository,
    onOpenSettings = {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
)
```

- [ ] **Step 3: Add the schedule row to `HomeScreen`**

Modify the `HomeScreen` signature in `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt`:

```kotlin
fun HomeScreen(
    holidayRepository: HolidayRepository,
    preferencesRepository: UserPreferencesRepository,
    schedulePreferencesRepository: RestSchedulePreferencesRepository,
    restDayRepository: RestDayRepository,
    historyRepository: AlarmHistoryRepository,
    onOpenSettings: () -> Unit
)
```

Add imports:

```kotlin
import vip.mystery0.pixel.snooze.schedule.RestDayRepository
import vip.mystery0.pixel.snooze.schedule.RestSchedulePreferencesRepository
import vip.mystery0.pixel.snooze.schedule.summaryText
```

Add state near the existing dialog state:

```kotlin
var restRule by remember { mutableStateOf(restDayRepository.currentRule()) }
var showRestScheduleDialog by remember { mutableStateOf(false) }
var showCustomScheduleDialog by remember { mutableStateOf(false) }
```

Refresh state in the existing `ON_RESUME` block:

```kotlin
restRule = restDayRepository.currentRule()
```

Add the row before `调休日历`:

```kotlin
StatusRow(
    label = "休息日规则",
    value = restRule.summaryText(),
    onClick = { showRestScheduleDialog = true }
)
```

Add the schedule dialog block after the existing edit dialogs:

```kotlin
if (showRestScheduleDialog) {
    RestScheduleDialog(
        currentRule = restRule,
        onSaveWeekend = {
            schedulePreferencesRepository.updateHolidayAndWeekend()
            restRule = restDayRepository.currentRule()
            showRestScheduleDialog = false
            Toast.makeText(context, "休息日规则已保存", Toast.LENGTH_SHORT).show()
        },
        onSaveSingleDayOff = { day ->
            schedulePreferencesRepository.updateSingleDayOff(day)
            restRule = restDayRepository.currentRule()
            showRestScheduleDialog = false
            Toast.makeText(context, "休息日规则已保存", Toast.LENGTH_SHORT).show()
        },
        onSaveAlternatingWeek = { largeDays, smallDays, weekType ->
            schedulePreferencesRepository.updateAlternatingWeek(largeDays, smallDays, weekType)
            restRule = restDayRepository.currentRule()
            showRestScheduleDialog = false
            Toast.makeText(context, "休息日规则已保存", Toast.LENGTH_SHORT).show()
        },
        onSaveCycle = { workDays, restDays, todayIndex ->
            schedulePreferencesRepository.updateCycle(workDays, restDays, todayIndex)
            restRule = restDayRepository.currentRule()
            showRestScheduleDialog = false
            Toast.makeText(context, "休息日规则已保存", Toast.LENGTH_SHORT).show()
        },
        onOpenCustomCalendar = {
            schedulePreferencesRepository.updateCustomMode()
            restRule = restDayRepository.currentRule()
            showRestScheduleDialog = false
            showCustomScheduleDialog = true
        },
        onDismiss = { showRestScheduleDialog = false }
    )
}
```

- [ ] **Step 4: Build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/vip/mystery0/pixel/snooze/ui/home/RestScheduleDialog.kt \
  app/src/main/java/vip/mystery0/pixel/snooze/MainActivity.kt \
  app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt
git commit -m "feat: add rest schedule settings UI"
```

## Task 5: Add Fully Custom Current-Month Calendar Editor

**Files:**

- Create: `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/CustomScheduleCalendar.kt`
- Modify: `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt`

- [ ] **Step 1: Create the custom calendar dialog**

Create `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/CustomScheduleCalendar.kt`:

```kotlin
package vip.mystery0.pixel.snooze.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.snooze.schedule.CustomMonthlySchedule
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomScheduleCalendarDialog(
    initialSchedule: CustomMonthlySchedule,
    onSave: (CustomMonthlySchedule) -> Unit,
    onDismiss: () -> Unit
) {
    var workDates by remember(initialSchedule) { mutableStateOf(initialSchedule.workDates) }
    var restDates by remember(initialSchedule) { mutableStateOf(initialSchedule.restDates) }
    val month = initialSchedule.month
    val dates = remember(month) { month.datesInMonth() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${month.format(monthFormatter)} 自定义") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "未设置日期默认不休息，不自动跳过。",
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dates.forEach { date ->
                        val stateText = when (date) {
                            in restDates -> "${date.dayOfMonth} 休"
                            in workDates -> "${date.dayOfMonth} 班"
                            else -> "${date.dayOfMonth}"
                        }
                        FilterChip(
                            selected = date in restDates || date in workDates,
                            onClick = {
                                when (date) {
                                    in restDates -> {
                                        restDates = restDates - date
                                        workDates = workDates + date
                                    }
                                    in workDates -> {
                                        workDates = workDates - date
                                    }
                                    else -> {
                                        restDates = restDates + date
                                    }
                                }
                            },
                            label = { Text(stateText) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("休息：${restDates.size} 天")
                    Text("上班：${workDates.size} 天")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        CustomMonthlySchedule(
                            month = month,
                            workDates = workDates,
                            restDates = restDates
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun YearMonth.datesInMonth(): List<LocalDate> {
    return (1..lengthOfMonth()).map { day -> atDay(day) }
}

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
```

- [ ] **Step 2: Wire the custom calendar in `HomeScreen`**

Modify `app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt`.

Add imports:

```kotlin
import java.time.YearMonth
```

Add state:

```kotlin
var customMonth by remember { mutableStateOf(YearMonth.now()) }
```

Add this dialog block after `RestScheduleDialog`:

```kotlin
if (showCustomScheduleDialog) {
    CustomScheduleCalendarDialog(
        initialSchedule = schedulePreferencesRepository.customMonthlySchedule(customMonth),
        onSave = { schedule ->
            schedulePreferencesRepository.updateCustomMode()
            schedulePreferencesRepository.updateCustomMonthlySchedule(schedule)
            restRule = restDayRepository.currentRule()
            showCustomScheduleDialog = false
            Toast.makeText(context, "自定义排班已保存", Toast.LENGTH_SHORT).show()
        },
        onDismiss = { showCustomScheduleDialog = false }
    )
}
```

Keep `customMonth` fixed to the current month for this implementation. Cross-month editing is outside this implementation scope and does not affect the notification path.

- [ ] **Step 3: Build and lint**

Run:

```bash
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

Expected: both commands finish successfully.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/vip/mystery0/pixel/snooze/ui/home/CustomScheduleCalendar.kt \
  app/src/main/java/vip/mystery0/pixel/snooze/ui/home/HomeScreen.kt
git commit -m "feat: add custom monthly schedule editor"
```

## Task 6: Update Documentation and Run Final Verification

**Files:**

- Modify: `README.md`
- Modify: `PRIVACY.md`

- [ ] **Step 1: Update README feature text**

Modify `README.md` so the behavior section says:

```markdown
- 仅判断今天是否为休息日，不判断明天或其他日期。
- 支持多种本地休息日规则：节假日 + 双休、节假日 + 单休、节假日 + 大小周、节假日 + 上 x 休 y、完全自定义。
- 节假日数据仍只来自 `holiday.json` 中的 `holidays` 集合。
- 完全自定义模式只使用用户手动标记的休息日，不叠加节假日数据。
```

Modify the notification flow section so step 4 says:

```markdown
4. 命中关键词后，根据当前休息日规则判断今天是否休息。
```

Modify the calendar section so it still says:

```markdown
- 日期存在于任一年份项的 `holidays` 中时，视为节假日休息日。
- 用户排班规则保存在本地，和 `holiday.json` 分离。
- 项目不需要 `workdays`、调休补班日或工作日数据。
```

- [ ] **Step 2: Update privacy policy**

Modify `PRIVACY.md` so it mentions local schedule preferences:

```markdown
应用还会在本地保存用户选择的休息日规则，例如单休星期几、大小周配置、上 x 休 y 周期锚点，以及完全自定义模式下用户手动标记的日期。这些数据只保存在应用私有存储中，不会主动上传。
```

Keep the existing statement that holiday updates are only requested when the user manually triggers an update.

- [ ] **Step 3: Final build and lint**

Run:

```bash
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

Expected: both commands finish successfully.

- [ ] **Step 4: Manual inspection**

Install or run the debug build, then check:

```text
首页显示“休息日规则”状态行。
默认模式显示“节假日 + 双休”。
单休模式可以保存用户选择的星期几。
大小周模式可以保存大周休息日、小周休息日和本周类型。
上 x 休 y 模式会拒绝无效数字，并能保存有效配置。
完全自定义模式进入当前月编辑器。
调休日历弹窗仍只展示 holiday.json 的 holidays 数据。
通知处理记录中的未跳过原因显示“今天未命中休息日规则”。
```

- [ ] **Step 5: Commit**

```bash
git add README.md PRIVACY.md
git commit -m "docs: describe rest schedule rules"
```

## Final Verification Checklist

- [ ] `./gradlew :app:assembleDebug` succeeds.
- [ ] `./gradlew :app:lintDebug` succeeds.
- [ ] `holiday.json` format is unchanged.
- [ ] Notification listener has no network I/O in the rest-day decision path.
- [ ] `HolidayRepository` still only loads and refreshes holiday data.
- [ ] `RestDayRepository` is the only runtime decision entry point for rest days.
- [ ] Fully custom mode does not check `holidays`.
- [ ] README and privacy policy describe local schedule preferences accurately.
