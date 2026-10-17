// SPDX-License-Identifier: Apache-2.0
// DRS OS - DRSSecurityCenter entry point.
package os.drs.security

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import os.drs.security.ui.DrsTheme
import os.drs.security.ui.SecurityScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrsTheme {
                SecurityScreen()
            }
        }
    }
}
