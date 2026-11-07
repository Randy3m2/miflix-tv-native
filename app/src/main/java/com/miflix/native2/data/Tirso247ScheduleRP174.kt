package com.miflix.native2.data

import java.util.Calendar
import java.util.Locale

data class TirsoScheduleItemRP174(
    val id: String, val title: String, val franchise: String,
    val startMinute: Int, val endMinute: Int, val dayKey: String,
    val playableQuery: String, val isMovie: Boolean = false,
    val tmdbId:Int=0, val episodeOrdinal:Int=0,
)

object Tirso247ScheduleRP174 {
    const val channelId = "tirso-247"
    const val channelName = "Tirso 24/7"

    fun forDay(year: Int, dayOfYear: Int): List<TirsoScheduleItemRP174> {
        return TirsoDayRP181.build(year,dayOfYear)
    }
    fun today(calendar: Calendar = Calendar.getInstance()) = forDay(calendar.get(Calendar.YEAR),calendar.get(Calendar.DAY_OF_YEAR))
    fun clock(minute:Int):String {
        val h=(minute/60)%24
        return String.format(Locale.US,"%d:%02d %s",if(h%12==0) 12 else h%12,minute%60,if(h<12) "AM" else "PM")
    }
    fun current(calendar: Calendar = Calendar.getInstance()): TirsoScheduleItemRP174? {
        val minute=calendar.get(Calendar.HOUR_OF_DAY)*60+calendar.get(Calendar.MINUTE)
        return today(calendar).firstOrNull { minute >= it.startMinute && minute < it.endMinute }
    }
}
