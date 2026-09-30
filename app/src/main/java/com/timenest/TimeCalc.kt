package com.timenest

import java.util.Calendar

object TimeCalc {
    fun endFromDuration(now: Long, h: Int, m: Int, s: Int): Long {
        val total = (h * 3600L + m * 60L + s) * 1000L
        require(total in 60_000L..86_400_000L) { "Durasi 1 menit - 24 jam" }
        return now + total
    }

    data class EndResolve(val endTime: Long, val nextDay: Boolean, val durationMs: Long)

    fun endFromClock(now: Long, hour: Int, min: Int, forceNextDay: Boolean = false): EndResolve {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val target = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var nextDay = forceNextDay
        if (!forceNextDay && target.timeInMillis <= now) nextDay = true
        if (nextDay) target.add(Calendar.DAY_OF_YEAR, 1)
        return EndResolve(target.timeInMillis, nextDay, target.timeInMillis - now)
    }

    fun remain(end: Long, now: Long) = (end - now).coerceAtLeast(0L)

    fun format(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, sec)
        else "%02d:%02d".format(m, sec)
    }

    fun summary(ms: Long): String {
        val m = ms / 60000
        val h = m / 60
        return if (h > 0) "$h jam ${m % 60} mnt" else "$m mnt"
    }
}
