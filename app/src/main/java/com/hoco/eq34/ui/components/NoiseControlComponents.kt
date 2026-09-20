package com.hoco.eq34.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoco.eq34.data.model.NoiseControlPositions
import com.hoco.eq34.data.model.NoiseControlState
import com.hoco.eq34.data.model.NoiseMode
import com.hoco.eq34.ui.theme.SelectedDarkIcon
import com.hoco.eq34.ui.theme.TextPrimary
import com.hoco.eq34.ui.theme.TextSecondary
import com.hoco.eq34.ui.theme.UnselectedLightIcon

/**
 * Segmented noise level bar matching the original HOCO app.
 * 11 segments: 0-4 (Transparency), 5 (Standard), 6-10 (ANC)
 * Selected segment shows icon, unselected shows number.
 */
@Composable
fun SegmentedNoiseLevelSelector(
    noiseState: NoiseControlState,
    enabled: Boolean,
    onProgressSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProgress = noiseState.uiProgress

    Column(modifier = modifier.fillMaxWidth()) {
        // Current mode label + level
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    currentProgress <= 4 -> "Transparency"
                    currentProgress == 5 -> "Standard"
                    else -> "ANC"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "$currentProgress/10",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1F2024))
                .padding(vertical = 6.dp, horizontal = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress 0-4 (Transparency range)
                for (progress in 0..4) {
                    val isActive = (progress <= currentProgress)
                    val isSelected = (progress == currentProgress)
                    val segmentColor = when {
                        isSelected -> Color(0xFF1F2024)
                        isActive -> Color(0xFF3F4048)
                        else -> Color(0xFFF3F4F6)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(segmentColor)
                            .clickable(enabled = enabled) { onProgressSelected(progress) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            when {
                                currentProgress <= 4 -> TransparencyMiniIcon(tint = Color.White)
                                currentProgress == 5 -> StandardMiniIcon(tint = Color.White)
                                else -> AncMiniIcon(tint = Color.White)
                            }
                        } else {
                            Text(
                                text = progress.toString(),
                                color = if (isActive) Color.White.copy(alpha = 0.7f) else Color(0xFF9CA3AF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Vertical separator
                    if (progress < 4) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(Color(0xFFD1D5DB))
                        )
                    }
                }

                // Center: Progress 5 (Standard mode)
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                currentProgress == 5 -> Color(0xFF1F2024)
                                currentProgress > 5 -> Color(0xFF3F4048)
                                else -> Color(0xFFF3F4F6)
                            }
                        )
                        .clickable(enabled = enabled) { onProgressSelected(NoiseControlPositions.STANDARD_PROGRESS) },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentProgress == 5) {
                        StandardMiniIcon(tint = Color.White)
                    } else {
                        StandardMiniIcon(tint = if (currentProgress > 5) Color.White.copy(alpha = 0.7f) else Color(0xFF9CA3AF))
                    }
                }

                // Vertical separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFFD1D5DB))
                )

                // Progress 6-10 (ANC range)
                for (progress in 6..10) {
                    val isActive = (progress <= currentProgress)
                    val isSelected = (progress == currentProgress)
                    val segmentColor = when {
                        isSelected -> Color(0xFF1F2024)
                        isActive -> Color(0xFF3F4048)
                        else -> Color(0xFFF3F4F6)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(segmentColor)
                            .clickable(enabled = enabled) { onProgressSelected(progress) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            AncMiniIcon(tint = Color.White)
                        } else {
                            Text(
                                text = progress.toString(),
                                color = if (isActive) Color.White.copy(alpha = 0.7f) else Color(0xFF9CA3AF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (progress < 10) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(Color(0xFFD1D5DB))
                        )
                    }
                }
            }
        }
    }
}

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
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (isSelected) SelectedDarkIcon else UnselectedLightIcon)
                .then(
                    if (!isSelected) Modifier.border(1.dp, Color(0xFFE5E7EB), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                NoiseMode.TRANSPARENCY -> TransparencyCustomIcon(tint = if (isSelected) Color.White else Color(0xFF9E9EA7))
                NoiseMode.STANDARD -> StandardCustomIcon(tint = if (isSelected) Color.White else Color(0xFF9E9EA7))
                NoiseMode.ANC -> AncCustomIcon(tint = if (isSelected) Color.White else Color(0xFF9E9EA7))
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
        drawCircle(color = tint, radius = size.width * 0.18f, center = Offset(size.width / 2f, size.height * 0.36f))
        drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(size.width * 0.18f, size.height * 0.56f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.40f))
        drawCircle(color = tint, radius = size.width * 0.10f, center = Offset(size.width * 0.20f, size.height * 0.36f))
        drawCircle(color = tint, radius = size.width * 0.10f, center = Offset(size.width * 0.80f, size.height * 0.36f))
    }
}

@Composable
fun StandardMiniIcon(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        drawCircle(color = tint, radius = size.width * 0.17f, center = Offset(size.width / 2f, size.height * 0.38f))
        drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f))
        drawRoundRect(color = tint, topLeft = Offset(size.width * 0.14f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        drawRoundRect(color = tint, topLeft = Offset(size.width * 0.74f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
    }
}

@Composable
fun AncMiniIcon(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.60f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = tint, radius = size.width * 0.16f, center = Offset(size.width / 2f, size.height * 0.38f))
        drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f))
        drawRoundRect(color = tint, topLeft = Offset(size.width * 0.14f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        drawRoundRect(color = tint, topLeft = Offset(size.width * 0.74f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
    }
}

// ==================== FULL SIZE ICONS FOR MODE BUTTONS ====================

@Composable
fun TransparencyCustomIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        drawCircle(color = tint, radius = size.width * 0.16f, center = Offset(size.width / 2f, size.height * 0.38f))
        drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(size.width * 0.22f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.35f))
    }
}

@Composable
fun StandardCustomIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        drawCircle(color = tint, radius = size.width * 0.17f, center = Offset(size.width / 2f, size.height * 0.38f))
        drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f))
    }
}

@Composable
fun AncCustomIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.60f),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = tint, radius = size.width * 0.16f, center = Offset(size.width / 2f, size.height * 0.38f))
        drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.36f))
    }
}
