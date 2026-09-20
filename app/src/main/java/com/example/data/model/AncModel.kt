package com.example.data.model

import com.jieli.bluetooth.bean.base.VoiceMode

/**
 * Noise control mode matching the official HOCO EQ34 Plus app.
 *
 * Constants confirmed from JieLi RCSP SDK VoiceMode.java:
 */
enum class NoiseMode(val modeId: Int, val displayName: String) {
    STANDARD(VoiceMode.VOICE_MODE_CLOSE, "Standard"),
    ANC(VoiceMode.VOICE_MODE_DENOISE, "Noise Cancellation"),
    TRANSPARENCY(VoiceMode.VOICE_MODE_TRANSPARENT, "Transparency");

    companion object {
        fun fromModeId(id: Int): NoiseMode {
            return entries.firstOrNull { it.modeId == id } ?: STANDARD
        }
    }
}

/**
 * Official HOCO EQ34 Plus noise control mapping (extracted from d78.java in hoco.music_1.3.5-gp.apks).
 *
 * The progress bar has 11 positions (0-10):
 *
 *   Progress:  0   1   2   3   4   [5]   6   7   8   9   10
 *   Mode:      TRANSPARENCY       STANDARD    ANC
 *
 * Device VoiceMode payload (9 bytes):
 *   byte[0]   = mode (0=Standard, 1=ANC, 2=Transparency)
 *   byte[1-2] = leftMax (from device's voice mode list)
 *   byte[3-4] = rightMax
 *   byte[5-6] = leftCurVal (level relative to leftMax)
 *   byte[7-8] = rightCurVal
 *
 * Official level calculation from d78.java setAncModelByProgress:
 *   step = leftMax / 5
 *   ANC (progress 6-10):     level = (progress-6) * step + step/2
 *   Transparency (0-4):      level = leftMax - (progress * step + step/2)
 *   Standard (5):            level = 0 (mode-only command)
 *
 * Example with leftMax=10, step=2:
 *   Progress 0:  Transp level=9    Progress 6:  ANC level=1
 *   Progress 1:  Transp level=7    Progress 7:  ANC level=3
 *   Progress 2:  Transp level=5    Progress 8:  ANC level=5
 *   Progress 3:  Transp level=3    Progress 9:  ANC level=7
 *   Progress 4:  Transp level=1    Progress 10: ANC level=9
 */
object NoiseControlPositions {
    /** Official progress range: 0-10 */
    const val MIN_PROGRESS = 0
    const val MAX_PROGRESS = 10
    const val STANDARD_PROGRESS = 5

    /** Range boundaries */
    const val TRANSPARENCY_MIN = 0
    const val TRANSPARENCY_MAX = 4
    const val ANC_MIN = 6
    const val ANC_MAX = 10

    fun isValidProgress(progress: Int): Boolean {
        return progress in MIN_PROGRESS..MAX_PROGRESS
    }

    /** Map UI progress to NoiseMode */
    fun modeForProgress(progress: Int): NoiseMode {
        return when {
            progress in TRANSPARENCY_MIN..TRANSPARENCY_MAX -> NoiseMode.TRANSPARENCY
            progress == STANDARD_PROGRESS -> NoiseMode.STANDARD
            progress in ANC_MIN..ANC_MAX -> NoiseMode.ANC
            else -> NoiseMode.STANDARD
        }
    }

    /**
     * Map UI progress to the actual device leftCurVal/rightCurVal.
     *
     * Matches d78.java setAncModelByProgress exactly:
     *   step = leftMax / 5
     *   ANC:     level = (progress - 6) * step + step / 2
     *   Transp:  level = leftMax - (progress * step + step / 2)
     *   Standard: level = 0 (unused, setAncModelById sends mode-only)
     */
    fun deviceLevelForProgress(progress: Int, leftMax: Int): Int {
        if (leftMax <= 0) return 0
        val step = leftMax / 5
        if (step <= 0) return 0

        return when {
            progress in ANC_MIN..ANC_MAX -> {
                val adjustedProgress = progress - 6
                (adjustedProgress * step) + (step / 2)
            }
            progress in TRANSPARENCY_MIN..TRANSPARENCY_MAX -> {
                leftMax - ((progress * step) + (step / 2))
            }
            else -> 0
        }
    }

    /**
     * Convert device mode + leftCurVal back to UI progress.
     *
     * Reverse of deviceLevelForProgress:
     *   ANC:     progress = (level - step/2) / step + 6
     *   Transp:  progress = (leftMax - level - step/2) / step
     *   Standard: progress = 5
     */
    fun progressForModeAndLevel(mode: NoiseMode, leftCurVal: Int, leftMax: Int): Int {
        if (leftMax <= 0) return STANDARD_PROGRESS
        val step = leftMax / 5
        if (step <= 0) return STANDARD_PROGRESS

        return when (mode) {
            NoiseMode.ANC -> {
                val raw = ((leftCurVal - step / 2) / step) + 6
                raw.coerceIn(ANC_MIN, ANC_MAX)
            }
            NoiseMode.TRANSPARENCY -> {
                val raw = (leftMax - leftCurVal - step / 2) / step
                raw.coerceIn(TRANSPARENCY_MIN, TRANSPARENCY_MAX)
            }
            NoiseMode.STANDARD -> STANDARD_PROGRESS
        }
    }
}

/**
 * Single source of truth for noise control state.
 *
 * The UI must be rendered ENTIRELY from this state.
 * No separate UI variables that could become inconsistent.
 */
data class NoiseControlState(
    /** Current active mode (read from device) */
    val mode: NoiseMode = NoiseMode.STANDARD,
    /** Internal level within the current mode (0-4 for ANC/Transparency, 0 for Standard) */
    val internalLevel: Int = 0,
    /** UI progress position (0-10) — computed from mode + level */
    val uiProgress: Int = NoiseControlPositions.STANDARD_PROGRESS,
    /** Maximum internal level supported by the device (read via getAllVoiceModes) */
    val maxInternalLevel: Int = 4,
    /** Whether the device has confirmed this state via read-back */
    val confirmedByDevice: Boolean = false,
    /** Whether we are waiting for device confirmation after a write */
    val isPendingVerification: Boolean = false,
    /** The target progress we are waiting confirmation for (null = none) */
    val pendingProgress: Int? = null,
    /** Last confirmed mode from device read-back */
    val confirmedMode: NoiseMode = NoiseMode.STANDARD,
    /** Last confirmed internal level from device read-back */
    val confirmedInternalLevel: Int = 0,
) {
    /**
     * Build from device VoiceMode callback data.
     * This is the ONLY way to update confirmed state.
     */
    fun confirmFromDevice(mode: NoiseMode, leftCurVal: Int, maxLevel: Int): NoiseControlState {
        val progress = NoiseControlPositions.progressForModeAndLevel(mode, leftCurVal, maxLevel)
        return copy(
            mode = mode,
            internalLevel = leftCurVal,
            uiProgress = progress,
            maxInternalLevel = maxLevel,
            confirmedByDevice = true,
            isPendingVerification = false,
            pendingProgress = null,
            confirmedMode = mode,
            confirmedInternalLevel = leftCurVal,
        )
    }

    /**
     * Called when user taps a progress position. Sets pending state.
     * Does NOT change confirmed state — only marks what we're waiting for.
     */
    fun requestProgress(progress: Int): NoiseControlState {
        return copy(
            isPendingVerification = true,
            pendingProgress = progress,
        )
    }
}
