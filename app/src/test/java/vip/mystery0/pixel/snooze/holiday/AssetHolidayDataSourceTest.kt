package vip.mystery0.pixel.snooze.holiday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
