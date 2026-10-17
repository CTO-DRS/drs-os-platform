// SPDX-License-Identifier: Apache-2.0
// DRS OS - DRSAutomation screens (Arabic-first RTL).
//
// Screen-size fixes applied in this rebuild (user-reported defects from
// device screenshots, 2026-10-17):
//  1. Rule-editor top field was clipped under the header and unlabeled
//     -> every input is now a labeled OutlinedTextField inside a
//     vertically scrolling column with safe-drawing + IME insets.
//  2. Main screen showed an oversized teal pill element -> the create
//     button now has a fixed 52dp height; headers are compact.
//  3. Latin action ids wrapped mid-word ("set_them / e_light") -> all
//     chips render single-line without word breaks.
package os.drs.automation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.drs.automation.dayStartMillis
import os.drs.automation.engine.ActionRegistry
import os.drs.automation.engine.Decision
import os.drs.automation.engine.EvaluationContext
import os.drs.automation.engine.Rule
import os.drs.automation.engine.RuleEngine
import os.drs.automation.engine.Trigger
import os.drs.automation.engine.ValidationResult
import os.drs.automation.engine.validateRule
import os.drs.automation.readDeviceContext
import os.drs.automation.store.AuditRecord
import os.drs.automation.store.RuleStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal fun reasonArabic(reason: String): String = when (reason) {
    RuleEngine.REASON_RULE_DISABLED -> "القاعدة معطّلة"
    RuleEngine.REASON_TRIGGER_NOT_ACTIVE -> "المشغّل غير نشط الآن"
    RuleEngine.REASON_CONDITION_FAILED -> "شرط غير مستوفى"
    RuleEngine.REASON_REQUIRES_UNLOCK -> "الجهاز مقفل وتتطلب القاعدة فتح القفل"
    RuleEngine.REASON_MAX_RUNS -> "بلغت الحد اليومي للتشغيل"
    RuleEngine.REASON_FIRED -> "اشتعلت — صدر طلب إجراء"
    else -> reason
}

internal fun decisionReason(d: Decision): String = when (d) {
    is Decision.RuleDisabled -> RuleEngine.REASON_RULE_DISABLED
    is Decision.TriggerNotActive -> RuleEngine.REASON_TRIGGER_NOT_ACTIVE
    is Decision.ConditionFailed -> RuleEngine.REASON_CONDITION_FAILED
    is Decision.RequiresUnlock -> RuleEngine.REASON_REQUIRES_UNLOCK
    is Decision.MaxRunsReached -> RuleEngine.REASON_MAX_RUNS
    is Decision.Fired -> RuleEngine.REASON_FIRED
}

internal fun triggerSummaryArabic(t: Trigger): String = when (t) {
    is Trigger.BatteryBelow -> "البطارية تحت ${t.threshold}٪"
    is Trigger.PowerConnected -> if (t.state) "عند توصيل الشاحن" else "عند فصل الشاحن"
    is Trigger.TimeDaily ->
        String.format(Locale.US, "%02d:%02d", t.timeMinutes / 60, t.timeMinutes % 60) +
            " (يتطلب جدولة النظام)"
}

internal fun minutesArabic(m: Int): String =
    String.format(Locale.US, "%02d:%02d", m / 60, m % 60)

internal fun validationArabic(reason: String): String = when (reason) {
    "id-blank" -> "خطأ داخلي: معرّف القاعدة فارغ."
    "name-blank" -> "أدخل اسمًا للقاعدة."
    "explanation-blank" -> "أدخل شرحًا مرفقًا للمستخدم."
    "action-not-in-registry" -> "الإجراء غير موجود في السجل المغلق."
    "trigger-threshold-range" -> "حد البطارية يجب أن يكون بين 1 و 99."
    "trigger-time-range" -> "الوقت غير صالح."
    "condition-window-range" -> "نافذة الوقت غير صالحة."
    "condition-weekday-range" -> "يوم الأسبوع غير صالح."
    "max-runs-below-1" -> "أقصى عدد تشغيلات يجب أن يكون 1 على الأقل."
    else -> "قيم غير صالحة: $reason"
}

internal sealed interface Screen {
    data object RulesList : Screen
    data object Editor : Screen
    data object Audit : Screen
}

