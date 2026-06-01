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
