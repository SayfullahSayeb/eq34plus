# Developer Guide

How to clone, build, and publish this app.

## Clone

```bash
git clone https://github.com/YOUR_USERNAME/eq34plus.git
cd eq34plus
```

## Build

Requires:
- Android SDK (API 36)
- JDK 17

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Publish a Release (Auto Build via GitHub)

### Setup (one time)

1. Push code to GitHub
2. Go to repo → Settings → Actions → General
3. Under "Workflow permissions", select "Read and write permissions"
4. Click Save

### Release a version

```bash
git add .
git commit -m "v1.0.0"
git tag v1.0.0
git push origin main --tags
```

GitHub Actions will automatically:
1. Build debug and release APKs
2. Create a GitHub Release
3. Attach both APKs to the release
4. Generate release notes

Users can then download the APK from your repo's Releases page.

### Without tags

Every push to `main`/`master` also builds the APK and uploads it as an artifact (under Actions tab → click the workflow run → scroll to Artifacts).

## Project Structure

```
app/src/main/java/com/example/
├── data/
│   ├── HocoBleController.kt        # BLE commands
│   ├── model/
│   │   ├── AncModel.kt             # Noise level mapping
│   │   ├── BatteryInfoModel.kt     # Battery state
│   │   └── HocoDevice.kt          # Device model
│   └── safety/
│       ├── CommandWhitelist.kt     # Allowed commands
│       └── DeviceIdentifier.kt     # Device verification
├── ui/
│   ├── HocoViewModel.kt           # ViewModel
│   ├── components/                 # UI components
│   └── screens/
│       └── MainControllerScreen.kt # Main screen
├── HocoApplication.kt             # App init
└── MainActivity.kt                # Entry point
```

## Adding Features

### To add a new BLE command

1. Add to `SafeCommand` enum in `CommandWhitelist.kt`
2. Add to `permittedCommands` set
3. Add the method in `HocoBleController.kt`
4. Add proxy in `HocoViewModel.kt`
5. Wire to UI

### To change ANC level calculation

Edit `NoiseControlPositions.deviceLevelForProgress()` in `AncModel.kt`. Formula matches `d78.java` in the official APK.

### Reference files (not in git)

- `hoco_apk_analysis/` — Decompiled official APK
- `tools/jadx/` — JADX decompiler
- `hoco.music_1.3.5-gp.apks` — Original APK

These are gitignored but kept locally for reference during development.
