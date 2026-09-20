package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoiseControlPositions
import com.example.data.model.NoiseControlState
import com.example.data.model.NoiseMode
import com.example.ui.theme.SegmentedDark
import com.example.ui.theme.SelectedDarkIcon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnselectedLightIcon

/**
 * Official HOCO EQ34 Plus noise control progress bar (0-10).
 *
 * From hoco.music_1.3.5-gp.apks (ii8.java):
 *   Progress:  0   1   2   3   4   [5]   6   7   8   9   10
 *   Mode:      TRANSPARENCY       STANDARD    ANC
 *   Level:     4→0 (strong→weak)  N/A         0→4 (weak→strong)
 */
@Composable
fun SegmentedNoiseLevelSelector(
    noiseState: NoiseControlState,
    enabled: Boolean,
    onProgressSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProgress = noiseState.uiProgress

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SegmentedDark)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag("segmented_noise_level_selector")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress 0-4 (Transparency range)
            for (progress in 0..4) {
                val isSelected = (progress == currentProgress)
                val modeColor = if (isSelected) Color(0xFF3B82F6) else Color(0xFFC4C7CF)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(enabled = enabled) { onProgressSelected(progress) }
                        .then(
                            if (isSelected) {
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1F2024))
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        TransparencyMiniIcon(tint = modeColor)
                    } else {
                        Text(
                            text = progress.toString(),
                            color = Color(0xFFC4C7CF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Vertical separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color(0xFF3F4048))
                )
            }

            // Center: Progress 5 (Standard mode)
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(44.dp)
                    .clickable(enabled = enabled) { onProgressSelected(NoiseControlPositions.STANDARD_PROGRESS) }
                    .then(
                        if (currentProgress == NoiseControlPositions.STANDARD_PROGRESS) {
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1F2024))
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (currentProgress == NoiseControlPositions.STANDARD_PROGRESS) {
                    StandardMiniIcon(tint = Color.White)
                } else {
                    StandardMiniIcon(tint = Color(0xFFC4C7CF))
                }
            }

            // Vertical separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Color(0xFF3F4048))
            )

            // Progress 6-10 (ANC range)
            for (progress in 6..10) {
                val isSelected = (progress == currentProgress)
                val modeColor = if (isSelected) Color(0xFF10B981) else Color(0xFFC4C7CF)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(enabled = enabled) { onProgressSelected(progress) }
                        .then(
                            if (isSelected) {
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1F2024))
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        AncMiniIcon(tint = modeColor)
                    } else {
                        Text(
                            text = progress.toString(),
                            color = Color(0xFFC4C7CF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Vertical separator between segments
                if (progress < 10) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color(0xFF3F4048))
                    )
                }
            }
        }
    }
}

/**
 * Noise Control Mode Item with round icon and label:
 * - Transparent Mode
 * - Standard Mode
 * - Noise Cancellation Mode
 *
 * Official mapping from c48.java:
 * - Off button → progress 5 (Standard)
 * - ANC button → progress 10 (max ANC)
 * - Transparency button → progress 0 (max Transparency)
 */
@Composable
fun NoiseModeButton(
    title: String,
    mode: NoiseMode,
    currentMode: NoiseMode,
    enabled: Boolean,
    onSelect: (NoiseMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = (mode == currentMode)

    Column(
        modifier = modifier
            .clickable(enabled = enabled) { onSelect(mode) }
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("mode_${mode.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circle Button
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (isSelected) SelectedDarkIcon else UnselectedLightIcon)
                .then(
                    if (!isSelected) {
                        Modifier.border(1.dp, Color(0xFFE5E7EB), CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                NoiseMode.TRANSPARENCY -> {
                    TransparencyCustomIcon(tint = if (isSelected) Color.White else Color(0xFF9E9EA7))
                }
                NoiseMode.STANDARD -> {
                    StandardCustomIcon(tint = if (isSelected) Color.White else Color(0xFF9E9EA7))
                }
                NoiseMode.ANC -> {
                    AncCustomIcon(tint = if (isSelected) Color.White else Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}

// ==================== MINI ICONS FOR SEGMENTED BAR ====================

@Composable
fun TransparencyMiniIcon(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        drawCircle(
            color = tint,
            radius = size.width * 0.18f,
            center = Offset(size.width / 2f, size.height * 0.36f)
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.18f, size.height * 0.56f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.40f)
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.10f,
            center = Offset(size.width * 0.20f, size.height * 0.36f)
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.10f,
            center = Offset(size.width * 0.80f, size.height * 0.36f)
        )
    }
}

@Composable
fun StandardMiniIcon(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        drawCircle(
            color = tint,
            radius = size.width * 0.17f,
            center = Offset(size.width / 2f, size.height * 0.38f)
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.14f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.74f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
    }
}

@Composable
fun AncMiniIcon(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.60f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.16f,
            center = Offset(size.width / 2f, size.height * 0.38f)
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.14f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.74f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
    }
}

// ==================== FULL SIZE ICONS FOR MODE BUTTONS ====================

@Composable
fun TransparencyCustomIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = tint,
            radius = size.width * 0.16f,
            center = Offset(size.width / 2f, size.height * 0.38f)
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.22f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.35f)
        )
        val radiusOuter = size.width * 0.44f
        for (angle in listOf(-140f, -110f, -70f, -40f, 0f, 40f, 70f, 110f, 140f, 180f)) {
            val rad = Math.toRadians(angle.toDouble())
            val x = (centerOffset.x + radiusOuter * Math.cos(rad)).toFloat()
            val y = (centerOffset.y + radiusOuter * Math.sin(rad)).toFloat()
            drawCircle(
                color = tint.copy(alpha = 0.8f),
                radius = 1.3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun StandardCustomIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        drawCircle(
            color = tint,
            radius = size.width * 0.17f,
            center = Offset(size.width / 2f, size.height * 0.38f)
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.14f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.74f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
    }
}

@Composable
fun AncCustomIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.60f),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.16f,
            center = Offset(size.width / 2f, size.height * 0.38f)
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.14f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.74f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
    }
}
