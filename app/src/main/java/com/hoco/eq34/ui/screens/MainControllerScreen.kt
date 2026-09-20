package com.hoco.eq34.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoco.eq34.R
import com.hoco.eq34.data.ConnectionStatus
import com.hoco.eq34.data.HocoBleController
import com.hoco.eq34.data.model.BatteryInfoModel
import com.hoco.eq34.data.model.HocoDevice
import com.hoco.eq34.data.model.NoiseControlState
import com.hoco.eq34.data.model.NoiseMode
import com.hoco.eq34.ui.HocoViewModel
import com.hoco.eq34.ui.components.CompactConnectionStatusBar
import com.hoco.eq34.ui.components.ConnectionDialogOverlay
import com.hoco.eq34.ui.components.EarbudsHeroDisplay
import com.hoco.eq34.ui.components.HocoTopBar
import com.hoco.eq34.ui.components.ScreenshotAccurateNoiseCard
import com.hoco.eq34.ui.theme.AudioDarkBackground
import com.hoco.eq34.ui.theme.AudioDarkSurface
import com.hoco.eq34.ui.theme.AudioDarkSurfaceBorder
import com.hoco.eq34.ui.theme.AudioDarkSurfaceVariant
import com.hoco.eq34.ui.theme.CleanCardBorder
import com.hoco.eq34.ui.theme.CleanWhiteBackground
import com.hoco.eq34.ui.theme.CleanWhiteSurface
import com.hoco.eq34.ui.theme.CyanAccent
import com.hoco.eq34.ui.theme.GreenBattery
import com.hoco.eq34.ui.theme.OrangeBattery
import com.hoco.eq34.ui.theme.RedBattery
import com.hoco.eq34.ui.theme.TextInactive
import com.hoco.eq34.ui.theme.TextPrimary
import com.hoco.eq34.ui.theme.TextPrimaryDark
import com.hoco.eq34.ui.theme.TextSecondary
import com.hoco.eq34.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainControllerScreen(
    viewModel: HocoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val batteryState by viewModel.batteryState.collectAsState()
    val noiseState by viewModel.noiseState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val logMessages by viewModel.logMessages.collectAsState()
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    val deviceIdentification by viewModel.deviceIdentification.collectAsState()
    val lastCommandResult by viewModel.lastCommandResult.collectAsState()

    var showLogs by remember { mutableStateOf(false) }
    var showDeviceSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameName by remember { mutableStateOf("") }
    var showConnectDialog by remember { mutableStateOf(false) }
    var pendingConnectDevice by remember { mutableStateOf<android.bluetooth.BluetoothDevice?>(null) }
    var pendingDeviceName by remember { mutableStateOf("EQ34 Plus") }

    // Auto-show connect dialog when a HOCO device is discovered and we're not connected
    LaunchedEffect(discoveredDevices, connectionState) {
        if (connectionState == ConnectionStatus.DISCONNECTED) {
            val hocoDevice = discoveredDevices.firstOrNull {
                it.name.contains("HOCO", ignoreCase = true) || it.name.contains("EQ34", ignoreCase = true)
            }
            if (hocoDevice != null && !showConnectDialog) {
                pendingConnectDevice = hocoDevice.device
                pendingDeviceName = hocoDevice.name
                showConnectDialog = true
            }
        }
    }

    // Controls are only enabled when device is identified and ready
    val controlsEnabled = connectionState == ConnectionStatus.READY

    // Permissions launcher
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.updatePermissionsGranted(allGranted)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CleanWhiteBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top Bar: [< Back] "HOCO EQ34 Plus ANC" [Hexagon Nut Settings]
            HocoTopBar(
                title = connectedDevice?.name?.ifBlank { "EQ34 Plus ANC" } ?: "EQ34 Plus ANC",
                onBackClick = {
                    showDeviceSheet = !showDeviceSheet
                },
                onSettingsClick = {
                    showLogs = !showLogs
                },
                onTitleClick = {
                    if (controlsEnabled) {
                        renameName = connectedDevice?.name ?: ""
                        showRenameDialog = true
                    }
                }
            )

            if (!hasPermissions) {
                // Permission warning card
                PermissionRequestCard(
                    onRequestPermissions = { permissionLauncher.launch(permissionsToRequest) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Bluetooth Status & Connect bar (subtle)
                item {
                    CompactConnectionStatusBar(
                        connectionState = connectionState,
                        connectedDevice = connectedDevice,
                        isScanning = isScanning,
                        onScanClick = {
                            if (isScanning) viewModel.stopScan() else viewModel.startScan()
                        },
                        onDisconnectClick = { viewModel.disconnect() },
                        onManageClick = { showDeviceSheet = !showDeviceSheet }
                    )
                }

                // 2. Earbuds Hero Image + Left & Right Battery Pills (L 100% | R 100%)
                item {
                    EarbudsHeroDisplay(batteryState = batteryState)
                }

                // 3. White Card: "Noise Control" + 1..10 segmented bar + 3 rounded buttons
                item {
                    ScreenshotAccurateNoiseCard(
                        noiseState = noiseState,
                        isConnected = controlsEnabled,
                        onProgressSelected = { progress -> viewModel.setNoiseProgress(progress) },
                        onModeSelect = { mode ->
                            when (mode) {
                                NoiseMode.STANDARD -> viewModel.setStandardMode()
                                NoiseMode.ANC -> viewModel.setNoiseProgress(10)
                                NoiseMode.TRANSPARENCY -> viewModel.setNoiseProgress(0)
                            }
                        }
                    )
                }

                // Device verification status banner
                if (connectionState == ConnectionStatus.IDENTIFYING) {
                    item {
                        VerificationBanner(
                            message = "Identifying device...",
                            isWarning = false
                        )
                    }
                }

                if (connectionState == ConnectionStatus.CONNECTED && !controlsEnabled) {
                    item {
                        VerificationBanner(
                            message = "Device not verified. Controls disabled for safety.",
                            isWarning = true
                        )
                    }
                }

                // Command result feedback
                when (val result = lastCommandResult) {
                    is HocoBleController.CommandResult.Failed -> {
                        item {
                            VerificationBanner(
                                message = "Command failed: ${result.message}",
                                isWarning = true
                            )
                        }
                    }
                    is HocoBleController.CommandResult.DeviceNotVerified -> {
                        item {
                            VerificationBanner(
                                message = result.message,
                                isWarning = true
                            )
                        }
                    }
                    else -> {}
                }

                // Expandable device manager / scan list
                if (showDeviceSheet || connectionState != ConnectionStatus.READY) {
                    item {
                        DeviceListSection(
                            isScanning = isScanning,
                            pairedDevices = pairedDevices,
                            discoveredDevices = discoveredDevices,
                            connectedDevice = connectedDevice,
                            onConnectDevice = { device -> viewModel.connect(device) },
                            onScanClick = {
                                if (isScanning) viewModel.stopScan() else viewModel.startScan()
                            }
                        )
                    }
                }

                // Real-time protocol log viewer (triggered by top-right gear or toggle)
                if (showLogs) {
                    item {
                        LogsSectionCard(logs = logMessages)
                    }
                }
            }
        }

        // Rename dialog
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Device") },
                text = {
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        label = { Text("Device name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.renameDevice(renameName)
                            showRenameDialog = false
                        },
                        enabled = renameName.isNotBlank() && renameName.length <= 32
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Connection popup overlay
        ConnectionDialogOverlay(
            show = showConnectDialog,
            deviceName = pendingDeviceName,
            isConnected = connectionState == ConnectionStatus.READY,
            isConnecting = connectionState == ConnectionStatus.CONNECTING || connectionState == ConnectionStatus.IDENTIFYING,
            batteryState = batteryState,
            onConnect = {
                pendingConnectDevice?.let { device ->
                    viewModel.connect(device)
                }
            },
            onCancel = {
                showConnectDialog = false
                pendingConnectDevice = null
            },
            onDismiss = {
                showConnectDialog = false
                pendingConnectDevice = null
            }
        )
    }
}

@Composable
fun VerificationBanner(
    message: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) Color(0xFFFFF3CD) else Color(0xFFD1ECF1)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isWarning) Color(0xFFFFE69C) else Color(0xFFBEE5EB)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWarning) Icons.Default.Info else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isWarning) Color(0xFF856404) else Color(0xFF0C5460),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isWarning) Color(0xFF856404) else Color(0xFF0C5460),
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TopControllerBar(
    connectionState: ConnectionStatus,
    connectedDeviceName: String?,
    isScanning: Boolean,
    onRefreshClick: () -> Unit,
    onToggleLogs: () -> Unit,
    showLogs: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Status indicator pill
                StatusBadgePill(connectionState = connectionState)
            }
            Text(
                text = if (connectionState == ConnectionStatus.CONNECTED && connectedDeviceName != null) {
                    "Connected: $connectedDeviceName"
                } else {
                    stringResource(R.string.app_subtitle)
                },
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleLogs,
                modifier = Modifier.testTag("toggle_logs_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Diagnostics Logs",
                    tint = if (showLogs) CyanAccent else TextSecondaryDark
                )
            }
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier.testTag("refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = CyanAccent
                )
            }
        }
    }
}

