// SPDX-License-Identifier: Apache-2.0
// DRS OS - rule and audit persistence for DRSAutomation.
// Local-only storage in the app-private files directory. There is no
// network write path (data-flow.md F6). Audit is append-only and capped
// at 500 records to bound storage. JSON via the platform org.json.
package os.drs.automation.store

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import os.drs.automation.engine.Rule
import os.drs.automation.engine.Trigger
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Audit record with the mandatory fields of spec section 5. */
data class AuditRecord(
    val ruleId: String,
    val ruleName: String,
    val reason: String,
    val actionId: String?,
    val explanation: String,
    val timeMinutes: Int,
    val weekday: Int,
    val battery: Int,
    val charging: Boolean,
    val unlocked: Boolean,
    val timestampMs: Long,
)

/**
 * Observable store backed by JSON files. State is exposed through
 * Compose mutableStateOf so UI recomposes on every mutation.
 */
class RuleStore(context: Context) {
    private val rulesFile = File(context.filesDir, "rules.json")
    private val auditFile = File(context.filesDir, "audit.json")

    var rules by mutableStateOf<List<Rule>>(emptyList())
        private set
    var audit by mutableStateOf<List<AuditRecord>>(emptyList())
        private set

    init {
        rules = loadRules()
        audit = loadAudit()
    }

    fun upsert(rule: Rule) {
        rules = rules.filterNot { it.id == rule.id } + rule
        persistRules()
    }

    fun setEnabled(id: String, enabled: Boolean) {
        rules = rules.map { if (it.id == id) it.copy(enabled = enabled) else it }
        persistRules()
    }

    fun delete(id: String) {
        rules = rules.filterNot { it.id == id }
        persistRules()
    }

    fun log(record: AuditRecord) {
        audit = (audit + record).takeLast(MAX_AUDIT_RECORDS)
        persistAudit()
    }

    fun clearAudit() {
        audit = emptyList()
        persistAudit()
    }

    /** Count of fired decisions for [ruleId] since [dayStartMs]. */
    fun firedSince(ruleId: String, dayStartMs: Long): Int =
        audit.count {
            it.ruleId == ruleId && it.reason == "fired" && it.timestampMs >= dayStartMs
        }

    private fun loadRules(): List<Rule> = runCatching {
        val arr = JSONArray(rulesFile.takeIf { it.exists() }?.readText() ?: "[]")
        (0 until arr.length()).map { i -> ruleFromJson(arr.getJSONObject(i)) }
    }.getOrDefault(emptyList())

    private fun loadAudit(): List<AuditRecord> = runCatching {
        val arr = JSONArray(auditFile.takeIf { it.exists() }?.readText() ?: "[]")
        (0 until arr.length()).map { i -> auditFromJson(arr.getJSONObject(i)) }
    }.getOrDefault(emptyList())

    private fun persistRules() {
        runCatching {
            val arr = JSONArray()
            rules.forEach { r -> arr.put(ruleToJson(r)) }
            rulesFile.writeText(arr.toString())
        }
    }

    private fun persistAudit() {
        runCatching {
            val arr = JSONArray()
            audit.forEach { a -> arr.put(auditToJson(a)) }
            auditFile.writeText(arr.toString())
        }
    }

    private fun ruleToJson(r: Rule) = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("enabled", r.enabled)
        when (val t = r.trigger) {
            is Trigger.TimeDaily -> {
                put("triggerKind", "TIME_DAILY")
                put("timeMinutes", t.timeMinutes)
            }
            is Trigger.BatteryBelow -> {
                put("triggerKind", "BATTERY_BELOW")
                put("threshold", t.threshold)
            }
            is Trigger.PowerConnected -> {
                put("triggerKind", "POWER_CONNECTED")
                put("state", t.state)
            }
        }
        put("actionId", r.actionId)
        put("requiresUnlock", r.requiresUnlock)
        put("maxRunsPerDay", r.maxRunsPerDay)
        put("explanation", r.explanation)
    }

    private fun ruleFromJson(o: JSONObject): Rule {
        val trigger = when (o.getString("triggerKind")) {
            "TIME_DAILY" -> Trigger.TimeDaily(o.getInt("timeMinutes"))
            "POWER_CONNECTED" -> Trigger.PowerConnected(o.getBoolean("state"))
            else -> Trigger.BatteryBelow(o.getInt("threshold"))
        }
        return Rule(
            id = o.getString("id"),
            name = o.getString("name"),
            enabled = o.optBoolean("enabled", true),
            trigger = trigger,
            conditions = emptyList(),
            actionId = o.getString("actionId"),
            requiresUnlock = o.optBoolean("requiresUnlock", true),
            maxRunsPerDay = o.optInt("maxRunsPerDay", 10),
            explanation = o.optString("explanation", ""),
        )
    }

    private fun auditToJson(a: AuditRecord) = JSONObject().apply {
        put("ruleId", a.ruleId)
        put("ruleName", a.ruleName)
        put("reason", a.reason)
        put("actionId", a.actionId ?: JSONObject.NULL)
        put("explanation", a.explanation)
        put("timeMinutes", a.timeMinutes)
        put("weekday", a.weekday)
        put("battery", a.battery)
        put("charging", a.charging)
        put("unlocked", a.unlocked)
        put("timestampMs", a.timestampMs)
    }

    private fun auditFromJson(o: JSONObject): AuditRecord {
        val actionId = if (o.has("actionId") && !o.isNull("actionId")) o.getString("actionId") else null
        return AuditRecord(
            ruleId = o.getString("ruleId"),
            ruleName = o.optString("ruleName", ""),
            reason = o.getString("reason"),
            actionId = actionId,
            explanation = o.optString("explanation", ""),
            timeMinutes = o.optInt("timeMinutes", 0),
            weekday = o.optInt("weekday", 0),
            battery = o.optInt("battery", 0),
            charging = o.optBoolean("charging", false),
            unlocked = o.optBoolean("unlocked", true),
            timestampMs = o.optLong("timestampMs", 0L),
        )
    }

    companion object {
        const val MAX_AUDIT_RECORDS = 500
    }
}
