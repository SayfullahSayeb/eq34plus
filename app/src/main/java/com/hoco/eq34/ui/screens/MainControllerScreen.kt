package com.hoco.eq34.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoco.eq34.R
import com.hoco.eq34.data.ConnectionStatus
import com.hoco.eq34.ui.HocoViewModel
import com.hoco.eq34.data.model.BatteryInfoModel
import com.hoco.eq34.data.model.HocoDevice
import com.hoco.eq34.data.model.NoiseControlState
import com.hoco.eq34.data.model.NoiseMode
import com.hoco.eq34.ui.theme.CleanWhiteBackground
import com.hoco.eq34.ui.theme.GreenBattery
import com.hoco.eq34.ui.theme.TextPrimary
import com.hoco.eq34.ui.theme.TextSecondary

@Composable
fun MainControllerScreen(
    viewModel: HocoViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val batteryState by viewModel.batteryState.collectAsState()
    val noiseState by viewModel.noiseState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameName by remember { mutableStateOf("") }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            viewModel.refreshBondedDevices()
            viewModel.startScan()
        }
    }

    val controlsEnabled = connectionState == ConnectionStatus.READY
    val isConnected = connectionState != ConnectionStatus.DISCONNECTED

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
            if (!isConnected) {
                NotConnectedScreen(
                    isScanning = isScanning,
                    pairedDevices = pairedDevices,
                    discoveredDevices = discoveredDevices,
                    onScanClick = {
                        if (isScanning) {
                            viewModel.stopScan()
                        } else if (hasPermissions) {
                            viewModel.startScan()
                        } else {
                            permissionLauncher.launch(permissionsToRequest)
                        }
                    },
                    onConnectDevice = { device ->
                        viewModel.connect(device)
                    }
                )
            } else {
                ConnectedScreen(
                    connectionState = connectionState,
                    connectedDeviceName = connectedDevice?.name ?: "HOCO EQ34 Plus",
                    batteryState = batteryState,
                    noiseState = noiseState,
                    controlsEnabled = controlsEnabled,
                    onProgressSelected = { progress -> viewModel.setNoiseProgress(progress) },
                    onModeSelect = { mode ->
                        when (mode) {
                            NoiseMode.STANDARD -> viewModel.setStandardMode()
                            NoiseMode.ANC -> viewModel.setNoiseProgress(10)
                            NoiseMode.TRANSPARENCY -> viewModel.setNoiseProgress(0)
                        }
                    },
                    onDisconnect = { viewModel.disconnect() },
                    onRename = { newName -> viewModel.renameDevice(newName) }
                )
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Device") },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    label = { Text("Device Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDevice(renameName)
                        showRenameDialog = false
                    },
                    enabled = renameName.isNotBlank() && renameName.length <= 32
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun NotConnectedScreen(
    isScanning: Boolean,
    pairedDevices: List<HocoDevice>,
    discoveredDevices: List<HocoDevice>,
    onScanClick: () -> Unit,
    onConnectDevice: (android.bluetooth.BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "HOCO EQ34 Plus",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Connect your earbuds",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Case image before scan button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.eq34_case),
                contentDescription = "EQ34 Plus Case",
                tint = Color.Unspecified,
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Scanning", color = Color.White)
            } else {
                Icon(
                    Icons.Default.Headset,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan for Devices", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val allDevices = pairedDevices + discoveredDevices.filter { scanned ->
            pairedDevices.none { it.address == scanned.address }
        }

        if (allDevices.isEmpty() && !isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No devices found",
                        textAlign = TextAlign.Center,
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pairedDevices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Paired Devices",
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(pairedDevices) { dev ->
                        DeviceItem(
                            name = dev.name,
                            isBonded = dev.isBonded,
                            onClick = { onConnectDevice(dev.device) }
                        )
                    }
                }

                if (discoveredDevices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Nearby Devices",
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(discoveredDevices.filter { scanned ->
                        pairedDevices.none { it.address == scanned.address }
                    }) { dev ->
                        DeviceItem(
                            name = dev.name,
                            isBonded = dev.isBonded,
                            onClick = { onConnectDevice(dev.device) }
                        )
                    }
                }

                if (isScanning) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = TextSecondary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning...", color = TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    name: String,
    isBonded: Boolean,
    onClick: () -> Unit
) {
    val isHoco = name.contains("HOCO", ignoreCase = true) || name.contains("EQ34", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHoco) Color(0xFFF0FDF4) else Color(0xFFF9FAFB))
            .border(
                width = 1.dp,
                color = if (isHoco) GreenBattery.copy(alpha = 0.5f) else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Headset,
            contentDescription = null,
            tint = if (isHoco) TextPrimary else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isHoco) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isBonded) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Paired",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                        color = GreenBattery,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun ConnectedScreen(
    connectionState: ConnectionStatus,
    connectedDeviceName: String,
    batteryState: BatteryInfoModel,
    noiseState: NoiseControlState,
    controlsEnabled: Boolean,
    onProgressSelected: (Int) -> Unit,
    onModeSelect: (NoiseMode) -> Unit,
    onDisconnect: () -> Unit,
    onRename: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Device name with edit icon on top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = connectedDeviceName,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    renameName = connectedDeviceName
                    showRenameDialog = true
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Name",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        val statusText = when (connectionState) {
            ConnectionStatus.CONNECTING -> "Connecting..."
            ConnectionStatus.IDENTIFYING -> "Identifying..."
            ConnectionStatus.READY -> "Connected"
            else -> "Connected"
        }
        Text(
            text = statusText,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = if (connectionState == ConnectionStatus.READY) GreenBattery else TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Earbuds images: Left bud only | Right bud only (no case)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.eq34_left),
                    contentDescription = "Left Earbud",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "L ${if (batteryState.leftBattery > 0) "${batteryState.leftBattery}%" else ""}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.eq34_right),
                    contentDescription = "Right Earbud",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "R ${if (batteryState.rightBattery > 0) "${batteryState.rightBattery}%" else ""}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Case battery row
        if (batteryState.caseBattery > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.eq34_case),
                    contentDescription = "Case",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Case ${batteryState.caseBattery}%",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ANC Control Card
        com.hoco.eq34.ui.components.ScreenshotAccurateNoiseCard(
            noiseState = noiseState,
            isConnected = controlsEnabled,
            onProgressSelected = onProgressSelected,
            onModeSelect = onModeSelect
        )

        if (connectionState == ConnectionStatus.CONNECTING || connectionState == ConnectionStatus.IDENTIFYING) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1ECF1))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF0C5460))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (connectionState == ConnectionStatus.CONNECTING) "Connecting..." else "Identifying device...",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0C5460)
                    )
                }
            }
        }

        if (connectionState == ConnectionStatus.CONNECTED && !controlsEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF856404), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device not verified. Controls disabled.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = Color(0xFF856404)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom: Disconnect
        TextButton(onClick = onDisconnect) {
            Text("Disconnect", color = TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Device") },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    label = { Text("Device Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRename(renameName)
                        showRenameDialog = false
                    },
                    enabled = renameName.isNotBlank() && renameName.length <= 32
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}