@Composable
fun StatusBadgePill(connectionState: ConnectionStatus) {
    val (color, text) = when (connectionState) {
        ConnectionStatus.READY -> GreenBattery to "Ready"
        ConnectionStatus.CONNECTED -> OrangeBattery to "Identifying"
        ConnectionStatus.IDENTIFYING -> OrangeBattery to "Identifying"
        ConnectionStatus.CONNECTING -> OrangeBattery to "Connecting"
        ConnectionStatus.DISCONNECTED -> Color(0xFF6E7681) to "Disconnected"
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        )
    }
}

@Composable
fun PermissionRequestCard(onRequestPermissions: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = OrangeBattery,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.permission_required),
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2024), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("grant_permissions_button")
            ) {
                Text(stringResource(R.string.grant_permission), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: ConnectionStatus,
    connectedDevice: HocoDevice?,
    onDisconnect: () -> Unit,
    onScanClick: () -> Unit,
    isScanning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AudioDarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(CyanAccent.copy(alpha = 0.35f), AudioDarkSurfaceBorder)
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (connectionState == ConnectionStatus.CONNECTED) {
                                    CyanAccent.copy(alpha = 0.15f)
                                } else AudioDarkSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headset,
                            contentDescription = null,
                            tint = if (connectionState == ConnectionStatus.CONNECTED) CyanAccent else TextSecondaryDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = connectedDevice?.name ?: "EQ34 Plus",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        )
                        Text(
                            text = connectedDevice?.address ?: "Tap 'Scan' below to find and connect",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                        )
                    }
                }

                if (connectionState == ConnectionStatus.CONNECTED) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedBattery),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(RedBattery, RedBattery.copy(alpha = 0.5f)))
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("disconnect_button")
                    ) {
                        Text(stringResource(R.string.btn_disconnect), style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = onScanClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color(0xFF00363F)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("scan_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF00363F),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_stop_scan), style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_scan), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatterySection(batteryState: BatteryInfoModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AudioDarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(AudioDarkSurfaceBorder, Color(0xFF1E2638)))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.section_battery),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryDark,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Earbud
                BatteryGaugeItem(
                    label = stringResource(R.string.left_earbud),
                    percentage = batteryState.leftBattery,
                    isCharging = batteryState.isLeftCharging,
                    icon = Icons.Default.Headset,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Right Earbud
                BatteryGaugeItem(
                    label = stringResource(R.string.right_earbud),
                    percentage = batteryState.rightBattery,
                    isCharging = batteryState.isRightCharging,
                    icon = Icons.Default.Headset,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Charging Case
                BatteryGaugeItem(
                    label = stringResource(R.string.charging_case),
                    percentage = batteryState.caseBattery,
                    isCharging = batteryState.isCaseCharging,
                    icon = Icons.Default.BatteryChargingFull,
                    modifier = Modifier.weight(1f)
                )
            }

            // If only single general battery is available
            if (batteryState.singleBattery >= 0 && !batteryState.hasEarbudsData) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AudioDarkSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Earbuds Combined Battery",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                    )
                    Text(
                        text = "${batteryState.singleBattery}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GreenBattery
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryGaugeItem(
    label: String,
    percentage: Int,
    isCharging: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val displayPercent = if (percentage in 0..100) "$percentage%" else "--"
    val gaugeColor = when {
        percentage < 0 -> TextSecondaryDark
        percentage <= 20 -> RedBattery
        percentage <= 45 -> OrangeBattery
        else -> GreenBattery
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AudioDarkSurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (percentage >= 0) CyanAccent else TextSecondaryDark,
                    modifier = Modifier.size(26.dp)
                )
                if (isCharging) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(GreenBattery),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Charging",
                            tint = Color.Black,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = displayPercent,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor
                )
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark)
            )
        }
    }
}

