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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var sliderValue by remember { mutableFloatStateOf(noiseState.uiProgress.toFloat()) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    noiseState.uiProgress <= 4 -> "Transparency"
                    noiseState.uiProgress == 5 -> "Standard"
                    else -> "ANC"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    noiseState.uiProgress <= 4 -> Color(0xFF3B82F6)
                    noiseState.uiProgress == 5 -> TextSecondary
                    else -> Color(0xFF10B981)
                }
            )
            Text(
                text = "${noiseState.uiProgress}/10",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onProgressSelected(sliderValue.toInt()) },
            valueRange = 0f..10f,
            steps = 9,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = when {
                    sliderValue <= 4 -> Color(0xFF3B82F6)
                    sliderValue == 5f -> TextSecondary
                    else -> Color(0xFF10B981)
                },
                activeTrackColor = when {
                    sliderValue <= 4 -> Color(0xFF3B82F6)
                    sliderValue == 5f -> Color(0xFF6B7280)
                    else -> Color(0xFF10B981)
                },
                inactiveTrackColor = Color(0xFFE5E7EB)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Transparency", fontSize = 10.sp, color = Color(0xFF3B82F6))
            Text("Standard", fontSize = 10.sp, color = TextSecondary)
            Text("ANC", fontSize = 10.sp, color = Color(0xFF10B981))
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
                NoiseMode.ANC -> AncCustomIcon(tint = Color.White)
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
