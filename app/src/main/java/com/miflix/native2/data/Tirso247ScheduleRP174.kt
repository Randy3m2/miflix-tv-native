package com.miflix.native2.data

import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class TirsoScheduleItemRP174(
    val id: String, val title: String, val franchise: String,
    val startMinute: Int, val endMinute: Int, val dayKey: String,
    val playableQuery: String, val isMovie: Boolean = false,
)

object Tirso247ScheduleRP174 {
    const val channelId = "tirso-247"
    const val channelName = "Tirso 24/7"
    private val movieGenres = listOf("Action","Comedy","Sci-Fi","Horror","Thriller","Adventure","Animation","Fantasy")

    fun forDay(year: Int, dayOfYear: Int): List<TirsoScheduleItemRP174> {
        val dayKey = String.format(Locale.US, "%04d-%03d", year, dayOfYear)
        val seed = abs(year * 1000 + dayOfYear)
        val result = mutableListOf<TirsoScheduleItemRP174>()
        fun add(id: String, title: String, franchise: String, start: Int, duration: Int, query: String, movie: Boolean = false) {
            result += TirsoScheduleItemRP174(id,title,franchise,start,start+duration,dayKey,query,movie)
        }
        val dragon = listOf("Dragon Ball Z","Dragon Ball GT","Dragon Ball Super")[seed % 3]
        repeat(3) { i -> add("${dayKey}-db-${i}", "${dragon} · Episode ${((seed+i)%291)+1}", dragon, 18*60+i*40, 40, dragon) }
        repeat(3) { i ->
            val franchise = listOf("Naruto","Naruto Shippuden","Family Guy","DC Movies & Series","Marvel Movies & Series")[(seed+i)%5]
            add("${dayKey}-day-${i}", "${franchise} · Episode ${((seed/3+i)%220)+1}", franchise, 8*60+i*40, 40, franchise)
        }
        repeat(3) { i ->
            val franchise = if (i % 2 == 0) "Naruto Shippuden" else "Family Guy"
            add("${dayKey}-evening-${i}", "${franchise} · Episode ${((seed+i*7)%180)+1}", franchise, 20*60+i*30, 30, franchise)
        }
        val hero = if (seed % 2 == 0) "DC Movies & Series" else "Marvel Movies & Series"
        add("${dayKey}-hero", "${hero} Spotlight", hero, 22*60, 120, hero)
        val genre = movieGenres[(seed/7)%movieGenres.size]
        add("${dayKey}-movie", "Tirso Movie Night · ${genre}", "Movie", 0, 120, "movie ${genre}", true)
        return result.sortedBy { it.startMinute }
    }
    fun today(calendar: Calendar = Calendar.getInstance()) = forDay(calendar.get(Calendar.YEAR),calendar.get(Calendar.DAY_OF_YEAR))
    fun current(calendar: Calendar = Calendar.getInstance()): TirsoScheduleItemRP174? {
        val minute=calendar.get(Calendar.HOUR_OF_DAY)*60+calendar.get(Calendar.MINUTE)
        return today(calendar).firstOrNull { minute >= it.startMinute && minute < it.endMinute }
    }
}