@Composable
fun NoiseControlSection(
    noiseState: NoiseControlState,
    isConnected: Boolean,
    onModeSelect: (NoiseMode) -> Unit,
    onProgressSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AudioDarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(AudioDarkSurfaceBorder, Color(0xFF1E2638)))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.section_noise_control),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryDark,
                        letterSpacing = 1.sp
                    )
                )
                if (isConnected) {
                    Text(
                        text = "Active: ${noiseState.mode.displayName}",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyanAccent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-way Segmented Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AudioDarkSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NoiseModeTab(
                    title = "ANC",
                    subtitle = "Noise Cancel",
                    icon = Icons.Default.Security,
                    isSelected = noiseState.mode == NoiseMode.ANC,
                    enabled = isConnected,
                    onClick = { onModeSelect(NoiseMode.ANC) },
                    modifier = Modifier.weight(1f)
                )

                NoiseModeTab(
                    title = "Standard",
                    subtitle = "Normal",
                    icon = Icons.Default.VolumeUp,
                    isSelected = noiseState.mode == NoiseMode.STANDARD,
                    enabled = isConnected,
                    onClick = { onModeSelect(NoiseMode.STANDARD) },
                    modifier = Modifier.weight(1f)
                )

                NoiseModeTab(
                    title = "Transparency",
                    subtitle = "Ambient",
                    icon = Icons.Default.Hearing,
                    isSelected = noiseState.mode == NoiseMode.TRANSPARENCY,
                    enabled = isConnected,
                    onClick = { onModeSelect(NoiseMode.TRANSPARENCY) },
                    modifier = Modifier.weight(1f)
                )
            }

            // ANC Gain slider (visible when ANC is active)
            AnimatedVisibility(
                visible = noiseState.mode == NoiseMode.ANC,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = AudioDarkSurfaceBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.anc_gain_level),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                            )
                        }

                        // Gain Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanAccent.copy(alpha = 0.15f))
                                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Level ${noiseState.internalLevel + 1} / 5",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = noiseState.uiProgress.toFloat(),
                        onValueChange = { newValue ->
                            val progress = newValue.toInt().coerceIn(6, 10)
                            onProgressSelected(progress)
                        },
                        valueRange = 6f..10f,
                        steps = 3,
                        enabled = isConnected,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAccent,
                            activeTrackColor = CyanAccent,
                            inactiveTrackColor = Color(0xFF21262D)
                        ),
                        modifier = Modifier.testTag("anc_level_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Low (1)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark))
                        Text("Moderate (3)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark))
                        Text("Max (5)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark))
                    }
                }
            }
        }
    }
}

