package com.hoco.eq34

import android.app.Application
import android.util.Log
import com.jieli.bluetooth.bean.BluetoothOption
import com.jieli.bluetooth.impl.rcsp.RCSPController
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HocoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Capture unhandled exceptions to file so we can diagnose crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val crashLog = "=== CRASH $timestamp ===\nThread: ${thread.name}\n$sw\n\n"
                val file = File(filesDir, "crash.log")
                file.appendText(crashLog)
                Log.e("HocoApplication", "Uncaught exception on ${thread.name}", throwable)
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        if (!RCSPController.isInit()) {
            try {
                val bluetoothOption = BluetoothOption.createDefaultOption()
                    .setUseMultiDevice(true)
                    .setReconnect(true)
                    .setPriority(BluetoothOption.PREFER_BLE)
                    .setMandatoryUseBLE(true)
                    .setMtu(509)
                    .setUseDeviceAuth(true)
                    .setBleScanMode(2)
                RCSPController.init(this, bluetoothOption)
            } catch (e: UnsatisfiedLinkError) {
                Log.w("HocoApplication", "Native library not available", e)
            } catch (e: Throwable) {
                Log.e("HocoApplication", "Failed to init RCSPController", e)
            }
        }
    }
}
