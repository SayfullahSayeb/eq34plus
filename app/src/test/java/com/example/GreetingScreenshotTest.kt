package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.ConnectionStatus
import com.example.data.model.NoiseControlState
import com.example.data.model.BatteryInfoModel
import com.example.data.model.NoiseMode
import com.example.ui.screens.BatterySection
import com.example.ui.screens.ConnectionStatusCard
import com.example.ui.screens.NoiseControlSection
import com.example.ui.screens.TopControllerBar
import com.example.ui.theme.AudioDarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = AudioDarkBackground) {
          TopControllerBar(
            connectionState = ConnectionStatus.CONNECTED,
            connectedDeviceName = "HOCO EQ34 Plus",
            isScanning = false,
            onRefreshClick = {},
            onToggleLogs = {},
            showLogs = false
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

