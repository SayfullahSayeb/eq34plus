package com.hoco.eq34.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoco.eq34.R
import com.hoco.eq34.data.ConnectionStatus
import com.hoco.eq34.data.model.BatteryInfoModel
import com.hoco.eq34.ui.theme.CleanWhiteSurface
import com.hoco.eq34.ui.theme.IconSubtleGray
import com.hoco.eq34.ui.theme.PillBg
import com.hoco.eq34.ui.theme.TextInactive
import com.hoco.eq34.ui.theme.TextPrimary
import com.hoco.eq34.ui.theme.TextSecondary

/**
 * Top App Bar matching screenshot:
 * [< Back Arrow]       "HOCO EQ34 Plus ANC"       [Hexagon Settings Nut Icon]
 */
@Composable
fun HocoTopBar(
    title: String,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onTitleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("top_bar_back_btn")
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .then(if (onTitleClick != null) Modifier.clickable { onTitleClick() } else Modifier)
        )

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.testTag("top_bar_settings_btn")
        ) {
            HexagonNutIcon(
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Hexagonal nut settings icon as shown on the top-right of the screenshot
 */
@Composable
fun HexagonNutIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.44f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Draw hexagon path
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0 until 6) {
            val angle = Math.toRadians((60 * i - 30).toDouble())
            val x = (center.x + radius * Math.cos(angle)).toFloat()
            val y = (center.y + radius * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Inner circle
        drawCircle(
            color = tint,
            radius = radius * 0.46f,
            center = center,
            style = Stroke(width = 2.2.dp.toPx())
        )
    }
}

/**
 * Earbuds Hero Display with Left and Right battery pill indicators:
 * [L 100%]                           [R 100%]
 */
@Composable
fun EarbudsHeroDisplay(
    batteryState: BatteryInfoModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // EQ34 Plus earbuds with case image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.eq34_case),
                contentDescription = "HOCO EQ34 Plus",
                modifier = Modifier
                    .height(250.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Left & Right Battery Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EarbudBatteryPill(
                side = "L",
                percentage = if (batteryState.leftBattery >= 0) batteryState.leftBattery else 0
            )

            EarbudBatteryPill(
                side = "R",
                percentage = if (batteryState.rightBattery >= 0) batteryState.rightBattery else 0
            )
        }

        if (batteryState.caseBattery > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            EarbudBatteryPill(
                side = "Case",
                percentage = batteryState.caseBattery
            )
        }
    }
}

@Composable
fun EarbudBatteryPill(
    side: String,
    percentage: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(PillBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = side,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Text(
            text = "$percentage%",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = TextPrimary
        )
    }
}

/**
 * Middle function tabs row matching screenshot:
 * [Sound wave icon (with active underline)]    [Equalizer slider icon]    [Touch gesture hand icon]
 */
@Composable
fun SubFeatureTabsRow(
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 0: Noise cancellation wave
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onTabSelected(0) }
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            SoundWaveCircleIcon(
                isSelected = selectedTabIndex == 0,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (selectedTabIndex == 0) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black)
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }

        // Tab 1: Equalizer sliders
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onTabSelected(1) }
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            EqualizerSlidersIcon(
                isSelected = selectedTabIndex == 1,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (selectedTabIndex == 1) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black)
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }

        // Tab 2: Touch Control finger icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onTabSelected(2) }
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            TouchFingerIcon(
                isSelected = selectedTabIndex == 2,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (selectedTabIndex == 2) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black)
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

/**
 * Sound wave inside circle icon
 */
@Composable
fun SoundWaveCircleIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) Color.Black else IconSubtleGray
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.44f

        drawCircle(
            color = tint,
            radius = r,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // 5 vertical sound bars inside
        val barHeights = listOf(0.24f, 0.45f, 0.60f, 0.45f, 0.24f)
        val spacing = size.width * 0.11f
        val startX = center.x - (2 * spacing)

        barHeights.forEachIndexed { index, heightFactor ->
            val x = startX + index * spacing
            val barH = size.height * heightFactor
            drawLine(
                color = tint,
                start = Offset(x, center.y - barH / 2f),
                end = Offset(x, center.y + barH / 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Equalizer triple vertical sliders icon
 */
@Composable
fun EqualizerSlidersIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) Color.Black else IconSubtleGray
    Canvas(modifier = modifier) {
        val xPositions = listOf(size.width * 0.24f, size.width * 0.50f, size.width * 0.76f)
        val knobY = listOf(size.height * 0.35f, size.height * 0.68f, size.height * 0.42f)

        xPositions.forEachIndexed { i, x ->
            drawLine(
                color = tint,
                start = Offset(x, size.height * 0.18f),
                end = Offset(x, size.height * 0.82f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // circle knob
            drawCircle(
                color = CleanWhiteSurface,
                radius = 3.6.dp.toPx(),
                center = Offset(x, knobY[i])
            )
            drawCircle(
                color = tint,
                radius = 3.6.dp.toPx(),
                center = Offset(x, knobY[i]),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Touch gesture finger icon
 */
@Composable
fun TouchFingerIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) Color.Black else IconSubtleGray
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)

        // Concentric touch ripples
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.25f, size.height * 0.12f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.50f, size.height * 0.50f),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Stylized hand/finger pointing up
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x, size.height * 0.28f)
            lineTo(center.x, size.height * 0.65f)
            // palm loop
            cubicTo(
                center.x - 12.dp.toPx(), size.height * 0.72f,
                center.x - 10.dp.toPx(), size.height * 0.90f,
                center.x, size.height * 0.90f
            )
            cubicTo(
                center.x + 10.dp.toPx(), size.height * 0.90f,
                center.x + 12.dp.toPx(), size.height * 0.72f,
                center.x, size.height * 0.65f
            )
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Bottom Navigation Bar matching screenshot:
 * [Home (Active solid black)]     [Music (Inactive grey)]     [Mine (Inactive grey)]
 */
@Composable
fun HocoBottomNavBar(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CleanWhiteSurface)
            .border(width = 1.dp, color = Color(0xFFF0F1F4))
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Filled.Home,
                label = "Home",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )

            BottomNavItem(
                icon = Icons.Outlined.Folder,
                label = "Music",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )

            BottomNavItem(
                icon = Icons.Outlined.Person,
                label = "Mine",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) TextPrimary else TextInactive,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextInactive
        )
    }
}

