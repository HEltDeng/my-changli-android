package com.mychangli.app.core.network.campustools

import org.junit.Assert.*
import org.junit.Test

class ForegroundScheduleTest {
    @Test fun firstUseAcrossBeijingMidnightBypassesThrottle() {
        val midnight = 20_000L * 86_400_000L - 8 * 3_600_000L
        assertTrue(ForegroundSchedule.due(midnight + 1_000, midnight - 1_000, false))
        assertFalse(ForegroundSchedule.due(midnight + 2_000, midnight + 1_000, false))
    }
    @Test fun delayedYesterdayRetryStillRequiresTodaysEvent() {
        val now = 20_000L * 86_400_000L
        assertTrue(ForegroundSchedule.due(now, now - 86_400_000, false))
        assertFalse(ForegroundSchedule.due(now + 1, now, false))
    }
    @Test fun versionActorChangesAndClockReversalRequireFreshEvent() {
        assertTrue(ForegroundSchedule.due(10_000, 9_999, true))
        assertTrue(ForegroundSchedule.due(9_000, 10_000, false))
        assertFalse(ForegroundSchedule.due(10_000, 9_999, false))
    }
}
