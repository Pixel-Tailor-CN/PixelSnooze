package vip.mystery0.pixel.snooze.holiday

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDate

class AssetHolidayDataSource(
    private val context: Context,
    private val assetName: String = "holiday.json"
) : HolidayDataSource {
    override fun loadCalendar(): HolidayCalendar {
        return context.assets.open(assetName).use(::parse)
    }

    companion object {
        fun parse(inputStream: InputStream): HolidayCalendar {
            val json = JSONObject(inputStream.bufferedReader().use { it.readText() })
            return parse(json)
        }

        fun parse(text: String): HolidayCalendar {
            return parse(JSONObject(text))
        }

        private fun parse(json: JSONObject): HolidayCalendar {
            return HolidayCalendar(
                year = json.getInt("year"),
                holidays = json.getJSONArray("holidays").toDateSet(),
            )
        }
    }
}

private fun JSONArray.toDateSet(): Set<LocalDate> {
    return buildSet {
        for (index in 0 until length()) {
            add(LocalDate.parse(getString(index)))
        }
    }
}
