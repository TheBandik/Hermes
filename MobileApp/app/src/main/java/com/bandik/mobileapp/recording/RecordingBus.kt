package com.bandik.mobileapp.recording

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LiveStats(
    val isRecording: Boolean = false,
    val secondsLeft: Int = 0,
    val eventsLast2s: Int = 0,
    val eventsPerSec: Double = 0.0,
    val uniqueBeaconsLast2s: Int = 0,
    val isReceiving: Boolean = false,
    val outputFileName: String? = null,
    val lastError: String? = null
)

object RecordingBus {
    private val _stats = MutableStateFlow(LiveStats())
    val stats: StateFlow<LiveStats> = _stats

    fun update(value: LiveStats) {
        _stats.value = value
    }

    fun error(message: String) {
        _stats.value = _stats.value.copy(lastError = message)
    }

    fun reset() {
        _stats.value = LiveStats()
    }
}
