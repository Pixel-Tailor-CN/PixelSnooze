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
