// SPDX-License-Identifier: Apache-2.0
// DRS OS - deterministic rule engine unit tests (spec section 3 + 6).
package os.drs.automation.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private fun ctx(
        now: Int = 600,
        weekday: Int = 0,
        battery: Int = 100,
        charging: Boolean = false,
        unlocked: Boolean = true,
        runs: Int = 0,
    ) = EvaluationContext(now, weekday, battery, charging, unlocked, runs)

    private fun batteryRule(
        threshold: Int = 20,
        enabled: Boolean = true,
        maxRuns: Int = 10,
        requiresUnlock: Boolean = false,
    ) = Rule(
        id = "r1",
        name = "قاعدة اختبار",
        enabled = enabled,
        trigger = Trigger.BatteryBelow(threshold),
        conditions = emptyList(),
        actionId = "set_dnd_on",
        requiresUnlock = requiresUnlock,
        maxRunsPerDay = maxRuns,
        explanation = "شرح اختبار",
    )

    @Test
    fun disabledRuleNeverFires() {
        assertEquals(
            Decision.RuleDisabled,
            RuleEngine.evaluate(batteryRule(enabled = false), ctx(battery = 10)),
        )
    }

    @Test
    fun batteryBelowThresholdFires() {
        assertEquals(
            Decision.Fired("set_dnd_on"),
            RuleEngine.evaluate(batteryRule(20), ctx(battery = 19)),
        )
    }

    @Test
    fun batteryEqualThresholdIsNotActive() {
        assertEquals(
            Decision.TriggerNotActive,
            RuleEngine.evaluate(batteryRule(20), ctx(battery = 20)),
        )
    }

    @Test
    fun powerConnectedTriggerMatchesExactState() {
        val r = batteryRule().copy(trigger = Trigger.PowerConnected(true))
        assertEquals(Decision.Fired("set_dnd_on"), RuleEngine.evaluate(r, ctx(charging = true)))
        assertEquals(Decision.TriggerNotActive, RuleEngine.evaluate(r, ctx(charging = false)))
    }

    @Test
    fun timeDailyTriggersOnlyOnExactMinute() {
        val r = batteryRule().copy(trigger = Trigger.TimeDaily(1380))
        assertEquals(Decision.Fired("set_dnd_on"), RuleEngine.evaluate(r, ctx(now = 1380)))
        assertEquals(Decision.TriggerNotActive, RuleEngine.evaluate(r, ctx(now = 1379)))
    }

    @Test
    fun conditionTimeWindowSameDay() {
        val r = batteryRule().copy(conditions = listOf(Condition.TimeWindow(540, 660)))
        assertEquals(
            Decision.Fired("set_dnd_on"),
            RuleEngine.evaluate(r, ctx(now = 600, battery = 10)),
        )
        assertEquals(
            Decision.ConditionFailed,
            RuleEngine.evaluate(r, ctx(now = 700, battery = 10)),
        )
    }

    @Test
    fun conditionTimeWindowSpansMidnight() {
        val r = batteryRule().copy(conditions = listOf(Condition.TimeWindow(1320, 300)))
        assertEquals(
            Decision.Fired("set_dnd_on"),
            RuleEngine.evaluate(r, ctx(now = 1380, battery = 10)),
        )
        assertEquals(
            Decision.Fired("set_dnd_on"),
            RuleEngine.evaluate(r, ctx(now = 120, battery = 10)),
        )
        assertEquals(
            Decision.ConditionFailed,
            RuleEngine.evaluate(r, ctx(now = 600, battery = 10)),
        )
    }

    @Test
    fun conditionDayOfWeek() {
        val r = batteryRule().copy(conditions = listOf(Condition.DayOfWeek(setOf(0, 4))))
        assertEquals(
            Decision.Fired("set_dnd_on"),
            RuleEngine.evaluate(r, ctx(weekday = 0, battery = 10)),
        )
        assertEquals(
            Decision.ConditionFailed,
            RuleEngine.evaluate(r, ctx(weekday = 1, battery = 10)),
        )
    }

    @Test
    fun requiresUnlockBlocksWhenLocked() {
        assertEquals(
            Decision.RequiresUnlock,
            RuleEngine.evaluate(
                batteryRule(requiresUnlock = true),
                ctx(battery = 10, unlocked = false),
            ),
        )
    }

    @Test
    fun maxRunsCeiling() {
        assertEquals(
            Decision.MaxRunsReached,
            RuleEngine.evaluate(batteryRule(maxRuns = 3), ctx(battery = 10, runs = 3)),
        )
        assertEquals(
            Decision.Fired("set_dnd_on"),
            RuleEngine.evaluate(batteryRule(maxRuns = 3), ctx(battery = 10, runs = 2)),
        )
    }

    @Test
    fun disabledWinsOverEveryOtherFailure() {
        assertEquals(
            Decision.RuleDisabled,
            RuleEngine.evaluate(batteryRule(enabled = false), ctx(battery = 10, runs = 99)),
        )
    }

    @Test
    fun windowStartEqualsEndIsEmpty() {
        assertFalse(RuleEngine.inWindow(600, 600, 600))
    }

    @Test
    fun validationRejectsUnknownAction() {
        assertTrue(validateRule(batteryRule().copy(actionId = "format_device")) is ValidationResult.Invalid)
        assertTrue(validateRule(batteryRule()) is ValidationResult.Ok)
    }

    @Test
    fun validationRejectsThresholdOutOfRange() {
        assertTrue(validateRule(batteryRule(threshold = 0)) is ValidationResult.Invalid)
        assertTrue(validateRule(batteryRule(threshold = 100)) is ValidationResult.Invalid)
        assertTrue(validateRule(batteryRule(threshold = 99)) is ValidationResult.Ok)
    }

    @Test
    fun validationRejectsBlankFieldsAndZeroMaxRuns() {
        assertTrue(validateRule(batteryRule().copy(name = " ")) is ValidationResult.Invalid)
        assertTrue(validateRule(batteryRule().copy(explanation = "")) is ValidationResult.Invalid)
        assertTrue(validateRule(batteryRule(maxRuns = 0)) is ValidationResult.Invalid)
    }
}
