package com.example

import android.app.Application
import com.jieli.bluetooth.bean.BluetoothOption
import com.jieli.bluetooth.constant.BluetoothConstant
import com.jieli.bluetooth.impl.rcsp.RCSPController

class HocoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!RCSPController.isInit()) {
            try {
                val bluetoothOption = BluetoothOption.createDefaultOption()
                    .setUseMultiDevice(false)
                    .setPriority(BluetoothOption.PREFER_BLE)
                    .setMandatoryUseBLE(false)
                    .setMtu(BluetoothConstant.BLE_MTU_MAX)
                    .setUseDeviceAuth(false)
                    .setBleScanMode(2)
                RCSPController.init(this, bluetoothOption)
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.w("HocoApplication", "Native library jl_bluetooth not available in host JVM environment (normal in unit tests)", e)
            } catch (e: Throwable) {
                android.util.Log.e("HocoApplication", "Failed to init RCSPController", e)
            }
        }
    }
}
