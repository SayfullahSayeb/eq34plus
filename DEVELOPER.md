# Developer Guide

## Project

- **Package**: `com.hoco.eq34`
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

## Reference

Decompiled original APK at `hoco_apk_analysis/jadx_output/sources/`:

| File | Maps to |
|------|---------|
| `b78.java` | BluetoothOption config → `HocoApplication.kt` |
| `d78.java` | ANC level calc → `NoiseControlPositions` |
| `ii8.java` | Progress bar 0-10 → `NoiseControlState` |
| `c48.java` | Mode button presets → `NoiseModeButton` |
