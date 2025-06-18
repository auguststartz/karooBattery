package com.karoo.battery

import kotlinx.serialization.Serializable

@Serializable
data class BatteryData(
    val level: Int,
    val health: BatteryHealth,
    val temperature: Float,
    val voltage: Int,
    val isCharging: Boolean,
    val chargingMethod: ChargingMethod,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class BatteryHealth {
    UNKNOWN,
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    UNSPECIFIED_FAILURE,
    COLD
}

@Serializable
enum class ChargingMethod {
    NONE,
    AC,
    USB,
    WIRELESS
}