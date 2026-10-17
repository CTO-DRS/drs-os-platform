// SPDX-License-Identifier: Apache-2.0
// DRS OS - DRSSecurityCenter screen (Arabic-first RTL).
// Honesty rules enforced in code: every status below is read from a
// permission-free platform API and labeled with its exact value; status
// is never color-only; nothing is invented.
package os.drs.security.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class CheckResult(val title: String, val status: String, val detail: String)

@Composable
fun SecurityScreen() {
    val context = LocalContext.current
    val checks = remember { runChecks(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        ) {
            Text(
                "مركز أمان DRS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "يعرض حالة المنصة كما هي — بلا أحكام مُخترعة وبلا أي صلاحيات",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "فحوصات الجهاز (قراءة فقط، بلا صلاحيات)") {
            checks.forEach { c ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        c.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        c.status,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        c.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SectionCard(title = "سياسات هذا التطبيق") {
            PolicyRow("صلاحيات أندرويد", "صفر — لا يطلب التطبيق أي صلاحية")
            PolicyRow("الشبكة", "لا يوجد أي كود شبكي في التطبيق")
            PolicyRow("الذكاء الاصطناعي/السحابة", "لا يوجد — عرض محلي فقط")
            PolicyRow("الأحكام الأمنية", "لا أحكام ولا نقاط — قيم مقروءة من المنصة فقط")
        }

        SectionCard(title = "حدود نسخة ألفا") {
            Text(
                "تكامل SafetyCenter وإدارة الصلاحيات وطبقة الخصوصية الموسعة تأتي مع دمج المنصة في مرحلة لاحقة وفق مواصفة الأسطح (المساحة 9). هذه النسخة عرض حالة فقط ولا تعدّل أي إعداد أمني.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun runChecks(context: Context): List<CheckResult> {
    val checks = mutableListOf<CheckResult>()

    // 1. Screen lock security — KeyguardManager (no permission needed).
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    val secure = km?.isDeviceSecure == true
    checks += CheckResult(
        title = "قفل الشاشة الآمن (PIN/نمط/بصمة على الأقل)",
        status = if (secure) "مُفعّل" else "غير مُفعّل",
        detail = "المصدر: KeyguardManager.isDeviceSecure — بدون أي صلاحية.",
    )

    // 2. Security patch level of the platform.
    checks += CheckResult(
        title = "مستوى ترقيع الأمان لنظام التشغيل",
        status = Build.VERSION.SECURITY_PATCH?.takeIf { it.isNotBlank() } ?: "غير متاح",
        detail = "المصدر: Build.VERSION.SECURITY_PATCH — قيمة المنصة كما هي.",
    )

    // 3. This build's signing/debug nature — honest self-report.
    val debuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    checks += CheckResult(
        title = "طبيعة هذه النسخة",
        status = if (debuggable) "نسخة تصحيح (debug) موقّعة بمفتاح تصحيح" else "نسخة غير قابلة للتنقيح",
        detail = "للتقييم فقط: لا تُستخدم كنسخة إنتاج. المصدر: ApplicationInfo.FLAG_DEBUGGABLE.",
    )

    // 4. Installation source (API 30+ API; guarded for API 29).
    val installer: String? = if (Build.VERSION.SDK_INT >= 30) {
        runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull()
    } else {
        @Suppress("DEPRECATION")
        runCatching {
            context.packageManager.getInstallerPackageName(context.packageName)
        }.getOrNull()
    }
    checks += CheckResult(
        title = "مصدر تثبيت هذا التطبيق",
        status = installer ?: "غير معروف (تثبيت مباشر بـ adb غالبًا)",
        detail = "المصدر: PackageManager install-source APIs — بدون أي صلاحية.",
    )

    // 5. Android version for context.
    checks += CheckResult(
        title = "إصدار أندرويد",
        status = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
        detail = "المصدر: Build.VERSION — قيم المنصة كما هي.",
    )

    return checks
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PolicyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
