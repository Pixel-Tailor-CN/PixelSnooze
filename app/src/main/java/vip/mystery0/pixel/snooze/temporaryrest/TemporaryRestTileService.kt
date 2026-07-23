package vip.mystery0.pixel.snooze.temporaryrest

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.koin.android.ext.android.inject

class TemporaryRestTileService : TileService() {
    private val temporaryRestManager: TemporaryRestManager by inject()

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        super.onClick()
        val shouldEnable = !temporaryRestManager.currentState().isActive()
        val updateAction = Runnable {
            if (shouldEnable) {
                temporaryRestManager.enableToday()
            } else {
                temporaryRestManager.disable()
            }
            updateTile()
        }

        if (shouldEnable && isLocked && isSecure) {
            unlockAndRun(updateAction)
        } else {
            updateAction.run()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val state = temporaryRestManager.currentState()
        val isActive = state.isActive()
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "临时休息"
        tile.subtitle = state.shortSummaryText()
        tile.contentDescription = "临时休息，${state.shortSummaryText()}"
        tile.stateDescription = state.shortSummaryText()
        tile.updateTile()
    }
}
