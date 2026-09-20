package com.example.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.example.data.model.AncSettings
import com.example.data.model.BatteryInfoModel
import com.example.data.model.HocoDevice
import com.example.data.model.NoiseMode
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
    CONNECTED
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

    private var cachedVoiceMode: VoiceMode? = null

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
                    addLog("Successfully connected to $devName")
                    // Query current voice mode and settings
                    queryDeviceStatus(device)
                }
                StateCode.CONNECTION_CONNECTING -> {
                    _connectionState.value = ConnectionStatus.CONNECTING
                }
                StateCode.CONNECTION_DISCONNECT, StateCode.CONNECTION_FAILED -> {
                    _connectionState.value = ConnectionStatus.DISCONNECTED
                    _connectedDevice.value = null
                    addLog("Disconnected from device")
                }
            }
        }

        override fun onBatteryChange(device: BluetoothDevice?, batteryInfo: BatteryInfo?) {
            batteryInfo?.let {
                addLog("Single Battery update: ${it.battery}%")
                _batteryState.value = _batteryState.value.copy(
                    singleBattery = it.battery
                )
            }
        }

        override fun onDeviceBroadcast(device: BluetoothDevice?, msg: DevBroadcastMsg?) {
            msg?.let {
                addLog("Broadcast update: L=${it.leftDeviceQuantity}% (charging=${it.isLeftCharging}), R=${it.rightDeviceQuantity}% (charging=${it.isRightCharging}), Case=${it.chargingBinQuantity}% (charging=${it.isDeviceCharging})")
                _batteryState.value = _batteryState.value.copy(
                    leftBattery = if (it.leftDeviceQuantity in 0..100) it.leftDeviceQuantity else _batteryState.value.leftBattery,
                    isLeftCharging = it.isLeftCharging,
                    rightBattery = if (it.rightDeviceQuantity in 0..100) it.rightDeviceQuantity else _batteryState.value.rightBattery,
                    isRightCharging = it.isRightCharging,
                    caseBattery = if (it.chargingBinQuantity in 0..100) it.chargingBinQuantity else _batteryState.value.caseBattery,
                    isCaseCharging = it.isDeviceCharging
                )
            }
        }

        override fun onDeviceSettingsInfo(device: BluetoothDevice?, type: Int, advInfo: ADVInfoResponse?) {
            advInfo?.let {
                addLog("Settings info received: L=${it.leftDeviceQuantity}%, R=${it.rightDeviceQuantity}%, Case=${it.chargingBinQuantity}%")
                _batteryState.value = _batteryState.value.copy(
                    leftBattery = if (it.leftDeviceQuantity in 0..100) it.leftDeviceQuantity else _batteryState.value.leftBattery,
                    isLeftCharging = it.isLeftCharging,
                    rightBattery = if (it.rightDeviceQuantity in 0..100) it.rightDeviceQuantity else _batteryState.value.rightBattery,
                    isRightCharging = it.isRightCharging,
                    caseBattery = if (it.chargingBinQuantity in 0..100) it.chargingBinQuantity else _batteryState.value.caseBattery
                )
            }
        }

        override fun onCurrentVoiceMode(device: BluetoothDevice?, voiceMode: VoiceMode?) {
            voiceMode?.let {
                cachedVoiceMode = it
                val noiseMode = NoiseMode.fromModeId(it.mode)
                val max = if (it.leftMax > 0) it.leftMax else 10
                val curVal = it.leftCurVal
                val mappedGain = if (max > 0) {
                    ((curVal.toFloat() / max.toFloat()) * 10).toInt().coerceIn(1, 10)
                } else 5

                addLog("Current VoiceMode: ${noiseMode.displayName} (mode=${it.mode}, leftCur=$curVal, leftMax=$max)")
                _ancSettings.value = _ancSettings.value.copy(
                    currentMode = noiseMode,
                    gainLevel = mappedGain,
                    rawLeftCurVal = curVal,
                    rawLeftMax = max
                )
            }
        }

        override fun onVoiceModeList(device: BluetoothDevice?, voiceModes: MutableList<VoiceMode>?) {
            voiceModes?.let { list ->
                addLog("Supported VoiceModes list: size=${list.size}")
                val ancMode = list.firstOrNull { it.mode == VoiceMode.VOICE_MODE_DENOISE }
                if (ancMode != null && ancMode.leftMax > 0) {
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

            // If BleScanMessage includes battery info from broadcast packets, update it
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
            // Sort to show HOCO devices at the top
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
        if (list.size > 25) {
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
            queryDeviceStatus(using)
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
        val name = getDeviceName(device)
        addLog("Connecting to $name (${device.address})...")
        ctrl.connectDevice(device)
    }

    fun disconnect() {
        val ctrl = rcspController
        val dev = _connectedDevice.value?.device ?: ctrl?.usingDevice
        if (dev != null && ctrl != null) {
            addLog("Disconnecting from ${dev.address}...")
            ctrl.disconnectDevice(dev)
        }
        _connectionState.value = ConnectionStatus.DISCONNECTED
        _connectedDevice.value = null
        _batteryState.value = BatteryInfoModel()
    }

    fun queryDeviceStatus(device: BluetoothDevice) {
        val ctrl = rcspController ?: return
        addLog("Querying voice modes and settings...")
        ctrl.getCurrentVoiceMode(device, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("Query current voice mode success")
            }

            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Query current voice mode error: ${error?.message}")
            }
        })

        ctrl.getAllVoiceModes(device, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("Query all voice modes success")
            }

            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Query all voice modes error: ${error?.message}")
            }
        })

        ctrl.getAllDeviceSettingsInfo(device, object : OnRcspActionCallback<ADVInfoResponse> {
            override fun onSuccess(dev: BluetoothDevice?, message: ADVInfoResponse?) {
                addLog("Query all device settings success")
                message?.let { adv ->
                    _batteryState.value = _batteryState.value.copy(
                        leftBattery = if (adv.leftDeviceQuantity in 0..100) adv.leftDeviceQuantity else _batteryState.value.leftBattery,
                        isLeftCharging = adv.isLeftCharging,
                        rightBattery = if (adv.rightDeviceQuantity in 0..100) adv.rightDeviceQuantity else _batteryState.value.rightBattery,
                        isRightCharging = adv.isRightCharging,
                        caseBattery = if (adv.chargingBinQuantity in 0..100) adv.chargingBinQuantity else _batteryState.value.caseBattery
                    )
                }
            }

            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Query all device settings error: ${error?.message}")
            }
        })
    }

    fun setNoiseMode(targetMode: NoiseMode) {
        val ctrl = rcspController
        val device = _connectedDevice.value?.device ?: ctrl?.usingDevice
        if (device == null || ctrl == null) {
            addLog("Cannot set noise mode: No device connected")
            return
        }

        val max = if (_ancSettings.value.rawLeftMax > 0) _ancSettings.value.rawLeftMax else 10
        val currentLevel = _ancSettings.value.gainLevel.coerceIn(1, 10)
        val calculatedCurVal = (currentLevel * max) / 10

        val mode = VoiceMode()
            .setMode(targetMode.modeId)
            .setLeftMax(max)
            .setRightMax(max)
            .setLeftCurVal(calculatedCurVal)
            .setRightCurVal(calculatedCurVal)

        addLog("Sending ANC Mode: ${targetMode.displayName} (mode=${targetMode.modeId}, gain=$currentLevel/10, curVal=$calculatedCurVal, max=$max)")
        
        // Optimistic UI update
        _ancSettings.value = _ancSettings.value.copy(
            currentMode = targetMode,
            rawLeftCurVal = calculatedCurVal
        )

        ctrl.setCurrentVoiceMode(device, mode, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("Set VoiceMode ${targetMode.displayName} ACK received")
            }

            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Set VoiceMode error: ${error?.message}")
            }
        })
    }

    fun setAncGainLevel(level: Int) {
        val ctrl = rcspController
        val coercedLevel = level.coerceIn(1, 10)
        val device = _connectedDevice.value?.device ?: ctrl?.usingDevice
        if (device == null || ctrl == null) {
            addLog("Cannot set ANC level: No device connected")
            return
        }

        val max = if (_ancSettings.value.rawLeftMax > 0) _ancSettings.value.rawLeftMax else 10
        val calculatedCurVal = (coercedLevel * max) / 10

        val mode = VoiceMode()
            .setMode(VoiceMode.VOICE_MODE_DENOISE)
            .setLeftMax(max)
            .setRightMax(max)
            .setLeftCurVal(calculatedCurVal)
            .setRightCurVal(calculatedCurVal)

        addLog("Sending ANC Gain Level: $coercedLevel/10 (curVal=$calculatedCurVal, max=$max)")

        _ancSettings.value = _ancSettings.value.copy(
            currentMode = NoiseMode.ANC,
            gainLevel = coercedLevel,
            rawLeftCurVal = calculatedCurVal
        )

        ctrl.setCurrentVoiceMode(device, mode, object : OnRcspActionCallback<Boolean> {
            override fun onSuccess(dev: BluetoothDevice?, message: Boolean?) {
                addLog("ANC Level $coercedLevel ACK received")
            }

            override fun onError(dev: BluetoothDevice?, error: BaseError?) {
                addLog("Set ANC Level error: ${error?.message}")
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