@Composable
fun NoiseModeTab(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) CyanAccent else Color.Transparent,
        animationSpec = tween(200),
        label = "tab_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF00363F) else TextSecondaryDark,
        animationSpec = tween(200),
        label = "tab_fg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = if (isSelected) Color(0xFF004D59) else TextSecondaryDark.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
fun DeviceListSection(
    isScanning: Boolean,
    pairedDevices: List<HocoDevice>,
    discoveredDevices: List<HocoDevice>,
    connectedDevice: HocoDevice?,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CleanCardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby & Paired Devices",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                TextButton(onClick = onScanClick) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color(0xFF1E2024),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", color = Color(0xFF1E2024), style = MaterialTheme.typography.labelMedium)
                    } else {
                        Text("Scan BLE", color = Color(0xFF1E2024), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Paired devices
            if (pairedDevices.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.paired_devices),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                pairedDevices.forEach { dev ->
                    DeviceRowItem(
                        device = dev,
                        isCurrentConnected = connectedDevice?.address == dev.address,
                        onConnect = { onConnectDevice(dev.device) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Scanned nearby devices
            if (discoveredDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.nearby_devices),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                discoveredDevices.filter { scanned ->
                    pairedDevices.none { it.address == scanned.address }
                }.forEach { dev ->
                    DeviceRowItem(
                        device = dev,
                        isCurrentConnected = connectedDevice?.address == dev.address,
                        onConnect = { onConnectDevice(dev.device) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            if (pairedDevices.isEmpty() && discoveredDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_devices_found),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceRowItem(
    device: HocoDevice,
    isCurrentConnected: Boolean,
    onConnect: () -> Unit
) {
    val isHoco = device.name.contains("HOCO", ignoreCase = true) || device.name.contains("EQ34", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrentConnected) Color(0xFFF0FDF4) else Color(0xFFF9FAFB))
            .border(
                width = 1.dp,
                color = if (isCurrentConnected) GreenBattery.copy(alpha = 0.5f) else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onConnect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Headset,
                contentDescription = null,
                tint = if (isCurrentConnected) GreenBattery else if (isHoco) Color(0xFF1E2024) else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isHoco || isCurrentConnected) FontWeight.Bold else FontWeight.Normal,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isHoco) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E2024))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "HOCO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
                Text(
                    text = "${device.address} ${if (device.rssi != 0) "â€¢ ${device.rssi} dBm" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
        }

        if (isCurrentConnected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Connected",
                    tint = GreenBattery,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GreenBattery,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        } else {
            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHoco) Color(0xFF1E2024) else Color(0xFFE5E7EB),
                    contentColor = if (isHoco) Color.White else TextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_connect),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
fun LogsSectionCard(logs: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2024)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF374151))
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF67E8F9),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Real-time JieLi RCSP Protocol Logs",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF67E8F9),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                Text(
                    text = "${logs.size} entries",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9CA3AF))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Text(
                    text = "Awaiting Bluetooth events...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9CA3AF),
                        fontFamily = FontFamily.Monospace
                    )
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    logs.take(8).forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (line.contains("error", ignoreCase = true)) RedBattery
                                else if (line.contains("success", ignoreCase = true) || line.contains("connected", ignoreCase = true)) GreenBattery
                                else if (line.contains("VoiceMode", ignoreCase = true) || line.contains("ANC", ignoreCase = true)) Color(0xFF67E8F9)
                                else Color(0xFFF3F4F6),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

