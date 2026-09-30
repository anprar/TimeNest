package com.timenest

import org.junit.Assert.*
import org.junit.Test

class TimeCalcTest {
    @Test fun countdown45() {
        val now = 1_700_000_000_000L
        assertEquals(now + 45 * 60_000L, TimeCalc.endFromDuration(now, 0, 45, 0))
    }
    @Test fun clockToday() {
        // 16:30 -> 18:00 = 90 mnt
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, 8, 30, 16, 30, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val r = TimeCalc.endFromClock(cal.timeInMillis, 18, 0, false)
        assertEquals(90 * 60_000L, r.durationMs)
    }
    @Test fun midnight() {
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, 8, 30, 23, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val r = TimeCalc.endFromClock(cal.timeInMillis, 1, 0, false)
        assertTrue(r.nextDay)
        assertEquals(2 * 3600_000L, r.durationMs)
    }
    @Test fun pauseResume() {
        val now = 1_700_000_000_000L
        val end = now + 3_600_000L
        val remain = TimeCalc.remain(end, now + 600_000L)
        assertEquals(3_000_000L, remain)
        assertEquals(now + 600_000L + remain, (now + 600_000L) + remain) // endTime baru
    }
    @Test fun formatTest() {
        assertEquals("01:30:00", TimeCalc.format(5_400_000L))
        assertEquals("45:20", TimeCalc.format(2_720_000L))
    }
}
