package com.example.data.model

import com.jieli.bluetooth.bean.base.VoiceMode

enum class NoiseMode(val modeId: Int, val displayName: String) {
    OFF(VoiceMode.VOICE_MODE_CLOSE, "Standard"),
    ANC(VoiceMode.VOICE_MODE_DENOISE, "Noise Cancellation"),
    TRANSPARENCY(VoiceMode.VOICE_MODE_TRANSPARENT, "Transparency");

    companion object {
        fun fromModeId(id: Int): NoiseMode {
            return entries.firstOrNull { it.modeId == id } ?: OFF
        }
    }
}

/**
 * ANC settings with read-back verification tracking.
 *
 * SAFETY: The UI should use lastConfirmedMode/lastConfirmedLevel to display
 * the actual device state, NOT the requested values. The requested values
 * are only used to show a "Verifying..." state until the device confirms.
 */
data class AncSettings(
    val currentMode: NoiseMode = NoiseMode.OFF,
    val gainLevel: Int = 5,
    val maxGain: Int = 10,
    val rawLeftCurVal: Int = 0,
    val rawLeftMax: Int = 10,
    val isSupported: Boolean = true,
    /** Last mode confirmed by device via read-back */
    val lastConfirmedMode: NoiseMode = NoiseMode.OFF,
    /** Last level confirmed by device via read-back */
    val lastConfirmedLevel: Int = 5,
    /** Whether we are waiting for device confirmation after a write */
    val isPendingVerification: Boolean = false,
    /** Requested mode waiting for confirmation */
    val requestedMode: NoiseMode? = null,
    /** Requested level waiting for confirmation */
    val requestedLevel: Int? = null,
)
