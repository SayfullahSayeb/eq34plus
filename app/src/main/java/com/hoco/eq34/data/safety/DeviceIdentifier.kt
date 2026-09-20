package com.hoco.eq34.data.safety

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

/**
 * Device identification for HOCO EQ34 Plus.
 *
 * This class implements a multi-factor identification check before allowing
 * any control commands to be sent to a Bluetooth device.
 *
 * SAFETY: If identification fails, ALL write controls are disabled.
 */
object DeviceIdentifier {

    /**
     * Known HOCO EQ34 Plus identifiers.
     * These are derived from the JieLi RCSP SDK and HOCO product line.
     */
    private val EQ34_PLUS_NAME_PATTERNS = listOf(
        "EQ34",
        "HOCO",
        "AC7003D4",
    )

    /**
     * JieLi RCSP device types that correspond to TWS earbuds with ANC.
     * The JieLi SDK uses these internally to classify devices.
     */
    private val JIELI_TWS_TYPES = setOf(
        0,  // Generic TWS
        1,  // TWS with ANC
    )

    /**
     * Result of device identification.
     */
    sealed class IdentificationResult {
        /** Device positively identified as HOCO EQ34 Plus. */
        data class Verified(
            val deviceName: String,
            val address: String,
            val protocolVersion: String? = null,
            val productName: String? = null,
            val hasAnc: Boolean = true,
        ) : IdentificationResult()

        /** Device could not be positively identified. */
        data class Unverified(
            val deviceName: String,
            val address: String,
            val reason: String,
        ) : IdentificationResult()

        /** Identification not yet attempted. */
        data object NotAttempted : IdentificationResult()
    }

    /**
     * Performs initial name-based identification during scan.
     * This is a lightweight pre-check before connection.
     */
    fun identifyFromScan(device: BluetoothDevice, deviceName: String?): IdentificationResult {
        val name = deviceName?.trim() ?: return IdentificationResult.Unverified(
            deviceName = "null",
            address = device.address ?: "unknown",
            reason = "Device has no name"
        )

        val nameUpper = name.uppercase()
        val isHoco = EQ34_PLUS_NAME_PATTERNS.any { pattern ->
            nameUpper.contains(pattern.uppercase())
        }

        return if (isHoco) {
            IdentificationResult.Verified(
                deviceName = name,
                address = device.address ?: "unknown",
            )
        } else {
            IdentificationResult.Unverified(
                deviceName = name,
                address = device.address ?: "unknown",
                reason = "Device name does not match EQ34 Plus patterns"
            )
        }
    }

    /**
     * Performs deep identification after RCSP connection is established.
     * This reads device info and verifies the device is actually an EQ34 Plus.
     *
     * Must be called AFTER RCSP initialization completes.
     */
    @SuppressLint("MissingPermission")
    fun identifyFromDeviceInfo(
        device: BluetoothDevice,
        deviceName: String?,
        protocolVersion: String?,
        productName: String?,
        hasAncFeature: Boolean,
    ): IdentificationResult {
        val name = deviceName ?: "unknown"

        // Check 1: Name should match known patterns
        val nameMatches = EQ34_PLUS_NAME_PATTERNS.any { pattern ->
            name.uppercase().contains(pattern.uppercase())
        }

        // Check 2: Device must support ANC
        if (!hasAncFeature) {
            return IdentificationResult.Unverified(
                deviceName = name,
                address = device.address ?: "unknown",
                reason = "Device does not report ANC capability"
            )
        }

        // Check 3: Name should be a known HOCO/EQ34 pattern
        if (!nameMatches) {
            return IdentificationResult.Unverified(
                deviceName = name,
                address = device.address ?: "unknown",
                reason = "Device name '$name' does not match known EQ34 Plus patterns. " +
                    "This may be an unsupported device. Control commands are disabled for safety."
            )
        }

        // All checks passed
        return IdentificationResult.Verified(
            deviceName = name,
            address = device.address ?: "unknown",
            protocolVersion = protocolVersion,
            productName = productName,
            hasAnc = hasAncFeature,
        )
    }

    /**
     * Returns true if the identification result indicates a verified EQ34 Plus.
     */
    fun isVerified(result: IdentificationResult): Boolean {
        return result is IdentificationResult.Verified
    }
}

