package com.miflix.native2.data

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class TirsoDayRP181Test {
    @Test fun everyDayCoversExactly24HoursWithoutOverlaps() {
        for(day in 1..366) {
            val rows=Tirso247ScheduleRP174.forDay(2028,day)
            assertEquals(0,rows.first().startMinute)
            assertEquals(1440,rows.last().endMinute)
            assertEquals(rows.size,rows.map {it.id}.distinct().size)
            rows.zipWithNext().forEach {(a,b)->assertEquals(a.endMinute,b.startMinute)}
            assertTrue(rows.all {it.tmdbId>0 && it.endMinute>it.startMinute})
            assertTrue(rows.filterNot {it.isMovie}.all {it.episodeOrdinal>0})
        }
    }
    @Test fun currentUsesDeviceLocalTimeAndPastSlotRetainsIdentity() {
        val clock=Calendar.getInstance(TimeZone.getTimeZone("America/Santo_Domingo"))
        clock.set(2026,Calendar.NOVEMBER,6,17,10,0)
        val program=Tirso247ScheduleRP174.current(clock)!!
        assertEquals(17*60,program.startMinute)
        clock.set(Calendar.HOUR_OF_DAY,23)
        assertNotEquals(program.id,Tirso247ScheduleRP174.current(clock)!!.id)
        assertEquals(program,Tirso247ScheduleRP174.today(clock).first {it.id==program.id})
    }
    @Test fun scheduleRotatesButRemainsStableWithinDay() {
        val a=Tirso247ScheduleRP174.forDay(2026,310)
        assertEquals(a,Tirso247ScheduleRP174.forDay(2026,310))
        assertNotEquals(a.map {it.title},Tirso247ScheduleRP174.forDay(2026,311).map {it.title})
        assertTrue(a.filter {it.isMovie}.map {it.tmdbId}.distinct().size>=5)
        assertTrue(a.filterNot {it.isMovie}.map {it.tmdbId}.distinct().size>=6)
    }
    @Test fun guideUsesTwelveHourClockAtMidnightNoonAndEvening() {
        assertEquals("12:00 AM",Tirso247ScheduleRP174.clock(0))
        assertEquals("12:00 PM",Tirso247ScheduleRP174.clock(720))
        assertEquals("4:30 PM",Tirso247ScheduleRP174.clock(990))
        assertEquals("12:00 AM",Tirso247ScheduleRP174.clock(1440))
    }
}
