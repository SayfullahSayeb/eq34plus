package com.hoco.eq34.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoco.eq34.data.ConnectionStatus
import com.hoco.eq34.data.model.NoiseControlState
import com.hoco.eq34.data.model.NoiseMode
import com.hoco.eq34.data.model.HocoDevice
import com.hoco.eq34.ui.theme.CleanCardBorder
import com.hoco.eq34.ui.theme.CleanWhiteSurface
import com.hoco.eq34.ui.theme.GreenBattery
import com.hoco.eq34.ui.theme.OrangeBattery
import com.hoco.eq34.ui.theme.RedBattery
import com.hoco.eq34.ui.theme.TextPrimary
import com.hoco.eq34.ui.theme.TextSecondary

@Composable
fun ScreenshotAccurateNoiseCard(
    noiseState: NoiseControlState,
    isConnected: Boolean,
    onProgressSelected: (Int) -> Unit,
    onModeSelect: (NoiseMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Noise Control",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            SegmentedNoiseLevelSelector(
                noiseState = noiseState,
                enabled = isConnected,
                onProgressSelected = onProgressSelected
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                NoiseModeButton(
                    title = "Transparent\nMode",
                    mode = NoiseMode.TRANSPARENCY,
                    currentMode = noiseState.mode,
                    enabled = isConnected,
                    onSelect = onModeSelect,
                    modifier = Modifier.weight(1f)
                )

                NoiseModeButton(
                    title = "Standard\nMode",
                    mode = NoiseMode.STANDARD,
                    currentMode = noiseState.mode,
                    enabled = isConnected,
                    onSelect = onModeSelect,
                    modifier = Modifier.weight(1f)
                )

                NoiseModeButton(
                    title = "Noise Cancellation\nMode",
                    mode = NoiseMode.ANC,
                    currentMode = noiseState.mode,
                    enabled = isConnected,
                    onSelect = onModeSelect,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

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
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder)
        )
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
                                ConnectionStatus.READY -> GreenBattery
                                ConnectionStatus.CONNECTED -> OrangeBattery
                                ConnectionStatus.IDENTIFYING -> OrangeBattery
                                ConnectionStatus.CONNECTING -> OrangeBattery
                                ConnectionStatus.DISCONNECTED -> Color(0xFF9CA3AF)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = when (connectionState) {
                            ConnectionStatus.READY -> connectedDevice?.name ?: "HOCO EQ34 Plus"
                            ConnectionStatus.CONNECTED -> connectedDevice?.name ?: "Connected"
                            ConnectionStatus.IDENTIFYING -> connectedDevice?.name ?: "Identifying..."
                            ConnectionStatus.CONNECTING -> "Connecting..."
                            ConnectionStatus.DISCONNECTED -> "Device Disconnected"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = when (connectionState) {
                            ConnectionStatus.READY -> "Verified & Ready"
                            ConnectionStatus.CONNECTED -> "Identifying device..."
                            ConnectionStatus.IDENTIFYING -> "Reading device info..."
                            ConnectionStatus.CONNECTING -> "Handshaking..."
                            ConnectionStatus.DISCONNECTED -> "Tap 'Scan' to discover earbuds"
                        },
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connectionState == ConnectionStatus.CONNECTED ||
                    connectionState == ConnectionStatus.READY ||
                    connectionState == ConnectionStatus.IDENTIFYING ||
                    connectionState == ConnectionStatus.CONNECTING
                ) {
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

