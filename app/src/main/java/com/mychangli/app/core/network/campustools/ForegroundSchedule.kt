package com.mychangli.app.core.network.campustools

/** Pure policy: first use on a new Beijing day bypasses the 15-minute throttle. */
internal object ForegroundSchedule {
    private const val DAY = 86_400_000L
    fun day(time: Long): Long = Math.floorDiv(time + 8 * 3_600_000L, DAY)
    fun due(now: Long, lastEventAt: Long, changed: Boolean): Boolean =
        changed || day(now) != day(lastEventAt) || now - lastEventAt !in 0 until 900_000L
}
