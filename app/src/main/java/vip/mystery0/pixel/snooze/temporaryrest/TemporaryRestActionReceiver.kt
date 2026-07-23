package vip.mystery0.pixel.snooze.temporaryrest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TemporaryRestActionReceiver : BroadcastReceiver(), KoinComponent {
    private val temporaryRestManager: TemporaryRestManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TemporaryRestActions.DISABLE) {
            temporaryRestManager.disable()
        }
    }
}
