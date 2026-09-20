package com.example.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import com.example.data.ConnectionStatus
import com.example.data.HocoBleController
import com.example.data.model.AncSettings
import com.example.data.model.BatteryInfoModel
import com.example.data.model.HocoDevice
import com.example.data.model.NoiseMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HocoViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = HocoBleController.getInstance(application)

    val connectionState: StateFlow<ConnectionStatus> = controller.connectionState
    val connectedDevice: StateFlow<HocoDevice?> = controller.connectedDevice
    val batteryState: StateFlow<BatteryInfoModel> = controller.batteryState
    val ancSettings: StateFlow<AncSettings> = controller.ancSettings
    val discoveredDevices: StateFlow<List<HocoDevice>> = controller.discoveredDevices
    val pairedDevices: StateFlow<List<HocoDevice>> = controller.pairedDevices
    val isScanning: StateFlow<Boolean> = controller.isScanning
    val logMessages: StateFlow<List<String>> = controller.logMessages

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

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

    fun setNoiseMode(mode: NoiseMode) {
        controller.setNoiseMode(mode)
    }

    fun setAncLevel(level: Int) {
        controller.setAncGainLevel(level)
    }
}
