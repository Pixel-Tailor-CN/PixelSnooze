package vip.mystery0.pixel.snooze.temporaryrest

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArraySet

class TemporaryRestManager(
    context: Context,
    private val preferencesRepository: TemporaryRestPreferencesRepository,
    private val statusNotification: TemporaryRestStatusNotification,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    private val context = context.applicationContext
    private val stateListeners = CopyOnWriteArraySet<(TemporaryRestState) -> Unit>()

    fun currentState(): TemporaryRestState {
        val state = preferencesRepository.currentState()
        if (state != TemporaryRestState.Disabled && !state.isActive(todayProvider())) {
            preferencesRepository.updateState(TemporaryRestState.Disabled)
            statusNotification.cancel()
            requestTileUpdate()
            notifyStateChanged(TemporaryRestState.Disabled)
            return TemporaryRestState.Disabled
        }
        return state
    }

    fun enableToday() {
        val today = todayProvider()
        val currentState = currentState()
        val newState = when (currentState) {
            TemporaryRestState.Disabled -> TemporaryRestState.UntilDate(today)
            is TemporaryRestState.UntilDate -> {
                if (currentState.endDate.isBefore(today)) {
                    TemporaryRestState.UntilDate(today)
                } else {
                    currentState
                }
            }

            TemporaryRestState.UntilDisabled -> currentState
        }
        updateState(newState)
    }

    fun enableUntil(endDate: LocalDate) {
        require(!endDate.isBefore(todayProvider())) {
            "Temporary rest end date cannot be before today"
        }
        updateState(TemporaryRestState.UntilDate(endDate))
    }

    fun enableUntilDisabled() {
        updateState(TemporaryRestState.UntilDisabled)
    }

    fun disable() {
        updateState(TemporaryRestState.Disabled)
    }

    fun shouldRequestNotificationPermission(): Boolean {
        return !preferencesRepository.hasRequestedNotificationPermission()
    }

    fun markNotificationPermissionRequested() {
        preferencesRepository.markNotificationPermissionRequested()
    }

    fun addStateListener(listener: (TemporaryRestState) -> Unit) {
        stateListeners += listener
        listener(currentState())
    }

    fun removeStateListener(listener: (TemporaryRestState) -> Unit) {
        stateListeners -= listener
    }

    fun refreshSurfaces() {
        val state = currentState()
        statusNotification.sync(state, todayProvider())
        requestTileUpdate()
    }

    private fun updateState(state: TemporaryRestState) {
        preferencesRepository.updateState(state)
        statusNotification.sync(state, todayProvider())
        requestTileUpdate()
        notifyStateChanged(state)
    }

    private fun requestTileUpdate() {
        runCatching {
            TileService.requestListeningState(
                context,
                ComponentName(context, TemporaryRestTileService::class.java)
            )
        }
    }

    private fun notifyStateChanged(state: TemporaryRestState) {
        stateListeners.forEach { listener -> listener(state) }
    }
}
