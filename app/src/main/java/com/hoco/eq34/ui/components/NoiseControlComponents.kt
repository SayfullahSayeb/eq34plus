@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

@Composable
fun SegmentedNoiseLevelSelector(
    noiseState: NoiseControlState,
    enabled: Boolean,
    onProgressSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProgress = noiseState.uiProgress
    var sliderValue by remember(currentProgress) { mutableFloatStateOf(currentProgress.toFloat()) }

    Column(modifier = modifier.fillMaxWidth()) {
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

        // Segmented bar + slider overlay (tap + drag)
        Box(modifier = Modifier.fillMaxWidth()) {
            // Visual segments (tap targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..10) {
                    val isActive = i <= currentProgress
                    val segColor = if (isActive) Color(0xFF4B5563) else Color(0xFFD1D5DB)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(segColor)
                            .clickable(enabled = enabled) { onProgressSelected(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (i == currentProgress) {
                            when {
                                i <= 4 -> TransparencyMiniIcon(tint = Color.White)
                                i == 5 -> StandardMiniIcon(tint = Color.White)
                                else -> AncMiniIcon(tint = Color.White)
                            }
                        } else {
                            Text(
                                text = i.toString(),
                                color = if (isActive) Color.White.copy(alpha = 0.7f) else Color(0xFF6B7280),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (i < 10) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(Color(0xFFD1D5DB))
                        )
                    }
                }
            }

            // Draggable slider overlay (drag only)
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    val snapped = sliderValue.toInt().coerceIn(0, 10)
                    onProgressSelected(snapped)
                },
                valueRange = 0f..10f,
                steps = 9,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF1F2024),
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2024)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            currentProgress <= 4 -> TransparencyMiniIcon(tint = Color.White, modifier = Modifier.size(14.dp))
                            currentProgress == 5 -> StandardMiniIcon(tint = Color.White, modifier = Modifier.size(14.dp))
                            else -> AncMiniIcon(tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            )
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
fun TransparencyMiniIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.then(Modifier.size(16.dp))) {
        drawCircle(color = tint, radius = size.width * 0.18f, center = Offset(size.width / 2f, size.height * 0.36f))
        drawArc(color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(size.width * 0.18f, size.height * 0.56f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.40f))
        drawCircle(color = tint, radius = size.width * 0.10f, center = Offset(size.width * 0.20f, size.height * 0.36f))
        drawCircle(color = tint, radius = size.width * 0.10f, center = Offset(size.width * 0.80f, size.height * 0.36f))
    }
}

@Composable
fun StandardMiniIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.then(Modifier.size(16.dp))) {
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
fun AncMiniIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.then(Modifier.size(16.dp))) {
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
