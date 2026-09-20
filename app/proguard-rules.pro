# JieLi Bluetooth SDK - Keep all classes as they use reflection and JNI
-keep class com.jieli.bluetooth.** { *; }
-keep class com.jieli.filebrowse.** { *; }

# Keep Gson serialized classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.jieli.bluetooth.bean.** { *; }
-keep class com.jieli.bluetooth.bean.device.** { *; }
-keep class com.jieli.bluetooth.bean.response.** { *; }
-keep class com.jieli.bluetooth.bean.command.** { *; }
-keep class com.jieli.bluetooth.bean.settings.** { *; }
-keep class com.jieli.bluetooth.bean.configuration.** { *; }

# Keep VoiceMode for ANC control
-keep class com.jieli.bluetooth.bean.base.VoiceMode { *; }

# Keep callbacks
-keep class com.jieli.bluetooth.interfaces.rcsp.callback.** { *; }
-keep class com.jieli.bluetooth.interfaces.rcsp.ITwsOp { *; }
-keep class com.jieli.bluetooth.interfaces.rcsp.IRcspOp { *; }
-keep class com.jieli.bluetooth.interfaces.rcsp.IRcspControl { *; }

# Keep RCSPController
-keep class com.jieli.bluetooth.impl.rcsp.RCSPController { *; }
-keep class com.jieli.bluetooth.impl.rcsp.TwsOpImpl { *; }

# Keep BLE manager
-keep class com.jieli.bluetooth.impl.JL_BluetoothManager { *; }
-keep class com.jieli.bluetooth.impl.BluetoothBle { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# R8 auto-generated rules
-dontwarn com.jieli.bt.decryption.**
-dontwarn com.jieli.bluetooth.utils.CryptoUtil

# SAFETY: Exclude firmware/OTA/flash classes from the SDK
# These classes are dangerous and must never be used
-dontwarn com.jieli.bluetooth.bean.command.ota.**
-dontwarn com.jieli.bluetooth.bean.parameter.flash.**
-dontwarn com.jieli.bluetooth.bean.parameter.FirmwareUpdate**
-dontwarn com.jieli.bluetooth.bean.parameter.InquireUpdate**
-dontwarn com.jieli.bluetooth.bean.parameter.NotifyUpdate**
-dontwarn com.jieli.bluetooth.bean.parameter.RebootDevice**
-dontwarn com.jieli.bluetooth.bean.response.EnterUpdateMode**
-dontwarn com.jieli.bluetooth.bean.response.ExitUpdateMode**
-dontwarn com.jieli.bluetooth.bean.response.FirmwareUpdate**
-dontwarn com.jieli.bluetooth.bean.response.InquireUpdate**
-dontwarn com.jieli.bluetooth.bean.response.RebootDevice**
-dontwarn com.jieli.bluetooth.bean.response.UpdateFileOffset**
-dontwarn com.jieli.bluetooth.bean.device.OTA**
-dontwarn com.jieli.bluetooth.impl.rcsp.file.**
-dontwarn com.jieli.bluetooth.impl.rcsp.task.**
-dontwarn com.jieli.bluetooth.impl.rcsp.data_transfer.**
-dontwarn com.jieli.bluetooth.impl.rcsp.translation.**
-dontwarn com.jieli.bluetooth.impl.rcsp.record.**
-dontwarn com.jieli.bluetooth.impl.rcsp.auracast.**
-dontwarn com.jieli.bluetooth.impl.rcsp.charging_case.**
-dontwarn com.jieli.bluetooth.impl.rcsp.FMControlImpl
-dontwarn com.jieli.bluetooth.impl.rcsp.AuxControlImpl
-dontwarn com.jieli.bluetooth.impl.rcsp.LightControlImpl
-dontwarn com.jieli.bluetooth.impl.rcsp.PCSlaveImpl
-dontwarn com.jieli.bluetooth.impl.rcsp.SoundCardImpl
-dontwarn com.jieli.bluetooth.impl.rcsp.SPDIFImpl
-dontwarn com.jieli.bluetooth.impl.rcsp.RTCImpl
-dontwarn com.jieli.bluetooth.impl.rcsp.MusicControlImpl
-dontwarn com.jieli.bluetooth.impl.rcsp.VolumeControlImpl
-dontwarn com.jieli.filebrowse.**
