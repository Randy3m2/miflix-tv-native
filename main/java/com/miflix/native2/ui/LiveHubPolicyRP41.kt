package com.miflix.native2.ui

import com.miflix.native2.data.LiveCategory

internal val liveHubKeysRP41=listOf("tv","basketball","baseball","f1","lidom","scores","soccer","football","others")
internal fun liveSportGroupRP41(category:LiveCategory):String {
    val text=(category.name+" "+category.id+" "+category.genre.orEmpty()).lowercase(java.util.Locale.ROOT).replace('-',' ').replace('_',' ')
    return when {
        listOf("american football","americanfootball","nfl","ncaaf","fútbol americano","futbol americano").any {it in text}->"football"
        listOf("basketball","basket ball","nba","baloncesto").any {it in text}->"basketball"
        listOf("baseball","mlb","béisbol","beisbol").any {it in text}->"baseball"
        listOf("formula 1","formula1","formula one","fórmula","f1","motorsport","motor sport","racing").any {it in text}->"f1"
        listOf("soccer","football","fútbol","futbol").any {it in text}->"soccer"
        else->"others"
    }
}
internal fun liveAggregateRP41(category:LiveCategory):Boolean {
    val name=category.name.lowercase(java.util.Locale.ROOT).trim()
    return name in setOf("all","all sports","all events","events","live events","all channels","all streams","live streams","upcoming","upcoming events","live now","live","todos","todos los eventos","en vivo","ahora en vivo")
}
