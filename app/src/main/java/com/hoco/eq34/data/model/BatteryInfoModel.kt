package com.hoco.eq34.data.model

data class BatteryInfoModel(
    val leftBattery: Int = -1,
    val isLeftCharging: Boolean = false,
    val rightBattery: Int = -1,
    val isRightCharging: Boolean = false,
    val caseBattery: Int = -1,
    val isCaseCharging: Boolean = false,
    val singleBattery: Int = -1
) {
    val hasEarbudsData: Boolean
        get() = leftBattery >= 0 || rightBattery >= 0

    val hasCaseData: Boolean
        get() = caseBattery >= 0

    val hasAnyData: Boolean
        get() = hasEarbudsData || hasCaseData || singleBattery >= 0
}

