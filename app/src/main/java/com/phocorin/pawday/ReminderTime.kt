package com.phocorin.pawday

import java.time.*

object ReminderTime {
    fun next(time: String, days: Set<Int>, now: ZonedDateTime): ZonedDateTime? {
        val t = try { LocalTime.parse(time) } catch (_: Exception) { return null }
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            val candidate = date.atTime(t).atZone(now.zone)
            if (date.dayOfWeek.value % 7 in days && candidate.isAfter(now)) return candidate
        }
        return null
    }
}
