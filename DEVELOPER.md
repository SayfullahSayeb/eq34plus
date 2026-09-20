# Developer Guide

## Project

- **Package**: `com.hoco.eq34`
- **GitHub**: `https://github.com/SayfullahSayeb/eq34plus`
- **SDK**: compileSdk 36, minSdk 24, JDK 17, Gradle 9.3.1, AGP 9.1.1, Kotlin 2.2.10

## Release Workflow

CI builds only on tag push. Regular pushes do nothing.

```bash
# Edit code, then:
# 1. Bump versionCode/versionName in app/build.gradle.kts
# 2. Commit + tag + push
git add -A
git commit -m "v1.0.X: description"
git tag v1.0.X
git push origin main --tags
```

CI will build the release APK and create a GitHub Release automatically.
APK: https://github.com/SayfullahSayeb/eq34plus/releases

No local builds. CI produces everything.

## Project Structure

```
app/src/main/java/com/hoco/eq34/
├── data/
│   ├── HocoBleController.kt        # BLE scan, connect, RCSP commands
│   ├── model/
│   │   ├── AncModel.kt             # Noise level mapping (0-10 ↔ device levels)
│   │   ├── BatteryInfoModel.kt     # Battery state
│   │   └── HocoDevice.kt           # Device model
│   └── safety/
│       ├── CommandWhitelist.kt     # 7-command whitelist
│       └── DeviceIdentifier.kt     # Device verification
├── ui/
│   ├── HocoViewModel.kt            # ViewModel
│   ├── components/
│   │   ├── NoiseControlComponents.kt  # Slider + mode buttons
│   │   └── BatteryLevelBar.kt      # Battery display
│   └── screens/
│       └── MainControllerScreen.kt # Main screen
├── HocoApplication.kt             # SDK init, crash handler
└── MainActivity.kt                # Entry point
```

## Key Technical Details

### RCSP Callback (Critical)

`rcspController.addBTRcspEventCallback(rcspEventCallback)` must be called in `connectToDevice()`.
Without it, no events reach the app — discovery, connection, battery, ANC all fail silently.

### BluetoothOption (matching `b78.java`)

```kotlin
useMultiDevice(true)
reconnect(false)
priority = PREFER_BLE
mandatoryUseBLE = false
mtu = 509
useDeviceAuth = true
bleScanMode = 2
```

### Permissions

- Android 12+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`
- Requested on scan tap only, not on startup
- No `neverForLocation` on `BLUETOOTH_SCAN`
- `BLUETOOTH`/`BLUETOOTH_ADMIN` with `maxSdkVersion="30"`

### ANC Control

- Slider 0-10 + 3 mode buttons (Transparency / Standard / ANC)
- Progress 0-4: Transparency, 5: Standard, 6-10: ANC
- Level formula: `leftCurVal = (progress * step) + step/2` where `step = leftMax / 5`
- VoiceMode (9 bytes): `[0]=mode`, `[1-2]=leftMax`, `[3-4]=rightMax`, `[5-6]=leftCurVal`, `[7-8]=rightCurVal`

## Reference

Decompiled original APK at `hoco_apk_analysis/jadx_output/sources/`:

| File | Maps to |
|------|---------|
| `b78.java` | BluetoothOption config → `HocoApplication.kt` |
| `d78.java` | ANC level calc → `NoiseControlPositions` |
| `ii8.java` | Progress bar 0-10 → `NoiseControlState` |
| `c48.java` | Mode button presets → `NoiseModeButton` |
