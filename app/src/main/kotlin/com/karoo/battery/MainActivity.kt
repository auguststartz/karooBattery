package com.karoo.battery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var batteryReporter: BatteryReporter
    private val batteryDataList = mutableListOf<BatteryData>()
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val MONITORING_INTERVAL = 10000L // 10 seconds
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        batteryMonitor = BatteryMonitor(this)
        batteryReporter = BatteryReporter()
        
        requestPermissions()
        startBatteryMonitoring()
        setupPeriodicReporting()
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun startBatteryMonitoring() {
        batteryMonitor.startMonitoring(MONITORING_INTERVAL) { batteryData ->
            batteryDataList.add(batteryData)
            
            // Log current battery status
            println("Battery Status:")
            println("  Level: ${batteryData.level}%")
            println("  Health: ${batteryData.health}")
            println("  Temperature: ${batteryData.temperature}°C")
            println("  Voltage: ${batteryData.voltage}mV")
            println("  Charging: ${batteryData.isCharging}")
            println("  Method: ${batteryData.chargingMethod}")
            println("  ---")
            
            // Show toast for critical conditions
            when {
                batteryData.temperature > 45 -> {
                    showToast("⚠️ Battery temperature high: ${batteryData.temperature}°C")
                }
                batteryData.level <= 10 && !batteryData.isCharging -> {
                    showToast("🔋 Low battery: ${batteryData.level}%")
                }
                batteryData.health != BatteryHealth.GOOD -> {
                    showToast("⚠️ Battery health: ${batteryData.health}")
                }
            }
        }
        
        showToast("🔋 Battery monitoring started")
    }
    
    private fun setupPeriodicReporting() {
        lifecycleScope.launch {
            while (true) {
                delay(300000) // 5 minutes
                
                if (batteryDataList.size >= 10) {
                    generateAndSaveReport()
                }
            }
        }
    }
    
    private fun generateAndSaveReport() {
        try {
            val report = batteryReporter.generateReport(batteryDataList)
            
            val documentsDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "BatteryReports")
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val jsonFile = File(documentsDir, "battery_report_$timestamp.json")
            val csvFile = File(documentsDir, "battery_data_$timestamp.csv")
            
            val jsonSuccess = batteryReporter.exportToJson(report, jsonFile.absolutePath)
            val csvSuccess = batteryReporter.exportToCsv(batteryDataList, csvFile.absolutePath)
            
            if (jsonSuccess && csvSuccess) {
                showToast("📊 Report saved to: ${documentsDir.absolutePath}")
                
                // Print report summary
                println("=== BATTERY REPORT SUMMARY ===")
                println("Monitoring Duration: ${report.monitoringDurationHours} hours")
                println("Average Battery Level: ${report.averageBatteryLevel}%")
                println("Temperature Range: ${report.minTemperature}°C - ${report.maxTemperature}°C")
                println("Charging Cycles: ${report.chargingCycles}")
                println("Power Consumption Rate: ${report.powerConsumptionRate}%/hour")
                println("Battery Degradation: ${report.batteryDegradation}%")
                println("Health Status: ${report.healthStatus}")
                println("\nRecommendations:")
                report.recommendations.forEach { println("  • $it") }
                println("==============================")
                
            } else {
                showToast("❌ Failed to save report")
            }
            
        } catch (e: Exception) {
            showToast("❌ Error generating report: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        batteryMonitor.stopMonitoring()
        
        // Generate final report
        if (batteryDataList.isNotEmpty()) {
            generateAndSaveReport()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    showToast("✅ Permissions granted")
                } else {
                    showToast("❌ Permissions required for file export")
                }
            }
        }
    }
}