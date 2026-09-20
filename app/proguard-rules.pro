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
