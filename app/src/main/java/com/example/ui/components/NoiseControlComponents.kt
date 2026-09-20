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
import com.example.data.model.NoiseMode
import com.example.ui.theme.SegmentedDark
import com.example.ui.theme.SelectedDarkIcon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UnselectedLightIcon

/**
 * 10-level segmented level slider matching the user screenshot:
 * A dark bar with 10 columns separated by dividers.
 * The active/selected level is highlighted with an icon/box.
 */
@Composable
fun SegmentedNoiseLevelSelector(
    currentLevel: Int,
    enabled: Boolean,
    onLevelSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalLevels = 10
    val activeLevel = currentLevel.coerceIn(1, totalLevels)

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
            for (level in 1..totalLevels) {
                val isSelected = (level == activeLevel)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(enabled = enabled) { onLevelSelected(level) }
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
                        // Headphone / person icon inside active slot
                        NoiseIndicatorMiniIcon(tint = Color.White)
                    } else {
                        Text(
                            text = level.toString(),
                            color = Color(0xFFC4C7CF),
                            fontSize = 13.sp,
                            fontWeight = if (level == 10) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Vertical separator between segments
                if (level < totalLevels) {
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
 * Noise Control Mode Item with round icon and label matching user screenshot:
 * - Transparent Mode
 * - Standard Mode (Off)
 * - Noise Cancellation Mode (ANC)
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
                NoiseMode.OFF -> {
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

/**
 * Headphone user icon with concentric waves (Transparency)
 */
@Composable
fun TransparencyCustomIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val centerOffset = Offset(size.width / 2f, size.height / 2f)

        // Head/user silhouette
        drawCircle(
            color = tint,
            radius = size.width * 0.16f,
            center = Offset(size.width / 2f, size.height * 0.38f)
        )
        // Shoulders / chest arc
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.22f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.35f)
        )

        // Dotted / sound transmission rays
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

/**
 * Standard mode: Person with side headphone earcups (Standard Mode)
 */
@Composable
fun StandardCustomIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        // Head
        drawCircle(
            color = tint,
            radius = size.width * 0.17f,
            center = Offset(size.width / 2f, size.height * 0.38f)
        )
        // Shoulders
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f)
        )
        // Left & Right Earcups
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

/**
 * Active ANC mode: Solid Person wearing over-ear headset with headband
 */
@Composable
fun AncCustomIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        // Headband arch
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.60f),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Head
        drawCircle(
            color = tint,
            radius = size.width * 0.16f,
            center = Offset(size.width / 2f, size.height * 0.38f)
        )
        // Shoulders
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f)
        )

        // Left earcup
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.14f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        // Right earcup
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.74f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
    }
}

/**
 * Mini headphone user icon for the segmented level bar
 */
@Composable
fun NoiseIndicatorMiniIcon(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        // Head
        drawCircle(
            color = tint,
            radius = size.width * 0.18f,
            center = Offset(size.width / 2f, size.height * 0.36f)
        )
        // Shoulders
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.18f, size.height * 0.56f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.40f)
        )
        // Left & Right small ear cushions
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
