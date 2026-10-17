// SPDX-License-Identifier: Apache-2.0
// DRS OS - deterministic rule engine (Kotlin product implementation).
// Semantics are pinned by docs/automation/rule-engine-spec.md and the
// Python reference model in tools/automation-reference/.
//
// Non-negotiable properties (spec section 1):
//  - No AI/ML: pure deterministic evaluation of explicit rules.
//  - Closed action vocabulary: unknown action ids are rejected at
//    validation time by validateRule().
//  - The engine executes nothing: evaluate() returns a Decision; a host
//    layer decides execution. This alpha build records audit entries and
//    never executes system actions.
package os.drs.automation.engine

/** Closed action registry (spec section 2, v1). */
object ActionRegistry {
    val ACTIONS: Set<String> = setOf(
        "set_dnd_on",
        "set_dnd_off",
        "set_theme_dark",
        "set_theme_light",
        "enable_battery_saver",
        "disable_battery_saver",
    )

    /** All v1 actions are reversible and none are security-sensitive. */
    val SECURITY_SENSITIVE: Set<String> = emptySet()

    fun isValid(actionId: String): Boolean = actionId in ACTIONS
}

/** Trigger kinds (spec section 2). */
sealed class Trigger {
    /** Edge-triggered at a fixed minute of day, 0..1439. */
    data class TimeDaily(val timeMinutes: Int) : Trigger()

    /** Level-triggered: true while battery level < threshold (1..99). */
    data class BatteryBelow(val threshold: Int) : Trigger()

    /** Level-triggered on charging-state match. */
    data class PowerConnected(val state: Boolean) : Trigger()
}

/** Condition kinds (spec section 2). All listed conditions must hold. */
sealed class Condition {
    /**
     * True when now is in [startMinutes, endMinutes).
     * May span midnight: start > end is legal (spec-mandated).
     */
    data class TimeWindow(val startMinutes: Int, val endMinutes: Int) : Condition()

    /** ISO weekday subset: 0=Monday .. 6=Sunday. */
    data class DayOfWeek(val days: Set<Int>) : Condition()
}

/** Immutable evaluation context (spec section 3). */
data class EvaluationContext(
    val nowMinutes: Int,
    val weekday: Int,
    val battery: Int,
    val charging: Boolean,
    val deviceUnlocked: Boolean,
    val runsToday: Int,
)

/** Rule record (spec section 2 schema). */
data class Rule(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val trigger: Trigger,
    val conditions: List<Condition> = emptyList(),
    val actionId: String,
    val requiresUnlock: Boolean = true,
    val maxRunsPerDay: Int = 10,
    val explanation: String,
)

/** Decision outcomes in the fixed priority order of spec section 3. */
sealed class Decision {
    object RuleDisabled : Decision()
    object TriggerNotActive : Decision()
    object ConditionFailed : Decision()
    object RequiresUnlock : Decision()
    object MaxRunsReached : Decision()
    data class Fired(val actionId: String) : Decision()
}

object RuleEngine {
    const val REASON_RULE_DISABLED = "rule-disabled"
    const val REASON_TRIGGER_NOT_ACTIVE = "trigger-not-active"
    const val REASON_CONDITION_FAILED = "condition-failed"
    const val REASON_REQUIRES_UNLOCK = "requires-unlock"
    const val REASON_MAX_RUNS = "max-runs-reached"
    const val REASON_FIRED = "fired"

    /**
     * Pure deterministic evaluation. Same inputs always produce the same
     * Decision (spec section 3). Decision order is fixed; the first
     * failing reason wins and is recorded by the caller.
     */
    fun evaluate(rule: Rule, ctx: EvaluationContext): Decision {
        if (!rule.enabled) return Decision.RuleDisabled

        val triggerActive = when (val t = rule.trigger) {
            is Trigger.TimeDaily -> ctx.nowMinutes == t.timeMinutes
            is Trigger.BatteryBelow -> ctx.battery < t.threshold
            is Trigger.PowerConnected -> ctx.charging == t.state
        }
        if (!triggerActive) return Decision.TriggerNotActive

        for (c in rule.conditions) {
            val holds = when (c) {
                is Condition.TimeWindow -> inWindow(ctx.nowMinutes, c.startMinutes, c.endMinutes)
                is Condition.DayOfWeek -> ctx.weekday in c.days
            }
            if (!holds) return Decision.ConditionFailed
        }

        if (rule.requiresUnlock && !ctx.deviceUnlocked) return Decision.RequiresUnlock
        if (ctx.runsToday >= rule.maxRunsPerDay) return Decision.MaxRunsReached

        return Decision.Fired(rule.actionId)
    }

    /**
     * Half-open window [start, end). start > end spans midnight.
     * start == end is an empty window (never true).
     */
    fun inWindow(now: Int, start: Int, end: Int): Boolean =
        if (start <= end) now in start until end
        else now >= start || now < end

    fun isValidWeekdayDay(day: Int): Boolean = day in 0..6
}

sealed class ValidationResult {
    object Ok : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

/**
 * Structural validation at rule creation time. Unknown actions and
 * out-of-range parameters fail validation outright (spec sections 1, 2).
 */
fun validateRule(r: Rule): ValidationResult {
    if (r.id.isBlank()) return ValidationResult.Invalid("id-blank")
    if (r.name.isBlank()) return ValidationResult.Invalid("name-blank")
    if (r.explanation.isBlank()) return ValidationResult.Invalid("explanation-blank")
    if (!ActionRegistry.isValid(r.actionId)) return ValidationResult.Invalid("action-not-in-registry")
    when (val t = r.trigger) {
        is Trigger.TimeDaily ->
            if (t.timeMinutes !in 0..1439) return ValidationResult.Invalid("trigger-time-range")
        is Trigger.BatteryBelow ->
            if (t.threshold !in 1..99) return ValidationResult.Invalid("trigger-threshold-range")
        is Trigger.PowerConnected -> { /* any boolean state is valid */ }
    }
    for (c in r.conditions) {
        when (c) {
            is Condition.TimeWindow ->
                if (c.startMinutes !in 0..1439 || c.endMinutes !in 0..1439) {
                    return ValidationResult.Invalid("condition-window-range")
                }
            is Condition.DayOfWeek ->
                if (c.days.any { !RuleEngine.isValidWeekdayDay(it) }) {
                    return ValidationResult.Invalid("condition-weekday-range")
                }
        }
    }
    if (r.maxRunsPerDay < 1) return ValidationResult.Invalid("max-runs-below-1")
    return ValidationResult.Ok
}
