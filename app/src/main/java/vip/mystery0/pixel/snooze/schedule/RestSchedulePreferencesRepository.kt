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
