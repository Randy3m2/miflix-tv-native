package com.miflix.native2.ui

import java.net.URI
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

internal fun f1ChannelRP42(name:String):Boolean {
    val text=Normalizer.normalize(name,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).replace('-',' ').replace('_',' ')
    return Regex("\\bf\\s*1(?:\\s*tv)?\\b|\\bformula\\s*(1|one|uno)\\b|\\b(?:movistar|dazn)\\s*f\\s*1\\b").containsMatchIn(text)
}
internal fun playlistWebRP42(url:String):Boolean {
    val u=runCatching {URI(url)}.getOrNull() ?: return false
    return u.host?.lowercase(Locale.ROOT) in setOf("sportsbite.org","www.sportsbite.org") || u.path.orEmpty().lowercase(Locale.ROOT).let {it.endsWith(".html") || it.endsWith(".htm")}
}
internal fun moveLidomDayRP42(date:String,days:Int):String {
    val format=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply {timeZone=TimeZone.getTimeZone("America/Santo_Domingo");isLenient=false}
    val calendar=Calendar.getInstance(format.timeZone).apply {time=requireNotNull(format.parse(date));add(Calendar.DATE,days)}
    return format.format(calendar.time)
}

/** DAZN's general channels were requested too; their presence does not confirm an F1 broadcast. */
internal fun requestedF1StationRP43(name:String):Boolean {
    if(f1ChannelRP42(name)) return true
    val text=name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+")," ")
    return Regex("dazn(?:\\s*\\d+)?(?:\\s+(?:hd|fhd|uhd|4k|es|spain|uk|us))*").matches(text)
}
