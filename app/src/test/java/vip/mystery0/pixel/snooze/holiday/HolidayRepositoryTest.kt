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
