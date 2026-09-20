package com.hoco.eq34

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.hoco.eq34.ui.HocoViewModel
import com.hoco.eq34.ui.screens.MainControllerScreen
import com.hoco.eq34.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: HocoViewModel by viewModels()

  private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { results ->
    val allGranted = results.values.all { it }
    viewModel.updatePermissionsGranted(allGranted)
    if (allGranted) {
      viewModel.refreshBondedDevices()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    )
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
    requestBluetoothPermissions()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MainControllerScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }

  private fun requestBluetoothPermissions() {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
      )
    } else {
      arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
      )
    }

    val allGranted = permissions.all {
      ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    if (allGranted) {
      viewModel.updatePermissionsGranted(true)
    } else {
      permissionLauncher.launch(permissions)
    }
  }
}
