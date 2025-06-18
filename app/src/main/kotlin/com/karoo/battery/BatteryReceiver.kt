package com.karoo.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BatteryReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BATTERY_LOW -> {
                context?.let {
                    Toast.makeText(it, "🔋 Battery Low Warning!", Toast.LENGTH_LONG).show()
                }
            }
            Intent.ACTION_BATTERY_OKAY -> {
                context?.let {
                    Toast.makeText(it, "✅ Battery Level OK", Toast.LENGTH_SHORT).show()
                }
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                // Battery status changed - could trigger additional monitoring if needed
            }
        }
    }
}