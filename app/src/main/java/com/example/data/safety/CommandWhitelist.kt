package com.example.data.safety

/**
 * Whitelist of safe runtime-control commands that may be sent to the EQ34 Plus.
 *
 * SAFETY PRINCIPLE: Only commands explicitly listed here are permitted.
 * Everything else is rejected. This prevents accidental firmware modification,
 * flash erasure, or any destructive operation.
 *
 * SAFE COMMANDS (runtime control only):
 * - READ_DEVICE_INFO: Query device identity and capabilities
 * - READ_BATTERY: Query battery levels
 * - READ_ANC_STATE: Query current ANC mode and level
 * - READ_VOICE_MODES: Query available voice modes
 * - SET_ANC_MODE: Set ANC mode (Off/ANC/Transparency)
 * - SET_ANC_LEVEL: Set ANC intensity level (1-10)
 *
 * DANGEROUS COMMANDS (explicitly blocked):
 * - FIRMWARE_UPDATE / OTA / DFU: Any firmware modification
 * - FLASH_ERASE / FLASH_WRITE: Any flash memory operations
 * - DEVICE_REBOOT: Forced device reboot
 * - RESTORE_FACTORY: Factory reset
 * - Any raw byte writes
 * - Any command not in the whitelist
 */
object CommandWhitelist {

    /**
     * Enumeration of permitted command categories.
     */
    enum class SafeCommand {
        READ_DEVICE_INFO,
        READ_BATTERY,
        READ_ANC_STATE,
        READ_VOICE_MODES,
        SET_ANC_MODE,
        SET_ANC_LEVEL,
    }

    /**
     * Enumeration of explicitly BLOCKED command categories.
     * These are never permitted under any circumstances.
     */
    enum class BlockedCommand {
        FIRMWARE_UPDATE_ENTER_MODE,
        FIRMWARE_UPDATE_SEND_BLOCK,
        FIRMWARE_UPDATE_EXIT_MODE,
        FIRMWARE_REBOOT,
        FLASH_ERASE,
        FLASH_WRITE,
        FLASH_CREATE_FILE,
        FLASH_DELETE_FILE,
        FLASH_RESTORE_SYSTEM,
        DEVICE_CONFIG_NAME,
        DEVICE_CONFIG_KEY_SETTINGS,
        DEVICE_CONFIG_LED_SETTINGS,
        RAW_BYTE_WRITE,
        SEARCH_DEVICE,
        STOP_SEARCH_DEVICE,
        UPDATE_FUNCTION_VALUE,
        MODIFY_DEVICE_SETTINGS,
        ANY_OTA_COMMAND,
        ANY_FLASH_COMMAND,
    }

    private val permittedCommands = setOf(
        SafeCommand.READ_DEVICE_INFO,
        SafeCommand.READ_BATTERY,
        SafeCommand.READ_ANC_STATE,
        SafeCommand.READ_VOICE_MODES,
        SafeCommand.SET_ANC_MODE,
        SafeCommand.SET_ANC_LEVEL,
    )

    /**
     * Check if a command is permitted.
     * Returns true only for commands explicitly in the whitelist.
     */
    fun isPermitted(command: SafeCommand): Boolean {
        return command in permittedCommands
    }

    /**
     * Get a human-readable description of why a command was blocked.
     */
    fun getBlockReason(command: BlockedCommand): String {
        return when (command) {
            BlockedCommand.FIRMWARE_UPDATE_ENTER_MODE,
            BlockedCommand.FIRMWARE_UPDATE_SEND_BLOCK,
            BlockedCommand.FIRMWARE_UPDATE_EXIT_MODE,
            BlockedCommand.FIRMWARE_REBOOT,
            BlockedCommand.ANY_OTA_COMMAND ->
                "Firmware/OTA operations are blocked to prevent device damage."
            BlockedCommand.FLASH_ERASE,
            BlockedCommand.FLASH_WRITE,
            BlockedCommand.FLASH_CREATE_FILE,
            BlockedCommand.FLASH_DELETE_FILE,
            BlockedCommand.FLASH_RESTORE_SYSTEM,
            BlockedCommand.ANY_FLASH_COMMAND ->
                "Flash memory operations are blocked to prevent data loss."
            BlockedCommand.DEVICE_CONFIG_NAME,
            BlockedCommand.DEVICE_CONFIG_KEY_SETTINGS,
            BlockedCommand.DEVICE_CONFIG_LED_SETTINGS ->
                "Device configuration changes are not permitted in this app."
            BlockedCommand.RAW_BYTE_WRITE ->
                "Raw BLE writes are blocked for safety."
            BlockedCommand.SEARCH_DEVICE,
            BlockedCommand.STOP_SEARCH_DEVICE ->
                "Device search commands are not used in this app."
            BlockedCommand.UPDATE_FUNCTION_VALUE,
            BlockedCommand.MODIFY_DEVICE_SETTINGS ->
                "Device settings modification is not permitted."
        }
    }

    /**
     * List of JieLi SDK OTA-related class prefixes that must never be instantiated or called.
     */
    val BLOCKED_CLASS_PREFIXES = listOf(
        "com.jieli.bluetooth.bean.command.ota.",
        "com.jieli.bluetooth.bean.parameter.flash.",
        "com.jieli.bluetooth.bean.parameter.FirmwareUpdate",
        "com.jieli.bluetooth.bean.parameter.InquireUpdate",
        "com.jieli.bluetooth.bean.parameter.NotifyUpdate",
        "com.jieli.bluetooth.bean.parameter.RebootDevice",
        "com.jieli.bluetooth.bean.response.EnterUpdateMode",
        "com.jieli.bluetooth.bean.response.ExitUpdateMode",
        "com.jieli.bluetooth.bean.response.FirmwareUpdate",
        "com.jieli.bluetooth.bean.response.InquireUpdate",
        "com.jieli.bluetooth.bean.response.RebootDevice",
        "com.jieli.bluetooth.bean.response.UpdateFileOffset",
        "com.jieli.bluetooth.bean.device.OTASettings",
        "com.jieli.bluetooth.bean.device.OTAState",
    )
}
