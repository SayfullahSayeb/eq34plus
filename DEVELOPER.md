# Developer Guide

How to work on this project. Read this first.

## Project Info

- **Package**: `com.hoco.eq34`
- **App name**: HOCO EQ34
- **GitHub**: `https://github.com/SayfullahSayeb/eq34plus`
- **Branch**: `main`

## How We Work (Follow This)

### Step 1: Make changes
Edit files in the project.

### Step 2: Commit and push (NO local build)
We do NOT build locally. GitHub Actions CI handles building.

```bash
git add -A
git commit -m "v1.0.X: description of changes"
git tag v1.0.X
git push origin main
git push origin v1.0.X
```

That's it. CI builds the APK and creates a GitHub Release automatically.

### Step 3: Check CI build
Go to https://github.com/SayfullahSayeb/eq34plus/actions to see the build status.

### Step 4: Get APK
Download from https://github.com/SayfullahSayeb/eq34plus/releases

## Why No Local Build?

- Local build takes 2-3 minutes and adds no value — CI builds the same thing
- CI produces the release APK that users actually download
- Faster workflow: commit → push → CI builds while we continue working

## Key Rules

1. **Version bump**: Update `versionCode` and `versionName` in `app/build.gradle.kts` before each release tag
2. **Never skip CI**: Always push with a tag so CI builds the release APK
3. **No secrets in git**: Release keystore password is `eq34plus2024`, keystore is NOT in git
4. **One change per push**: If making multiple changes, commit them together or push separately — each tag = one release

## Reference: Decompiled Original APK

Original HOCO Music APK decompiled at `hoco_apk_analysis/jadx_output/sources/` for protocol reference.

### Key references
- `b78.java` — BluetoothOption config (matches our `HocoApplication.kt`)
- `d78.java` — ANC level calculation (matches our `NoiseControlPositions`)
- `ii8.java` — Progress bar mapping (0-10, Transparency/Standard/ANC)
- `c48.java` — Mode button presets (Off→5, ANC→10, Transparency→0)

## Project Structure

```
app/src/main/java/com/hoco/eq34/
├── data/
│   ├── HocoBleController.kt        # BLE scan, connect, RCSP commands, callbacks
│   ├── model/
│   │   ├── AncModel.kt             # Noise level mapping (0-10 ↔ device levels)
│   │   ├── BatteryInfoModel.kt     # Battery state
│   │   └── HocoDevice.kt           # Device model
│   └── safety/
│       ├── CommandWhitelist.kt     # 7-command whitelist
│       └── DeviceIdentifier.kt     # Device verification
├── ui/
│   ├── HocoViewModel.kt            # ViewModel bridging UI ↔ controller
│   ├── components/
│   │   ├── NoiseControlComponents.kt  # Slider + mode buttons
│   │   └── BatteryLevelBar.kt      # Battery display
│   └── screens/
│       └── MainControllerScreen.kt # NotConnectedScreen + ConnectedScreen
├── HocoApplication.kt             # SDK init, crash handler
└── MainActivity.kt                # Entry point
```

## Critical Details (Don't Forget)

### RCSP Callback Registration
Must call `rcspController.addBTRcspEventCallback(rcspEventCallback)` in `connectToDevice()`.
Without this, NO events reach the app (discovery, connection, battery, ANC — all silent).

### BluetoothOption Config (matching `b78.java`)
```kotlin
useMultiDevice(true)
reconnect(false)        // matching original app
priority = PREFER_BLE
mandatoryUseBLE = false
mtu = 509
useDeviceAuth = true    // CRITICAL
bleScanMode = 2
```

### Permissions
- On Android 12+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`
- Requested ONLY when user taps Scan, NOT on startup
- No `neverForLocation` flag on `BLUETOOTH_SCAN`
- `BLUETOOTH`/`BLUETOOTH_ADMIN` with `maxSdkVersion="30"`

### ANC Control
- Slider 0-10 with 3 mode buttons below
- Progress 0-4: Transparency (blue)
- Progress 5: Standard (gray)
- Progress 6-10: ANC (green)
- Level formula: `leftCurVal = (progress * step) + step/2` where `step = leftMax / 5`

### VoiceMode byte layout (9 bytes)
`byte[0]=mode`, `byte[1-2]=leftMax`, `byte[3-4]=rightMax`, `byte[5-6]=leftCurVal`, `byte[7-8]=rightCurVal`
