// SPDX-License-Identifier: Apache-2.0
// DRS OS - DRSAutomation entry point and zero-permission device context.
package os.drs.automation

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import os.drs.automation.ui.AutomationApp
import os.drs.automation.ui.DrsTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrsTheme {
                AutomationApp()
            }
        }
    }
}

/**
 * Device snapshot used as the evaluation context. Every input here is
 * read WITHOUT any Android permission: battery state comes from the
 * sticky ACTION_BATTERY_CHANGED broadcast, lock state from
 * KeyguardManager (both are permission-free public APIs).
 */
data class DeviceContext(
    val nowMinutes: Int,
    val weekday: Int,
    val battery: Int,
    val charging: Boolean,
    val deviceUnlocked: Boolean,
)

fun readDeviceContext(context: Context): DeviceContext {
    val cal = Calendar.getInstance()
    val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    // Calendar.DAY_OF_WEEK: 1=Sunday..7=Saturday -> ISO weekday 0=Monday..6=Sunday
    val weekday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val battery = if (level >= 0 && scale > 0) (level * 100) / scale else 0
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    val unlocked = !(km?.isKeyguardLocked ?: false)

    return DeviceContext(nowMinutes, weekday, battery, charging, unlocked)
}

/** Start-of-day timestamp (local time) for per-day run counting. */
fun dayStartMillis(nowMs: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = nowMs
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
