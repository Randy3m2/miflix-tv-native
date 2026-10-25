package com.miflix.native2.data

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

internal object SportsBrowsePolicyRP121 {
    fun key(text:String)=Normalizer.normalize(text,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"")
        .lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+")," ").trim()
    // Only explicit ISO dates qualify an add-on listing for a different day.
    // Undated listings remain visible Today; we never guess future broadcasts from team names.
    fun visibleDay(name:String,description:String,day:String,today:String,continuous:Boolean=false,zone:java.time.ZoneId=java.time.ZoneId.systemDefault()):Boolean {
        LocalDate.parse(day)
        if(continuous) return true
        val text=name+" "+description
        val timestamp=Regex("20[0-9]{2}-[01][0-9]-[0-3][0-9]T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]+)?(?:Z|[+-][0-9]{2}:[0-9]{2})").findAll(text)
            .mapNotNull {runCatching {java.time.OffsetDateTime.parse(it.value).atZoneSameInstant(zone).toLocalDate().toString()}.getOrNull()}.toList()
        if(timestamp.isNotEmpty()) return day in timestamp
        val dates=Regex("(?<![0-9])20[0-9]{2}-[01][0-9]-[0-3][0-9](?![0-9])").findAll(text)
            .mapNotNull {runCatching {LocalDate.parse(it.value).toString()}.getOrNull()}.toList()
        return if(dates.isEmpty()) day==today else day in dates
    }
    fun startLabel(raw:String,zone:java.time.ZoneId=java.time.ZoneId.systemDefault()):String = runCatching {
        java.time.OffsetDateTime.parse(raw).atZoneSameInstant(zone).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
    }.getOrDefault("")
    fun lidom(text:String):Boolean {
        val value=" "+key(text)+" "
        return " lidom " in value || listOf("licey","aguilas","escogido","estrellas","gigantes","toros").count {" $it " in value}>=2
    }
    fun leagueEvent(section:String,text:String):Boolean {
        val value=" "+key(text)+" "
        return when(section) {
            "lidom"->lidom(text)
            "basketball"->listOf("wnba","ncaa","ncaab","euroleague").none {" $it " in value}
            "baseball"->!lidom(text) && listOf("npb","kbo","lmp").none {" $it " in value}
            "f1"->listOf("motogp","nascar","indycar","f2","f3").none {" $it " in value}
            else->true
        }
    }
    val nbaFeatured=listOf("1966","3975","3202","3945274","3032977","3112335","4065648","3059318","4278073","4594268","5104157","3136193","3992","6450","4279888","3908809","6606")
    val mlbFeatured=listOf("660271","592450","665742","545361","605141","547180","660670","664139","608070","592518","665487","646240","677594","514888","694973","666969")
    val f1Featured=listOf("max_verstappen","hamilton","norris","leclerc","alonso","russell","piastri","sainz")
    fun featured(league:String,id:String,name:String="")=when(league) {"NBA"->nbaFeatured.indexOf(id);"MLB"->mlbFeatured.indexOf(id);"LIDOM"->listOf("marcell ozuna","robinson cano","emilio bonifacio","junior lake","cesar valdez","hanser alberto","franmil reyes","jean segura","starling marte").indexOf(key(name)).let {if(it>=0) it else mlbFeatured.indexOf(id)};"F1"->f1Featured.indexOf(id);else->-1}
}
