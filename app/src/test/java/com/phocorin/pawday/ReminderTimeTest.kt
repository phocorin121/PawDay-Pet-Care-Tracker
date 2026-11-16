package com.phocorin.pawday
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime
class ReminderTimeTest {
    private val now = ZonedDateTime.parse("2026-11-16T09:00:00-05:00[America/New_York]")
    @Test fun laterToday() { assertEquals("2026-11-16T10:00-05:00[America/New_York]", ReminderTime.next("10:00", setOf(1), now).toString()) }
    @Test fun passedToday() { assertEquals(23, ReminderTime.next("08:00", setOf(1), now)!!.dayOfMonth) }
    @Test fun sundayMapping() { assertEquals(22, ReminderTime.next("08:00", setOf(0), now)!!.dayOfMonth) }
    @Test fun emptyDays() { assertNull(ReminderTime.next("10:00", emptySet(), now)) }
    @Test fun invalidTime() { assertNull(ReminderTime.next("99:99", setOf(1), now)) }
    @Test fun equalTimeMovesForward() { assertEquals(23, ReminderTime.next("09:00", setOf(1), now)!!.dayOfMonth) }
    @Test fun dstTransition() { val n=ZonedDateTime.parse("2026-03-07T12:00:00-05:00[America/New_York]"); assertEquals(8,ReminderTime.next("02:30",setOf(0),n)!!.dayOfMonth) }
}
