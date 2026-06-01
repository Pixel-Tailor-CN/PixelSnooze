package vip.mystery0.pixel.snooze.notification

object DeskClockPackages {
    private val targetPackages = setOf(
        "com.google.android.deskclock",
        "com.android.deskclock"
    )

    fun isTarget(packageName: String): Boolean = packageName in targetPackages
}
