// SPDX-License-Identifier: Apache-2.0
// DRS OS - DRSSettings entry point.
package os.drs.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import os.drs.settings.ui.DrsTheme
import os.drs.settings.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrsTheme {
                SettingsScreen()
            }
        }
    }
}
