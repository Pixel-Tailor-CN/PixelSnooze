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
