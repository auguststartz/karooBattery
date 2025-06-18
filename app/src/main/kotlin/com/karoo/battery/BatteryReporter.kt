package com.karoo.battery

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Serializable
data class BatteryReport(
    val reportDate: String,
    val totalSamples: Int,
    val monitoringDurationHours: Double,
    val averageBatteryLevel: Double,
    val minBatteryLevel: Int,
    val maxBatteryLevel: Int,
    val averageTemperature: Double,
    val maxTemperature: Float,
    val minTemperature: Float,
    val averageVoltage: Double,
    val chargingCycles: Int,
    val timeCharging: Double,
    val timeDischarging: Double,
    val healthStatus: BatteryHealth,
    val powerConsumptionRate: Double,
    val batteryDegradation: Double,
    val recommendations: List<String>
)

class BatteryReporter {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val json = Json { prettyPrint = true }
    
    fun generateReport(batteryData: List<BatteryData>): BatteryReport {
        if (batteryData.isEmpty()) {
            throw IllegalArgumentException("Battery data cannot be empty")
        }
        
        val sortedData = batteryData.sortedBy { it.timestamp }
        val firstSample = sortedData.first()
        val lastSample = sortedData.last()
        
        val durationMs = lastSample.timestamp - firstSample.timestamp
        val durationHours = durationMs / (1000.0 * 60.0 * 60.0)
        
        val averageLevel = batteryData.map { it.level }.average()
        val minLevel = batteryData.minOf { it.level }
        val maxLevel = batteryData.maxOf { it.level }
        
        val averageTemp = batteryData.map { it.temperature.toDouble() }.average()
        val maxTemp = batteryData.maxOf { it.temperature }
        val minTemp = batteryData.minOf { it.temperature }
        
        val averageVoltage = batteryData.map { it.voltage.toDouble() }.average()
        
        val chargingCycles = countChargingCycles(sortedData)
        val (chargingTime, dischargingTime) = calculateChargingTime(sortedData)
        
        val healthStatus = batteryData.lastOrNull()?.health ?: BatteryHealth.UNKNOWN
        
        val powerConsumption = calculatePowerConsumptionRate(sortedData)
        val degradation = estimateBatteryDegradation(sortedData)
        
        val recommendations = generateRecommendations(
            averageTemp, maxTemp, chargingTime, dischargingTime, degradation
        )
        
        return BatteryReport(
            reportDate = dateFormat.format(Date()),
            totalSamples = batteryData.size,
            monitoringDurationHours = durationHours,
            averageBatteryLevel = (averageLevel * 100).roundToInt() / 100.0,
            minBatteryLevel = minLevel,
            maxBatteryLevel = maxLevel,
            averageTemperature = (averageTemp * 100).roundToInt() / 100.0,
            maxTemperature = maxTemp,
            minTemperature = minTemp,
            averageVoltage = (averageVoltage * 100).roundToInt() / 100.0,
            chargingCycles = chargingCycles,
            timeCharging = (chargingTime * 100).roundToInt() / 100.0,
            timeDischarging = (dischargingTime * 100).roundToInt() / 100.0,
            healthStatus = healthStatus,
            powerConsumptionRate = (powerConsumption * 100).roundToInt() / 100.0,
            batteryDegradation = (degradation * 100).roundToInt() / 100.0,
            recommendations = recommendations
        )
    }
    
    fun exportToJson(report: BatteryReport, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(report))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun exportToCsv(batteryData: List<BatteryData>, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            
            FileWriter(file).use { writer ->
                writer.append("Timestamp,Level,Health,Temperature,Voltage,IsCharging,ChargingMethod\n")
                
                batteryData.forEach { data ->
                    writer.append("${data.timestamp},")
                    writer.append("${data.level},")
                    writer.append("${data.health},")
                    writer.append("${data.temperature},")
                    writer.append("${data.voltage},")
                    writer.append("${data.isCharging},")
                    writer.append("${data.chargingMethod}\n")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun countChargingCycles(sortedData: List<BatteryData>): Int {
        var cycles = 0
        var wasCharging = false
        
        sortedData.forEach { data ->
            if (!wasCharging && data.isCharging) {
                cycles++
            }
            wasCharging = data.isCharging
        }
        
        return cycles
    }
    
    private fun calculateChargingTime(sortedData: List<BatteryData>): Pair<Double, Double> {
        var chargingTime = 0.0
        var dischargingTime = 0.0
        
        for (i in 1 until sortedData.size) {
            val current = sortedData[i]
            val previous = sortedData[i - 1]
            val timeInterval = (current.timestamp - previous.timestamp) / 1000.0 / 60.0 // minutes
            
            if (current.isCharging) {
                chargingTime += timeInterval
            } else {
                dischargingTime += timeInterval
            }
        }
        
        return Pair(chargingTime, dischargingTime)
    }
    
    private fun calculatePowerConsumptionRate(sortedData: List<BatteryData>): Double {
        if (sortedData.size < 2) return 0.0
        
        val dischargingData = sortedData.filter { !it.isCharging }
        if (dischargingData.size < 2) return 0.0
        
        val first = dischargingData.first()
        val last = dischargingData.last()
        
        val levelDrop = first.level - last.level
        val timeHours = (last.timestamp - first.timestamp) / (1000.0 * 60.0 * 60.0)
        
        return if (timeHours > 0) levelDrop / timeHours else 0.0
    }
    
    private fun estimateBatteryDegradation(sortedData: List<BatteryData>): Double {
        val fullCharges = sortedData.filter { it.level >= 95 && it.isCharging }
        if (fullCharges.isEmpty()) return 0.0
        
        val averageFullChargeVoltage = fullCharges.map { it.voltage }.average()
        val expectedVoltage = 4200.0 // Typical Li-ion full charge voltage in mV
        
        return maxOf(0.0, (expectedVoltage - averageFullChargeVoltage) / expectedVoltage * 100)
    }
    
    private fun generateRecommendations(
        avgTemp: Double,
        maxTemp: Float,
        chargingTime: Double,
        dischargingTime: Double,
        degradation: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (maxTemp > 45) {
            recommendations.add("Battery temperature exceeded 45°C. Consider reducing device usage during charging.")
        }
        
        if (avgTemp > 35) {
            recommendations.add("Average temperature is high. Ensure proper ventilation during rides.")
        }
        
        if (chargingTime > dischargingTime * 2) {
            recommendations.add("Long charging times detected. Check charging cable and power source.")
        }
        
        if (degradation > 10) {
            recommendations.add("Battery degradation detected. Consider battery health check.")
        }
        
        if (degradation > 20) {
            recommendations.add("Significant battery degradation. Battery replacement may be needed.")
        }
        
        recommendations.add("For optimal battery life, avoid charging to 100% regularly.")
        recommendations.add("Try to keep battery level between 20-80% when possible.")
        
        return recommendations
    }
}