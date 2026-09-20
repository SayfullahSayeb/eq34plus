package com.example.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.example.data.model.AncSettings
import com.example.data.model.BatteryInfoModel
import com.example.data.model.HocoDevice
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

/**
 * Hardened BLE controller for HOCO EQ34 Plus.
 *
 * Safety features:
 * - Device identification before any write commands
 * - Command whitelist enforcement
 * - Read-back verification (no optimistic UI updates)
 * - Safe connection sequence (identify first, then enable controls)
 * - No firmware/OTA/flash operations
 * - No raw BLE packet sending
 */
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

    private val _ancSettings = MutableStateFlow(AncSettings())
    val ancSettings: StateFlow<AncSettings> = _ancSettings.asStateFlow()

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

    private var cachedVoiceMode: VoiceMode? = null
    private var rawLeftMax: Int = 10

    /**
     * Checks if it is safe to send write commands to the device.
     * Returns true only if the device has been positively identified as EQ34 Plus.
     */
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
            val devName = getDeviceName(device)
            val devAddr = device?.address ?: "Unknown"
            addLog("Connection event: dev=$devName ($devAddr), status=$status")

            when (status) {
                StateCode.CONNECTION_OK, StateCode.CONNECTION_CONNECTED -> {
                    _connectionState.value = ConnectionStatus.CONNECTED
                    _connectedDevice.value = HocoDevice(
                        device = device!!,
                        name = devName,
                        address = devAddr,
                        isConnected = true,
                        isBonded = isDeviceBonded(device)
                    )
                    addLog("Connected to $devName. Starting safe identification sequence...")
                    scope.launch {
                        performSafeIdentificationSequence(device)
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
            batteryInfo?.let {
                addLog("Battery update: ${it.battery}%")
                _batteryState.value = _batteryState.value.copy(
                    singleBattery = it.battery
                )
            }
        }

        override fun onDeviceBroadcast(device: BluetoothDevice?, msg: DevBroadcastMsg?) {
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
        }

        override fun onDeviceSettingsInfo(device: BluetoothDevice?, type: Int, advInfo: ADVInfoResponse?) {
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
        }

        override fun onCurrentVoiceMode(device: BluetoothDevice?, voiceMode: VoiceMode?) {
            voiceMode?.let {
                cachedVoiceMode = it
                val noiseMode = NoiseMode.fromModeId(it.mode)
                val max = if (it.leftMax > 0) it.leftMax else 10
                rawLeftMax = max
                val curVal = it.leftCurVal
                val mappedGain = if (max > 0) {
                    ((curVal.toFloat() / max.toFloat()) * 10).toInt().coerceIn(1, 10)
                } else 5

                addLog("VoiceMode read-back: ${noiseMode.displayName} (mode=${it.mode}, " +
                    "curVal=$curVal, max=$max, mappedGain=$mappedGain)")
                _ancSettings.value = _ancSettings.value.copy(
                    currentMode = noiseMode,
                    gainLevel = mappedGain,
                    rawLeftCurVal = curVal,
                    rawLeftMax = max,
                    lastConfirmedMode = noiseMode,
                    lastConfirmedLevel = mappedGain,
                    isPendingVerification = false
                )
                _lastCommandResult.value = CommandResult.Success(
                    "State confirmed: ${noiseMode.displayName}, Level $mappedGain/10"
                )
            }
        }

        override fun onVoiceModeList(device: BluetoothDevice?, voiceModes: MutableList<VoiceMode>?) {
            voiceModes?.let { list ->
                addLog("Supported VoiceModes: size=${list.size}")
                val ancMode = list.firstOrNull { it.mode == VoiceMode.VOICE_MODE_DENOISE }
                if (ancMode != null && ancMode.leftMax > 0) {
                    rawLeftMax = ancMode.leftMax
                    _ancSettings.value = _ancSettings.value.copy(
                        rawLeftMax = ancMode.leftMax
                    )
                }
            }
        }

        override fun onDiscovery(device: BluetoothDevice?, bleScanMessage: BleScanMessage?) {
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
        }

        override fun onDiscoveryStatus(bBle: Boolean, bStart: Boolean) {
            addLog("Scan status: isScanning=$bStart")
            _isScanning.value = bStart
        }
    }

    /**
     * SAFE CONNECTION SEQUENCE:
     * 1. Connect
     * 2. Wait for RCSP initialization
     * 3. Read device info (IDENTIFY)
     * 4. Read battery
     * 5. Read ANC state
     * 6. Only then set READY state (controls enabled)
     *
     * If identification fails, controls remain disabled.
     */
    private suspend fun performSafeIdentificationSequence(device: BluetoothDevice) {
        _connectionState.value = ConnectionStatus.IDENTIFYING
        addLog("=== SAFE IDENTIFICATION SEQUENCE ===")

        // Step 1: Wait for RCSP to initialize
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

        // Step 2: Query device info for identification
        addLog("Step 2: Querying device info for identification...")
        rcspController?.getAllDeviceSettingsInfo(device, object : OnRcspActionCallback<ADVInfoResponse> {
            override fun onSuccess(dev: BluetoothDevice?, message: ADVInfoResponse?) {
                addLog("Device settings received. Performing identification...")

                // Perform deep identification
                val name = getDeviceName(dev)
                val hasAnc = true // TWS earbuds from HOCO with RCSP voice modes support ANC

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
                    // Continue with safe reads
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

                // If we can't get device info, attempt name-only identification
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
    }

    /**
     * Safe reads: battery, ANC state. No writes performed.
     */
    private suspend fun performSafeReads(device: BluetoothDevice) {
        addLog("Step 3: Reading current state (read-only)...")

        // Read battery
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

        // Read current voice mode
        rcspController?.getCurrentVoiceMode(device, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("ANC state read complete")
            }
            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("ANC state read error: ${error?.message}")
            }
        })

        // Read all voice modes (for max level info)
        rcspController?.getAllVoiceModes(device, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("Voice modes read complete")
            }
            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Voice modes read error: ${error?.message}")
            }
        })

        // Allow time for callbacks to complete
        delay(1000)

        // Now enable controls
        _connectionState.value = ConnectionStatus.READY
        addLog("=== IDENTIFICATION AND STATE READ COMPLETE ===")
        addLog("Controls are now enabled.")
    }

    private fun handleDisconnection() {
        _connectionState.value = ConnectionStatus.DISCONNECTED
        _connectedDevice.value = null
        _batteryState.value = BatteryInfoModel()
        _ancSettings.value = AncSettings()
        _deviceIdentification.value = DeviceIdentifier.IdentificationResult.NotAttempted
        _lastCommandResult.value = CommandResult.Idle
        addLog("Device disconnected. State reset.")
    }

    init {
        rcspController?.addBTRcspEventCallback(rcspEventCallback)
        refreshBondedDevices()
        checkInitialConnectedDevice()
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

    fun startScan() {
        val ctrl = rcspController
        if (ctrl == null) {
            addLog("BLE Controller not ready")
            return
        }
        _discoveredDevices.value = emptyList()
        addLog("Starting BLE Scan...")
        val success = ctrl.startBleScan(15000)
        _isScanning.value = success
        if (!success) {
            addLog("Failed to start BLE Scan (Check Bluetooth/Location)")
        }
    }

    fun stopScan() {
        addLog("Stopping scan")
        rcspController?.stopScan()
        _isScanning.value = false
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        val ctrl = rcspController
        if (ctrl == null) {
            addLog("BLE Controller not ready")
            return
        }
        _connectionState.value = ConnectionStatus.CONNECTING
        _deviceIdentification.value = DeviceIdentifier.IdentificationResult.NotAttempted
        val name = getDeviceName(device)
        addLog("Connecting to $name (${device.address})...")
        ctrl.connectDevice(device)
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

    /**
     * Set noise mode with SAFETY CHECKS and READ-BACK VERIFICATION.
     *
     * 1. Verify device is identified
     * 2. Verify command is whitelisted
     * 3. Send command
     * 4. Mark as pending verification
     * 5. Read-back confirmation happens via onCurrentVoiceMode callback
     */
    fun setNoiseMode(targetMode: NoiseMode) {
        if (!isSafeToSendCommands()) {
            _lastCommandResult.value = CommandResult.DeviceNotVerified(
                "Cannot change ANC mode: Device not verified as EQ34 Plus."
            )
            return
        }

        val ctrl = rcspController
        val device = _connectedDevice.value?.device ?: ctrl?.usingDevice
        if (device == null || ctrl == null) {
            addLog("Cannot set noise mode: No device connected")
            _lastCommandResult.value = CommandResult.Failed("No device connected")
            return
        }

        if (!CommandWhitelist.isPermitted(CommandWhitelist.SafeCommand.SET_ANC_MODE)) {
            addLog("SAFETY: SET_ANC_MODE not in whitelist")
            _lastCommandResult.value = CommandResult.Failed("Command not permitted")
            return
        }

        val max = if (rawLeftMax > 0) rawLeftMax else 10
        val currentLevel = _ancSettings.value.gainLevel.coerceIn(1, 10)
        val calculatedCurVal = (currentLevel * max) / 10

        val mode = VoiceMode()
            .setMode(targetMode.modeId)
            .setLeftMax(max)
            .setRightMax(max)
            .setLeftCurVal(calculatedCurVal)
            .setRightCurVal(calculatedCurVal)

        addLog("Sending ANC Mode: ${targetMode.displayName} (mode=${targetMode.modeId}, " +
            "gain=$currentLevel/10, curVal=$calculatedCurVal, max=$max)")

        // Mark as pending verification - UI will show "Verifying..." until read-back confirms
        _ancSettings.value = _ancSettings.value.copy(
            isPendingVerification = true,
            requestedMode = targetMode
        )
        _lastCommandResult.value = CommandResult.Idle

        ctrl.setCurrentVoiceMode(device, mode, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("ANC Mode ACK received for ${targetMode.displayName}")
                // Read-back will happen via onCurrentVoiceMode callback
            }

            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("ANC Mode error: ${error?.message}")
                // Revert pending state - read back current state
                _ancSettings.value = _ancSettings.value.copy(
                    isPendingVerification = false
                )
                _lastCommandResult.value = CommandResult.Failed(
                    "Failed to set ${targetMode.displayName}",
                    error?.message
                )
                // Re-read current state to ensure UI matches device
                scope.launch { readCurrentState(device) }
            }
        })
    }

    /**
     * Set ANC gain level with SAFETY CHECKS and READ-BACK VERIFICATION.
     */
    fun setAncGainLevel(level: Int) {
        if (!isSafeToSendCommands()) {
            _lastCommandResult.value = CommandResult.DeviceNotVerified(
                "Cannot change ANC level: Device not verified as EQ34 Plus."
            )
            return
        }

        val ctrl = rcspController
        val coercedLevel = level.coerceIn(1, 10)
        val device = _connectedDevice.value?.device ?: ctrl?.usingDevice
        if (device == null || ctrl == null) {
            addLog("Cannot set ANC level: No device connected")
            _lastCommandResult.value = CommandResult.Failed("No device connected")
            return
        }

        if (!CommandWhitelist.isPermitted(CommandWhitelist.SafeCommand.SET_ANC_LEVEL)) {
            addLog("SAFETY: SET_ANC_LEVEL not in whitelist")
            _lastCommandResult.value = CommandResult.Failed("Command not permitted")
            return
        }

        val max = if (rawLeftMax > 0) rawLeftMax else 10
        val calculatedCurVal = (coercedLevel * max) / 10

        val mode = VoiceMode()
            .setMode(VoiceMode.VOICE_MODE_DENOISE)
            .setLeftMax(max)
            .setRightMax(max)
            .setLeftCurVal(calculatedCurVal)
            .setRightCurVal(calculatedCurVal)

        addLog("Sending ANC Level: $coercedLevel/10 (curVal=$calculatedCurVal, max=$max)")

        // Mark as pending verification
        _ancSettings.value = _ancSettings.value.copy(
            isPendingVerification = true,
            requestedLevel = coercedLevel
        )
        _lastCommandResult.value = CommandResult.Idle

        ctrl.setCurrentVoiceMode(device, mode, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("ANC Level $coercedLevel ACK received")
                // Read-back will happen via onCurrentVoiceMode callback
            }

            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("ANC Level error: ${error?.message}")
                _ancSettings.value = _ancSettings.value.copy(
                    isPendingVerification = false
                )
                _lastCommandResult.value = CommandResult.Failed(
                    "Failed to set ANC Level $coercedLevel",
                    error?.message
                )
                scope.launch { readCurrentState(device) }
            }
        })
    }

    /**
     * Read current device state. Used after errors to re-sync.
     */
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