@Composable
fun AutomationApp() {
    val context = LocalContext.current
    val store = remember { RuleStore(context) }
    var screen by remember { mutableStateOf<Screen>(Screen.RulesList) }
    var editingRuleId by remember { mutableStateOf<String?>(null) }

    when (screen) {
        Screen.RulesList -> MainScreen(
            store = store,
            onCreate = {
                editingRuleId = null
                screen = Screen.Editor
            },
            onEdit = { id ->
                editingRuleId = id
                screen = Screen.Editor
            },
            onAudit = { screen = Screen.Audit },
        )
        Screen.Editor -> EditorScreen(
            store = store,
            editing = store.rules.firstOrNull { it.id == editingRuleId },
            onDone = { screen = Screen.RulesList },
        )
        Screen.Audit -> AuditScreen(
            store = store,
            onBack = { screen = Screen.RulesList },
        )
    }
}

@Composable
internal fun MainScreen(
    store: RuleStore,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onAudit: () -> Unit,
) {
    var evaluateSummary by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Compact fixed header (screen-size fix: no oversized elements).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        ) {
            Text(
                "أتمتة DRS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "قواعد محلية حتمية — بدون ذكاء اصطناعي وبدون شبكة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Fixed-height create button (screen-size fix: 52dp, never grows).
        Button(
            onClick = onCreate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("قاعدة جديدة", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(8.dp))

        if (store.rules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("لا توجد قواعد بعد", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "أنشئ قاعدة من الزر أعلاه.\nالتقييم حتمي بالكامل وبدون أي ذكاء اصطناعي.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                items(store.rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onEdit = { onEdit(rule.id) },
                        onToggle = { enabled -> store.setEnabled(rule.id, enabled) },
                        onDelete = { store.delete(rule.id) },
                    )
                }
            }
        }

        evaluateSummary?.let { summary ->
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val dc = readDeviceContext(context)
                    var fired = 0
                    val nowMs = System.currentTimeMillis()
                    store.rules.forEach { rule ->
                        val runs = store.firedSince(rule.id, dayStartMillis(nowMs))
                        val ctx = EvaluationContext(
                            nowMinutes = dc.nowMinutes,
                            weekday = dc.weekday,
                            battery = dc.battery,
                            charging = dc.charging,
                            deviceUnlocked = dc.deviceUnlocked,
                            runsToday = runs,
                        )
                        val decision = RuleEngine.evaluate(rule, ctx)
                        if (decision is Decision.Fired) fired++
                        store.log(
                            AuditRecord(
                                ruleId = rule.id,
                                ruleName = rule.name,
                                reason = decisionReason(decision),
                                actionId = (decision as? Decision.Fired)?.actionId,
                                explanation = rule.explanation,
                                timeMinutes = dc.nowMinutes,
                                weekday = dc.weekday,
                                battery = dc.battery,
                                charging = dc.charging,
                                unlocked = dc.deviceUnlocked,
                                timestampMs = nowMs,
                            ),
                        )
                    }
                    evaluateSummary = if (store.rules.isEmpty()) {
                        "لا توجد قواعد للتقييم."
                    } else {
                        "نتيجة التقييم: اشتعلت $fired من ${store.rules.size}. التفاصيل في سجل التدقيق. تنفيذ الإجراءات ينتظر تكامل النظام (ألفا)."
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                enabled = store.rules.isNotEmpty(),
            ) {
                Text("تقييم القواعد الآن")
            }
            OutlinedButton(
                onClick = onAudit,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Text("سجل التدقيق")
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onEdit() },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        triggerSummaryArabic(rule.trigger),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        rule.actionId,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        // Screen-size fix: single line, never breaks mid-word.
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (rule.enabled) "مفعّلة" else "معطّلة",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (rule.enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Text(
                        "حذف",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                rule.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditorScreen(
    store: RuleStore,
    editing: Rule?,
    onDone: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var explanation by rememberSaveable { mutableStateOf(editing?.explanation ?: "") }
    var triggerBattery by rememberSaveable {
        mutableStateOf(editing?.trigger !is Trigger.PowerConnected)
    }
    var threshold by rememberSaveable {
        mutableStateOf((editing?.trigger as? Trigger.BatteryBelow)?.threshold?.toString() ?: "20")
    }
    var powerOn by rememberSaveable {
        mutableStateOf((editing?.trigger as? Trigger.PowerConnected)?.state ?: true)
    }
    var actionId by rememberSaveable { mutableStateOf(editing?.actionId ?: "set_dnd_on") }
    var maxRuns by rememberSaveable {
        mutableStateOf(editing?.maxRunsPerDay?.toString() ?: "10")
    }
    var requiresUnlock by rememberSaveable { mutableStateOf(editing?.requiresUnlock ?: true) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDone) { Text("رجوع") }
            Text(
                if (editing == null) "قاعدة جديدة" else "تعديل القاعدة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        // Screen-size fix: labeled field inside a scrollable column —
        // the previous build's top input was clipped and unlabeled.
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("اسم القاعدة") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        OutlinedTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = { Text("شرح مرفق للمستخدم (يظهر في الواجهة وسجل التدقيق)") },
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )

        SectionTitle("المشغّل")
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectableChip("البطارية تحت حد", selected = triggerBattery) {
                triggerBattery = true
            }
            SelectableChip("حالة الشحن", selected = !triggerBattery) {
                triggerBattery = false
            }
        }

        if (triggerBattery) {
            OutlinedTextField(
                value = threshold,
                onValueChange = { v ->
                    if (v.isEmpty() || (v.all { it.isDigit() } && v.length <= 3)) threshold = v
                },
                label = { Text("حد البطارية (1-99)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SelectableChip("متصل", selected = powerOn) { powerOn = true }
                SelectableChip("غير متصل", selected = !powerOn) { powerOn = false }
            }
        }

        SectionTitle("الإجراء (من السجل المغلق)")
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionRegistry.ACTIONS.sorted().forEach { id ->
                // Screen-size fix: chips are single-line, no mid-word breaks.
                SelectableChip(id, selected = actionId == id) { actionId = id }
            }
        }

        OutlinedTextField(
            value = maxRuns,
            onValueChange = { v ->
                if (v.isEmpty() || (v.all { it.isDigit() } && v.length <= 3)) maxRuns = v
            },
            label = { Text("أقصى عدد تشغيلات في اليوم") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = requiresUnlock,
                onCheckedChange = { requiresUnlock = it },
            )
            Text("يتطلب فتح قفل الجهاز قبل التنفيذ", style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            "ملاحظة ألفا: المشغّل الزمني اليومي يتطلب جدولة على مستوى النظام ولم يُفعَّل بعد؛ لذلك لا يمكن إنشاؤه في هذه النسخة. تنفيذ الإجراءات يُسجَّل كطلبات في سجل التدقيق دون تنفيذ فعلي.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )

        errorMsg?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Text("إلغاء")
            }
            Button(
                onClick = {
                    val trigger = if (triggerBattery) {
                        Trigger.BatteryBelow(threshold.toIntOrNull() ?: Int.MIN_VALUE)
                    } else {
                        Trigger.PowerConnected(powerOn)
                    }
                    val rule = Rule(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        enabled = editing?.enabled ?: true,
                        trigger = trigger,
                        conditions = emptyList(),
                        actionId = actionId,
                        requiresUnlock = requiresUnlock,
                        maxRunsPerDay = maxRuns.toIntOrNull() ?: 0,
                        explanation = explanation.trim(),
                    )
                    when (val v = validateRule(rule)) {
                        is ValidationResult.Ok -> {
                            store.upsert(rule)
                            onDone()
                        }
                        is ValidationResult.Invalid -> errorMsg = validationArabic(v.reason)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("حفظ", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun AuditScreen(
    store: RuleStore,
    onBack: () -> Unit,
) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("رجوع") }
            Text(
                "سجل التدقيق",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            if (store.audit.isNotEmpty()) {
                TextButton(onClick = { store.clearAudit() }) { Text("مسح السجل") }
            }
        }
        Text(
            "تسجيل محلي فقط داخل الجهاز — لا يُرسل أي شيء خارج الجهاز.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        if (store.audit.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("السجل فارغ", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "استخدم زر «تقييم القواعد الآن» لتوليد سجلات قرار حتمية.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                items(store.audit.asReversed()) { a ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                "${a.ruleName} — ${reasonArabic(a.reason)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "الإجراء: ${a.actionId ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "السياق: الساعة ${minutesArabic(a.timeMinutes)}، " +
                                    "بطارية ${a.battery}٪، " +
                                    if (a.charging) "الشاحن متصل، " else "الشاحن منفصل، " +
                                    if (a.unlocked) "الجهاز مفتوح" else "الجهاز مقفل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                fmt.format(Date(a.timestampMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** Selectable chip with a single-line label — never wraps mid-word. */
@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            Color.Transparent
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
    ) {
        Text(
            label,
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            softWrap = false,
            maxLines = 1,
        )
    }
}
