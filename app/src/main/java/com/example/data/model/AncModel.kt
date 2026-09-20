package com.example.data.model

import com.jieli.bluetooth.bean.base.VoiceMode

enum class NoiseMode(val modeId: Int, val displayName: String) {
    OFF(VoiceMode.VOICE_MODE_CLOSE, "Normal / Off"),
    ANC(VoiceMode.VOICE_MODE_DENOISE, "Noise Cancellation"),
    TRANSPARENCY(VoiceMode.VOICE_MODE_TRANSPARENT, "Transparency");

    companion object {
        fun fromModeId(id: Int): NoiseMode {
            return entries.firstOrNull { it.modeId == id } ?: OFF
        }
    }
}

data class AncSettings(
    val currentMode: NoiseMode = NoiseMode.OFF,
    val gainLevel: Int = 5, // 1 to 10 user scale
    val maxGain: Int = 10,
    val rawLeftCurVal: Int = 0,
    val rawLeftMax: Int = 10,
    val isSupported: Boolean = true
)
