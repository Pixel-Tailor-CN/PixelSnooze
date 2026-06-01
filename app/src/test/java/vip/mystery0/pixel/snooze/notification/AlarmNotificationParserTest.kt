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
