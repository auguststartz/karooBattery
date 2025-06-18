package com.karoo.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

class BatteryMonitor(private val context: Context) {
    
    private val batteryManager: BatteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<(BatteryData) -> Unit>()
    private val batteryHistory = mutableListOf<BatteryData>()
    private var isMonitoring = false
    private var monitoringRunnable: Runnable? = null
    
    fun startMonitoring(intervalMs: Long = 5000, callback: (BatteryData) -> Unit) {
        if (isMonitoring) return
        
        listeners.add(callback)
        isMonitoring = true
        
        monitoringRunnable = object : Runnable {
            override fun run() {
                val batteryData = getCurrentBatteryData()
                batteryHistory.add(batteryData)
                
                listeners.forEach { it(batteryData) }
                
                if (isMonitoring) {
                    handler.postDelayed(this, intervalMs)
                }
            }
        }
        
        handler.post(monitoringRunnable!!)
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        listeners.clear()
    }
    
    fun getCurrentBatteryData(): BatteryData {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
        
        val health = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UNSPECIFIED_FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            else -> BatteryHealth.UNKNOWN
        }
        
        val temperature = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                        status == BatteryManager.BATTERY_STATUS_FULL
        
        val chargingMethod = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingMethod.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingMethod.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingMethod.WIRELESS
            else -> ChargingMethod.NONE
        }
        
        return BatteryData(
            level = batteryPct,
            health = health,
            temperature = temperature,
            voltage = voltage,
            isCharging = isCharging,
            chargingMethod = chargingMethod
        )
    }
    
    fun getBatteryHistory(): List<BatteryData> = batteryHistory.toList()
    
    fun clearHistory() {
        batteryHistory.clear()
    }
    
    fun getBatteryCapacity(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    fun getEnergyCounter(): Long {
        return batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
    }
    
    fun getCurrentAverage(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
    }
    
    fun getCurrentNow(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    }
}