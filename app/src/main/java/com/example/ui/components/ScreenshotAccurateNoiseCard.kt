package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ConnectionStatus
import com.example.data.model.AncSettings
import com.example.data.model.HocoDevice
import com.example.data.model.NoiseMode
import com.example.ui.theme.CleanCardBorder
import com.example.ui.theme.CleanWhiteSurface
import com.example.ui.theme.GreenBattery
import com.example.ui.theme.OrangeBattery
import com.example.ui.theme.PillBg
import com.example.ui.theme.RedBattery
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Clean white noise control card matching user reference image:
 * - "Noise Control" header
 * - 1..10 segmented level selector
 * - 3 circular mode buttons:
 *     [Transparent Mode]   [Standard Mode]   [Noise Cancellation Mode]
 */
@Composable
fun ScreenshotAccurateNoiseCard(
    ancSettings: AncSettings,
    isConnected: Boolean,
    onModeSelect: (NoiseMode) -> Unit,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // Header: "Noise Control"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Noise Control",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Level ${ancSettings.gainLevel}/10",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 10-level segmented slider (always responsive, active slot highlighted with headphone icon)
            SegmentedNoiseLevelSelector(
                currentLevel = ancSettings.gainLevel,
                enabled = isConnected,
                onLevelSelected = onLevelChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3 Mode items matching screenshot:
            // 1. Transparent Mode
            // 2. Standard Mode (Off)
            // 3. Noise Cancellation Mode (ANC)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                NoiseModeButton(
                    title = "Transparent\nMode",
                    mode = NoiseMode.TRANSPARENCY,
                    currentMode = ancSettings.currentMode,
                    enabled = isConnected,
                    onSelect = onModeSelect,
                    modifier = Modifier.weight(1f)
                )

                NoiseModeButton(
                    title = "Standard\nMode",
                    mode = NoiseMode.OFF,
                    currentMode = ancSettings.currentMode,
                    enabled = isConnected,
                    onSelect = onModeSelect,
                    modifier = Modifier.weight(1f)
                )

                NoiseModeButton(
                    title = "Noise Cancellation\nMode",
                    mode = NoiseMode.ANC,
                    currentMode = ancSettings.currentMode,
                    enabled = isConnected,
                    onSelect = onModeSelect,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Compact clean connection bar that integrates seamlessly with the white companion design
 */
@Composable
fun CompactConnectionStatusBar(
    connectionState: ConnectionStatus,
    connectedDevice: HocoDevice?,
    isScanning: Boolean,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (connectionState) {
                                ConnectionStatus.CONNECTED -> GreenBattery
                                ConnectionStatus.CONNECTING -> OrangeBattery
                                ConnectionStatus.DISCONNECTED -> Color(0xFF9CA3AF)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = when (connectionState) {
                            ConnectionStatus.CONNECTED -> connectedDevice?.name ?: "Connected"
                            ConnectionStatus.CONNECTING -> "Connecting..."
                            ConnectionStatus.DISCONNECTED -> "Device Disconnected"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = when (connectionState) {
                            ConnectionStatus.CONNECTED -> "JieLi RCSP Protocol Ready"
                            ConnectionStatus.CONNECTING -> "Handshaking..."
                            ConnectionStatus.DISCONNECTED -> "Tap 'Scan' to discover earbuds"
                        },
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connectionState == ConnectionStatus.CONNECTED) {
                    TextButton(
                        onClick = onDisconnectClick,
                        modifier = Modifier.testTag("compact_disconnect_btn")
                    ) {
                        Text("Disconnect", fontSize = 12.sp, color = RedBattery)
                    }
                } else {
                    Button(
                        onClick = onScanClick,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E2024),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("compact_scan_btn")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Color.White,
                                strokeWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scanning", fontSize = 11.sp)
                        } else {
                            Text("Scan", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
