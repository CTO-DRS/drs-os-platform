// SPDX-License-Identifier: Apache-2.0
// DRS OS - DRSSettings screen (Arabic-first RTL).
// Honest alpha scope: read-only information surfaces. No system settings
// are modified (platform integration arrives in a later phase), no fake
// toggles, no claims of functionality that does not exist.
package os.drs.settings.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        ) {
            Text(
                "إعدادات DRS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "نظام DRS — خصوصية أولًا، عربية أولًا، بدون ذكاء اصطناعي",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "عن هذه النسخة") {
            InfoRow("الإصدار", "0.3.1-phase3 (ألفا)")
            InfoRow("النطاق", "تطبيقات مستقلة قبل تكامل المنصة")
            InfoRow("الأساس المخطط", "AOSP — android-latest-release")
            Text(
                "هذه نسخة تقييمية مبكرة: تُظهر واجهات ومكونات النظام المخطط لها قبل دمجها في صورة النظام. بعض الإعدادات عرضية فقط في هذه المرحلة، وكل شاشة تصف حدودها بصدق.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SectionCard(title = "معلومات الجهاز (قراءة فقط)") {
            InfoRow("الطراز", Build.MODEL ?: "—")
            InfoRow("الجهاز", Build.DEVICE ?: "—")
            InfoRow("إصدار أندرويد", "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            InfoRow("ترقيع الأمان", Build.VERSION.SECURITY_PATCH ?: "—")
        }

        SectionCard(title = "المظهر") {
            InfoRow("الوضع", "يتبع إعداد النظام (داكن/فاتح)")
            InfoRow("الاتجاه", "من اليمين إلى اليسار افتراضيًا (عربية أولًا)")
            Text(
                "تبديل المظهر والتخصيص الكامل يأتي مع تكامل طبقة المنصة (المرحلة القادمة) عبر طبقات RRO وUiModeManager وفق مواصفة الأسطح الاثني عشر.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SectionCard(title = "الخصوصية والأمان") {
            InfoRow("صلاحيات أندرويد", "صفر — لا يطلب هذا التطبيق أي صلاحية")
            InfoRow("الشبكة", "لا يوجد أي كود شبكي")
            InfoRow("الذكاء الاصطناعي", "لا يوجد — كل السلوك حتمي ومعلن")
            InfoRow("السحابة", "لا يوجد — كل البيانات محلية")
        }

        SectionCard(title = "اللغة") {
            InfoRow("الافتراضية", "العربية (RTL)")
            InfoRow("البديلة", "الإنجليزية حسب لغة النظام")
        }

        Spacer(Modifier.height(24.dp))
    }
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
private fun InfoRow(label: String, value: String) {
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
