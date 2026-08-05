package com.mychangli.app.feature.sports

import com.mychangli.app.feature.sports.ui.SportType
import com.mychangli.app.feature.sports.ui.LocalReservation
import com.mychangli.app.feature.sports.ui.ReservationStatus
import com.mychangli.app.feature.sports.ui.SportsReservationRules
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SportsReservationRulesTest {
    @Test
    fun `local demo inventory and prices remain explicit`() {
        assertEquals(8, SportType.BADMINTON.count)
        assertEquals(20, SportType.BADMINTON.priceYuan)
        assertEquals(10, SportType.TABLE_TENNIS.count)
        assertEquals(10, SportType.TABLE_TENNIS.priceYuan)
    }

    @Test
    fun `selection is limited to seven days valid slots and court range`() {
        val now = LocalDateTime.of(2026, 7, 28, 7, 0)
        assertEquals(
            true,
            SportsReservationRules.isValidSelection(
                SportType.BADMINTON,
                "2026-07-28",
                "08:00-09:00",
                8,
                now,
            ),
        )
        assertEquals(
            false,
            SportsReservationRules.isValidSelection(
                SportType.BADMINTON,
                "2026-08-04",
                "08:00-09:00",
                1,
                now,
            ),
        )
        assertEquals(
            false,
            SportsReservationRules.isValidSelection(
                SportType.BADMINTON,
                "2026-07-28",
                "08:00-09:00",
                9,
                now,
            ),
        )
    }

    @Test
    fun `cancelled and expired display states are derived without changing stored record`() {
        val reservation = LocalReservation(
            id = "r1",
            sport = SportType.TABLE_TENNIS,
            date = "2026-07-28",
            timeSlot = "08:00-09:00",
            courtNumber = 1,
            priceYuan = 10,
        )
        assertEquals(
            "已过期",
            SportsReservationRules.displayStatus(
                reservation,
                LocalDateTime.of(2026, 7, 28, 10, 0),
            ),
        )
        assertEquals(
            "已取消",
            SportsReservationRules.displayStatus(
                reservation.copy(status = ReservationStatus.CANCELLED),
                LocalDateTime.of(2026, 7, 28, 10, 0),
            ),
        )
        assertEquals(
            "已取消 · 管理员取消",
            SportsReservationRules.displayStatus(
                reservation.copy(
                    status = ReservationStatus.CANCELLED,
                    cancelledBy = "ADMIN",
                ),
                LocalDateTime.of(2026, 7, 28, 10, 0),
            ),
        )
    }
}
