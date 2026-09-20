# HOCO EQ34 Plus Controller

Lightweight Android app (18 MB) to control HOCO EQ34 Plus TWS earbuds via Bluetooth, replacing the 147 MB official HOCO Music app.

## Features

- **Noise Control** — ANC, Transparency, Standard with 11-level adjustment
- **Battery** — Left earbud, right earbud, and case battery levels
- **Device Rename** — Change your earbuds' Bluetooth name
- **Connection Popup** — Animated slide-up when earbuds are detected
- **Safe** — Device identification + command whitelist, no firmware operations

## Comparison

| Feature | Official (147 MB) | This App (2 MB) |
|---|---|---|
| Noise Control (ANC/Transparency/Standard) | ✅ | ✅ |
| 11-Level Adjustment | ✅ | ✅ |
| Battery (L/R/Case) | ✅ | ✅ |
| Device Rename | ✅ | ✅ |
| Connect Popup | ✅ | ✅ |
| Read-back Verification | ✅ | ✅ |
| EQ Settings | ✅ | ❌ |
| Touch Key Customization | ✅ | ❌ |
| Firmware OTA Update | ✅ | ❌ (blocked for safety) |
| 50+ HOCO Models | ✅ | ❌ (EQ34 Plus only) |

**You get what you need daily.** Missing features (EQ, touch keys, OTA) require the official app and are rarely used.


## Download

Go to [Releases](../../releases) and download the APK. Enable "Install from unknown sources" in Android settings.

## How It Works

1. App scans for nearby BLE devices
2. When EQ34 Plus case is opened, popup appears
3. Tap Connect — app identifies device and enables controls
4. Use noise control slider or mode buttons
5. Battery levels update in real-time

## Credits

Built using [JieLi RCSP SDK](https://www.jielibluetooth.com/) and reverse-engineered from the official HOCO Music APK.

## License

Educational project. Use at your own risk.
