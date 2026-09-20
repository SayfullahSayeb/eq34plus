package com.hoco.eq34.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hoco.eq34.BuildConfig
import com.hoco.eq34.data.ConnectionStatus
import com.hoco.eq34.data.HocoBleController
import com.hoco.eq34.data.model.BatteryInfoModel
import com.hoco.eq34.data.model.HocoDevice
import com.hoco.eq34.data.model.NoiseControlState
import com.hoco.eq34.data.model.NoiseMode
import com.hoco.eq34.data.safety.DeviceIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL

class HocoViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = HocoBleController.getInstance(application)

    val connectionState: StateFlow<ConnectionStatus> = controller.connectionState
    val connectedDevice: StateFlow<HocoDevice?> = controller.connectedDevice
    val batteryState: StateFlow<BatteryInfoModel> = controller.batteryState
    val noiseState: StateFlow<NoiseControlState> = controller.noiseState
    val discoveredDevices: StateFlow<List<HocoDevice>> = controller.discoveredDevices
    val pairedDevices: StateFlow<List<HocoDevice>> = controller.pairedDevices
    val isScanning: StateFlow<Boolean> = controller.isScanning
    val logMessages: StateFlow<List<String>> = controller.logMessages
    val deviceIdentification: StateFlow<DeviceIdentifier.IdentificationResult> = controller.deviceIdentification
    val lastCommandResult: StateFlow<HocoBleController.CommandResult> = controller.lastCommandResult

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    private val _latestVersion = MutableStateFlow("")
    val latestVersion: StateFlow<String> = _latestVersion.asStateFlow()

    private val _hasUpdate = MutableStateFlow(false)
    val hasUpdate: StateFlow<Boolean> = _hasUpdate.asStateFlow()

    init {
        if (_hasPermissions.value) {
            controller.refreshBondedDevices()
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/SayfullahSayeb/eq34plus/releases/latest")
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val json = connection.getInputStream().bufferedReader().use { it.readText() }
                val tagName = JSONObject(json).getString("tag_name")
                _latestVersion.value = tagName
                _hasUpdate.value = tagName != currentVersion && tagName.isNotEmpty()
            } catch (_: Exception) {
            }
        }
    }

    fun updatePermissionsGranted(granted: Boolean) {
        _hasPermissions.value = granted
        if (granted) {
            controller.refreshBondedDevices()
        }
    }

    fun startScan() {
        controller.startScan()
    }

    fun stopScan() {
        controller.stopScan()
    }

    fun refreshBondedDevices() {
        controller.refreshBondedDevices()
    }

    fun connect(device: BluetoothDevice) {
        controller.connect(device)
    }

    fun disconnect() {
        controller.disconnect()
    }

    fun setNoiseProgress(progress: Int) {
        controller.setNoiseProgress(progress)
    }

    fun setStandardMode() {
        controller.setStandardMode()
    }

    fun renameDevice(newName: String) {
        controller.renameDevice(newName)
    }
}
