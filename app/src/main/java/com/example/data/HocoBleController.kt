package com.example.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.example.data.model.BatteryInfoModel
import com.example.data.model.HocoDevice
import com.example.data.model.NoiseControlState
import com.example.data.model.NoiseControlPositions
import com.example.data.model.NoiseMode
import com.example.data.safety.CommandWhitelist
import com.example.data.safety.DeviceIdentifier
import com.jieli.bluetooth.bean.BleScanMessage
import com.jieli.bluetooth.bean.base.BaseError
import com.jieli.bluetooth.bean.base.VoiceMode
import com.jieli.bluetooth.bean.device.DevBroadcastMsg
import com.jieli.bluetooth.bean.device.status.BatteryInfo
import com.jieli.bluetooth.bean.response.ADVInfoResponse
import com.jieli.bluetooth.constant.StateCode
import com.jieli.bluetooth.impl.rcsp.RCSPController
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    IDENTIFYING,
    READY
}

class HocoBleController private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val rcspController: RCSPController? = try {
        if (RCSPController.isInit()) RCSPController.getInstance() else null
    } catch (e: Throwable) {
        null
    }

    private val _connectionState = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionState: StateFlow<ConnectionStatus> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<HocoDevice?>(null)
    val connectedDevice: StateFlow<HocoDevice?> = _connectedDevice.asStateFlow()

    private val _batteryState = MutableStateFlow(BatteryInfoModel())
    val batteryState: StateFlow<BatteryInfoModel> = _batteryState.asStateFlow()

    private val _noiseState = MutableStateFlow(NoiseControlState())
    val noiseState: StateFlow<NoiseControlState> = _noiseState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<HocoDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<HocoDevice>> = _discoveredDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<HocoDevice>>(emptyList())
    val pairedDevices: StateFlow<List<HocoDevice>> = _pairedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _logMessages = MutableStateFlow<List<String>>(emptyList())
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    private val _deviceIdentification = MutableStateFlow<DeviceIdentifier.IdentificationResult>(
        DeviceIdentifier.IdentificationResult.NotAttempted
    )
    val deviceIdentification: StateFlow<DeviceIdentifier.IdentificationResult> =
        _deviceIdentification.asStateFlow()

    private val _lastCommandResult = MutableStateFlow<CommandResult>(CommandResult.Idle)
    val lastCommandResult: StateFlow<CommandResult> = _lastCommandResult.asStateFlow()

    sealed class CommandResult {
        data object Idle : CommandResult()
        data class Success(val message: String) : CommandResult()
        data class Failed(val message: String, val error: String? = null) : CommandResult()
        data class DeviceNotVerified(val message: String) : CommandResult()
    }

    private var rawLeftMax: Int = 10
    private var commandDebounceJob: Job? = null

    private fun isSafeToSendCommands(): Boolean {
        val result = _deviceIdentification.value
        if (!DeviceIdentifier.isVerified(result)) {
            addLog("SAFETY: Write command blocked. Device not identified as EQ34 Plus.")
            return false
        }
        return true
    }

    private val rcspEventCallback = object : BTRcspEventCallback() {
        override fun onConnection(device: BluetoothDevice?, status: Int) {
            val dev = device ?: run {
                addLog("Connection event with null device, status=$status")
                return
            }
            val devName = getDeviceName(dev)
            val devAddr = dev.address ?: "Unknown"
            addLog("Connection event: dev=$devName ($devAddr), status=$status")

            when (status) {
                StateCode.CONNECTION_OK, StateCode.CONNECTION_CONNECTED -> {
                    _connectionState.value = ConnectionStatus.CONNECTED
                    _connectedDevice.value = HocoDevice(
                        device = dev,
                        name = devName,
                        address = devAddr,
                        isConnected = true,
                        isBonded = isDeviceBonded(dev)
                    )
                    addLog("Connected to $devName. Starting identification...")
                    scope.launch {
                        performSafeIdentificationSequence(dev)
                    }
                }
                StateCode.CONNECTION_CONNECTING -> {
                    _connectionState.value = ConnectionStatus.CONNECTING
                }
                StateCode.CONNECTION_DISCONNECT, StateCode.CONNECTION_FAILED -> {
                    handleDisconnection()
                }
            }
        }

        override fun onBatteryChange(device: BluetoothDevice?, batteryInfo: BatteryInfo?) {
            try {
                batteryInfo?.let {
                    addLog("Battery update: ${it.battery}%")
                    _batteryState.value = _batteryState.value.copy(
                        singleBattery = it.battery
                    )
                }
            } catch (e: Exception) {
                addLog("Battery callback error: ${e.message}")
            }
        }

        override fun onDeviceBroadcast(device: BluetoothDevice?, msg: DevBroadcastMsg?) {
            try {
                msg?.let {
                    val left = if (it.leftDeviceQuantity in 0..100) it.leftDeviceQuantity else _batteryState.value.leftBattery
                    val right = if (it.rightDeviceQuantity in 0..100) it.rightDeviceQuantity else _batteryState.value.rightBattery
                    val case = if (it.chargingBinQuantity in 0..100) it.chargingBinQuantity else _batteryState.value.caseBattery
                    addLog("Broadcast: L=${left}% R=${right}% Case=${case}%")
                    _batteryState.value = _batteryState.value.copy(
                        leftBattery = left,
                        isLeftCharging = it.isLeftCharging,
                        rightBattery = right,
                        isRightCharging = it.isRightCharging,
                        caseBattery = case,
                        isCaseCharging = it.isDeviceCharging
                    )
                }
            } catch (e: Exception) {
                addLog("Broadcast callback error: ${e.message}")
            }
        }

        override fun onDeviceSettingsInfo(device: BluetoothDevice?, type: Int, advInfo: ADVInfoResponse?) {
            try {
                advInfo?.let {
                    val left = if (it.leftDeviceQuantity in 0..100) it.leftDeviceQuantity else _batteryState.value.leftBattery
                    val right = if (it.rightDeviceQuantity in 0..100) it.rightDeviceQuantity else _batteryState.value.rightBattery
                    val case = if (it.chargingBinQuantity in 0..100) it.chargingBinQuantity else _batteryState.value.caseBattery
                    addLog("Settings info: L=${left}% R=${right}% Case=${case}%")
                    _batteryState.value = _batteryState.value.copy(
                        leftBattery = left,
                        isLeftCharging = it.isLeftCharging,
                        rightBattery = right,
                        isRightCharging = it.isRightCharging,
                        caseBattery = case
                    )
                }
            } catch (e: Exception) {
                addLog("Settings info callback error: ${e.message}")
            }
        }

        override fun onCurrentVoiceMode(device: BluetoothDevice?, voiceMode: VoiceMode?) {
            try {
                voiceMode?.let {
                val noiseMode = NoiseMode.fromModeId(it.mode)
                val maxLevel = if (it.leftMax > 0) it.leftMax else rawLeftMax
                rawLeftMax = maxLevel
                val internalLevel = it.leftCurVal

                addLog("VoiceMode read-back: ${noiseMode.displayName} (mode=${it.mode}, " +
                    "internalLevel=$internalLevel, maxLevel=$maxLevel)")

                val confirmedState = _noiseState.value.confirmFromDevice(
                    mode = noiseMode,
                    leftCurVal = internalLevel,
                    maxLevel = maxLevel
                )

                val pendingProg = _noiseState.value.pendingProgress
                if (pendingProg != null && confirmedState.uiProgress == pendingProg) {
                    addLog("CONFIRMED: progress $pendingProg matches device read-back")
                    _lastCommandResult.value = CommandResult.Success(
                        "${noiseMode.displayName} confirmed at progress ${confirmedState.uiProgress}"
                    )
                } else if (pendingProg != null && confirmedState.uiProgress != pendingProg) {
                    addLog("MISMATCH: requested progress $pendingProg but device reports ${confirmedState.uiProgress}")
                    _lastCommandResult.value = CommandResult.Failed(
                        "Mismatch: requested $pendingProg, device reports ${confirmedState.uiProgress}"
                    )
                } else {
                    _lastCommandResult.value = CommandResult.Success(
                        "State read: ${noiseMode.displayName}, level $internalLevel/$maxLevel"
                    )
                }

                _noiseState.value = confirmedState
            }
            } catch (e: Exception) {
                addLog("VoiceMode callback error: ${e.message}")
            }
        }

        override fun onVoiceModeList(device: BluetoothDevice?, voiceModes: MutableList<VoiceMode>?) {
            try {
                voiceModes?.let { list ->
                    addLog("Supported VoiceModes: size=${list.size}")
                    val ancMode = list.firstOrNull { it.mode == VoiceMode.VOICE_MODE_DENOISE }
                    if (ancMode != null && ancMode.leftMax > 0) {
                        rawLeftMax = ancMode.leftMax
                        _noiseState.value = _noiseState.value.copy(maxInternalLevel = ancMode.leftMax)
                    }
                }
            } catch (e: Exception) {
                addLog("VoiceModeList callback error: ${e.message}")
            }
        }

        override fun onDiscovery(device: BluetoothDevice?, bleScanMessage: BleScanMessage?) {
            try {
                device ?: return
                val name = getDeviceName(device)
                val address = device.address ?: return
                val rssi = bleScanMessage?.rssi ?: 0

                val item = HocoDevice(
                    device = device,
                    name = if (name.isNotEmpty()) name else "JieLi Audio Device",
                    address = address,
                    isBonded = isDeviceBonded(device),
                    rssi = rssi
                )

                bleScanMessage?.let {
                    if (it.leftDeviceQuantity in 0..100 || it.rightDeviceQuantity in 0..100 || it.chargingBinQuantity in 0..100) {
                        if (_connectedDevice.value?.address == address) {
                            _batteryState.value = _batteryState.value.copy(
                                leftBattery = if (it.leftDeviceQuantity in 0..100) it.leftDeviceQuantity else _batteryState.value.leftBattery,
                                isLeftCharging = it.isLeftCharging,
                                rightBattery = if (it.rightDeviceQuantity in 0..100) it.rightDeviceQuantity else _batteryState.value.rightBattery,
                                isRightCharging = it.isRightCharging,
                                caseBattery = if (it.chargingBinQuantity in 0..100) it.chargingBinQuantity else _batteryState.value.caseBattery,
                                isCaseCharging = it.chargingBinStatus > 0
                            )
                        }
                    }
                }

                val currentList = _discoveredDevices.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.address == address }
                if (existingIndex >= 0) {
                    currentList[existingIndex] = item
                } else {
                    currentList.add(item)
                }
                currentList.sortByDescending { dev ->
                    var priority = 0
                    if (dev.name.contains("HOCO", ignoreCase = true)) priority += 100
                    if (dev.name.contains("EQ34", ignoreCase = true)) priority += 200
                    if (dev.isBonded) priority += 50
                    priority
                }
                _discoveredDevices.value = currentList
            } catch (e: SecurityException) {
                addLog("Discovery permission error: ${e.message}")
            } catch (e: Exception) {
                addLog("Discovery error: ${e.message}")
            }
        }

        override fun onDiscoveryStatus(bBle: Boolean, bStart: Boolean) {
            addLog("Scan status: isScanning=$bStart")
            _isScanning.value = bStart
        }
    }

    private suspend fun performSafeIdentificationSequence(device: BluetoothDevice) {
        try {
            _connectionState.value = ConnectionStatus.IDENTIFYING
            addLog("=== SAFE IDENTIFICATION SEQUENCE ===")

        addLog("Step 1: Waiting for RCSP initialization...")
        var retries = 0
        while (retries < 20) {
            if (rcspController?.isDeviceConnected(device) == true) {
                break
            }
            delay(250)
            retries++
        }

        if (retries >= 20) {
            addLog("RCSP initialization timeout. Disconnecting for safety.")
            handleDisconnection()
            return
        }

        addLog("Step 2: Querying device info for identification...")
        rcspController?.getAllDeviceSettingsInfo(device, object : OnRcspActionCallback<ADVInfoResponse> {
            override fun onSuccess(dev: BluetoothDevice?, message: ADVInfoResponse?) {
                addLog("Device settings received. Performing identification...")
                val name = getDeviceName(dev)
                val hasAnc = true

                val identification = DeviceIdentifier.identifyFromDeviceInfo(
                    device = dev ?: device,
                    deviceName = name,
                    protocolVersion = null,
                    productName = null,
                    hasAncFeature = hasAnc,
                )

                _deviceIdentification.value = identification

                if (DeviceIdentifier.isVerified(identification)) {
                    addLog("IDENTIFIED: $name is a verified EQ34 Plus")
                    scope.launch { performSafeReads(device) }
                } else {
                    val reason = (identification as? DeviceIdentifier.IdentificationResult.Unverified)?.reason
                        ?: "Unknown reason"
                    addLog("IDENTIFICATION FAILED: $reason")
                    addLog("ALL WRITE CONTROLS DISABLED for safety.")
                    _connectionState.value = ConnectionStatus.CONNECTED
                    _lastCommandResult.value = CommandResult.DeviceNotVerified(
                        "Device not verified as EQ34 Plus. Controls disabled. Reason: $reason"
                    )
                }
            }

            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Device info query error: ${error?.message}")
                val name = getDeviceName(dev ?: device)
                val identification = DeviceIdentifier.identifyFromScan(dev ?: device, name)
                _deviceIdentification.value = identification

                if (DeviceIdentifier.isVerified(identification)) {
                    addLog("Name-only identification passed: $name")
                    scope.launch { performSafeReads(device) }
                } else {
                    addLog("Identification failed. Controls disabled.")
                    _connectionState.value = ConnectionStatus.CONNECTED
                    _lastCommandResult.value = CommandResult.DeviceNotVerified(
                        "Could not verify device identity. Controls disabled."
                    )
                }
            }
        })
        } catch (e: Exception) {
            addLog("Identification sequence error: ${e.message}")
            _connectionState.value = ConnectionStatus.CONNECTED
        }
    }

    private suspend fun performSafeReads(device: BluetoothDevice) {
        addLog("Step 3: Reading current state (read-only)...")

        rcspController?.getAllDeviceSettingsInfo(device, object : OnRcspActionCallback<ADVInfoResponse> {
            override fun onSuccess(dev: BluetoothDevice?, message: ADVInfoResponse?) {
                message?.let { adv ->
                    _batteryState.value = _batteryState.value.copy(
                        leftBattery = if (adv.leftDeviceQuantity in 0..100) adv.leftDeviceQuantity else _batteryState.value.leftBattery,
                        isLeftCharging = adv.isLeftCharging,
                        rightBattery = if (adv.rightDeviceQuantity in 0..100) adv.rightDeviceQuantity else _batteryState.value.rightBattery,
                        isRightCharging = adv.isRightCharging,
                        caseBattery = if (adv.chargingBinQuantity in 0..100) adv.chargingBinQuantity else _batteryState.value.caseBattery
                    )
                }
                addLog("Battery read complete")
            }
            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Battery read error: ${error?.message}")
            }
        })

        rcspController?.getCurrentVoiceMode(device, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("ANC state read complete")
            }
            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("ANC state read error: ${error?.message}")
            }
        })

        rcspController?.getAllVoiceModes(device, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("Voice modes read complete")
            }
            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Voice modes read error: ${error?.message}")
            }
        })

        delay(1000)

        _connectionState.value = ConnectionStatus.READY
        addLog("=== IDENTIFICATION AND STATE READ COMPLETE ===")
        addLog("Controls are now enabled.")
    }

    fun setStandardMode() {
        if (!isSafeToSendCommands()) {
            _lastCommandResult.value = CommandResult.DeviceNotVerified(
                "Cannot change noise control: Device not verified as EQ34 Plus."
            )
            return
        }
        setNoiseProgress(NoiseControlPositions.STANDARD_PROGRESS)
    }

    fun renameDevice(newName: String) {
        if (!isSafeToSendCommands()) {
            _lastCommandResult.value = CommandResult.DeviceNotVerified(
                "Cannot rename: Device not verified as EQ34 Plus."
            )
            return
        }

        if (!CommandWhitelist.isPermitted(CommandWhitelist.SafeCommand.RENAME_DEVICE)) {
            addLog("SAFETY: RENAME_DEVICE not in whitelist")
            _lastCommandResult.value = CommandResult.Failed("Rename not permitted")
            return
        }

        if (newName.isBlank() || newName.length > 32) {
            _lastCommandResult.value = CommandResult.Failed("Name must be 1-32 characters")
            return
        }

        val ctrl = rcspController
        val device = _connectedDevice.value?.device ?: ctrl?.usingDevice
        if (device == null || ctrl == null) {
            addLog("Cannot rename: No device connected")
            _lastCommandResult.value = CommandResult.Failed("No device connected")
            return
        }

        addLog("Renaming device to: $newName")
        _lastCommandResult.value = CommandResult.Idle

        ctrl.configDeviceName(device, newName, object : OnRcspActionCallback<Int> {
            override fun onSuccess(dev: BluetoothDevice?, message: Int?) {
                addLog("Rename successful: $newName")
                _connectedDevice.value = _connectedDevice.value?.copy(name = newName)
                _lastCommandResult.value = CommandResult.Success("Renamed to $newName")
            }

            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Rename failed: ${error?.message}")
                _lastCommandResult.value = CommandResult.Failed(
                    "Failed to rename to $newName",
                    error?.message
                )
            }
        })
    }

    /**
     * Set noise control by official progress position (0-10).
     *
     * Official mapping from hoco.music_1.3.5-gp.apks (ii8.java):
     * - Progress 0-4: Transparency, level = 4 - progress
     * - Progress 5:   Standard (off)
     * - Progress 6-10: ANC, level = progress - 6
     */
    fun setNoiseProgress(progress: Int) {
        if (!isSafeToSendCommands()) {
            _lastCommandResult.value = CommandResult.DeviceNotVerified(
                "Cannot change noise control: Device not verified as EQ34 Plus."
            )
            return
        }

        if (!CommandWhitelist.isPermitted(CommandWhitelist.SafeCommand.SET_ANC_MODE) &&
            !CommandWhitelist.isPermitted(CommandWhitelist.SafeCommand.SET_ANC_LEVEL)) {
            addLog("SAFETY: Neither SET_ANC_MODE nor SET_ANC_LEVEL in whitelist")
            _lastCommandResult.value = CommandResult.Failed("Command not permitted")
            return
        }

        if (!NoiseControlPositions.isValidProgress(progress)) {
            addLog("Invalid progress: $progress (must be 0-10)")
            _lastCommandResult.value = CommandResult.Failed("Invalid progress: $progress")
            return
        }

        commandDebounceJob?.cancel()

        commandDebounceJob = scope.launch {
            delay(150)

            val ctrl = rcspController
            val device = _connectedDevice.value?.device ?: ctrl?.usingDevice
            if (device == null || ctrl == null) {
                addLog("Cannot set noise progress: No device connected")
                _lastCommandResult.value = CommandResult.Failed("No device connected")
                return@launch
            }

            _noiseState.value = _noiseState.value.requestProgress(progress)

            val mode = NoiseControlPositions.modeForProgress(progress)
            val maxLevel = if (rawLeftMax > 0) rawLeftMax else 10
            val internalLevel = NoiseControlPositions.deviceLevelForProgress(progress, maxLevel)

            val voiceMode = VoiceMode()
                .setMode(mode.modeId)
                .setLeftMax(maxLevel)
                .setRightMax(maxLevel)
                .setLeftCurVal(internalLevel)
                .setRightCurVal(internalLevel)

            addLog("Sending progress=$progress: ${mode.displayName} level=$internalLevel (max=$maxLevel)")
            _lastCommandResult.value = CommandResult.Idle

            ctrl.setCurrentVoiceMode(device, voiceMode, object : OnRcspActionCallback<Boolean> {
                override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                    addLog("Progress $progress ACK received for ${mode.displayName}")
                }

                override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                    addLog("Progress $progress error: ${error?.message}")
                    _noiseState.value = _noiseState.value.copy(
                        isPendingVerification = false,
                        pendingProgress = null
                    )
                    _lastCommandResult.value = CommandResult.Failed(
                        "Failed to set progress $progress: ${mode.displayName}",
                        error?.message
                    )
                    scope.launch { readCurrentState(device) }
                }
            })
        }
    }

    private suspend fun readCurrentState(device: BluetoothDevice) {
        rcspController?.getCurrentVoiceMode(device, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("State re-read after error")
            }
            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("State re-read error: ${error?.message}")
            }
        })
    }

    private fun handleDisconnection() {
        _connectionState.value = ConnectionStatus.DISCONNECTED
        _connectedDevice.value = null
        _batteryState.value = BatteryInfoModel()
        _noiseState.value = NoiseControlState()
        _deviceIdentification.value = DeviceIdentifier.IdentificationResult.NotAttempted
        _lastCommandResult.value = CommandResult.Idle
        commandDebounceJob?.cancel()
        commandDebounceJob = null
        addLog("Device disconnected. State reset.")
    }

    fun addLog(msg: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formatted = "[$timestamp] $msg"
        Log.d(TAG, formatted)
        val list = _logMessages.value.toMutableList()
        list.add(0, formatted)
        if (list.size > 50) {
            list.removeAt(list.size - 1)
        }
        _logMessages.value = list
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter != null && adapter.isEnabled) {
            val bonded = adapter.bondedDevices ?: emptySet()
            val list = bonded.map { dev ->
                HocoDevice(
                    device = dev,
                    name = dev.name ?: "Unknown Bluetooth Device",
                    address = dev.address,
                    isBonded = true
                )
            }.sortedByDescending { dev ->
                var priority = 0
                if (dev.name.contains("HOCO", ignoreCase = true)) priority += 100
                if (dev.name.contains("EQ34", ignoreCase = true)) priority += 200
                priority
            }
            _pairedDevices.value = list
            addLog("Found ${list.size} paired devices")
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkInitialConnectedDevice() {
        val ctrl = rcspController ?: return
        val using = ctrl.usingDevice
        if (using != null && ctrl.isDeviceConnected(using)) {
            val name = getDeviceName(using)
            _connectionState.value = ConnectionStatus.CONNECTED
            _connectedDevice.value = HocoDevice(
                device = using,
                name = name,
                address = using.address,
                isConnected = true,
                isBonded = isDeviceBonded(using)
            )
            scope.launch {
                performSafeIdentificationSequence(using)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val ctrl = rcspController
        if (ctrl == null) {
            addLog("BLE Controller not ready — RCSP not initialized")
            return
        }
        _discoveredDevices.value = emptyList()
        addLog("Starting BLE Scan...")
        try {
            val success = ctrl.startBleScan(15000)
            _isScanning.value = success
            if (!success) {
                addLog("Failed to start BLE Scan (Check Bluetooth/Location)")
            }
        } catch (e: SecurityException) {
            addLog("Bluetooth permission denied: ${e.message}")
            _isScanning.value = false
        } catch (e: Exception) {
            addLog("Scan error: ${e.message}")
            _isScanning.value = false
        }
    }

    fun stopScan() {
        addLog("Stopping scan")
        rcspController?.stopScan()
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stopScan()
        val ctrl = rcspController
        if (ctrl == null) {
            addLog("BLE Controller not ready — RCSP not initialized")
            return
        }
        _connectionState.value = ConnectionStatus.CONNECTING
        _deviceIdentification.value = DeviceIdentifier.IdentificationResult.NotAttempted
        val name = getDeviceName(device)
        addLog("Connecting to $name (${device.address})...")
        try {
            ctrl.connectDevice(device)
        } catch (e: SecurityException) {
            addLog("Bluetooth permission denied: ${e.message}")
            _connectionState.value = ConnectionStatus.DISCONNECTED
        } catch (e: Exception) {
            addLog("Connect error: ${e.message}")
            _connectionState.value = ConnectionStatus.DISCONNECTED
        }
    }

    fun disconnect() {
        addLog("Disconnecting...")
        val ctrl = rcspController
        val dev = _connectedDevice.value?.device ?: ctrl?.usingDevice
        if (dev != null && ctrl != null) {
            ctrl.disconnectDevice(dev)
        }
        handleDisconnection()
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceName(device: BluetoothDevice?): String {
        if (device == null) return "Unknown"
        return try {
            device.name ?: device.address ?: "Device"
        } catch (e: Exception) {
            device.address ?: "Device"
        }
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceBonded(device: BluetoothDevice?): Boolean {
        if (device == null) return false
        return try {
            device.bondState == BluetoothDevice.BOND_BONDED
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "HocoBleController"

        @Volatile
        private var instance: HocoBleController? = null

        fun getInstance(context: Context): HocoBleController {
            return instance ?: synchronized(this) {
                instance ?: HocoBleController(context.applicationContext).also { instance = it }
            }
        }
    }
}
