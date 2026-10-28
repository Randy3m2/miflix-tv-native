package com.miflix.native2.data

internal object MobileLatestRP139 {
    fun validFeed(raw:String):Boolean=raw.isBlank() || runCatching {
        val uri=java.net.URI(raw.trim());uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.userInfo==null && uri.fragment==null
    }.getOrDefault(false)
    fun leagueName(id:String,spanish:Boolean=false)=when(id) {
        "MOTOGP"->"MotoGP";"F1"->"F1";"BOXING"->if(spanish) "Boxeo" else "Boxing"
        "GRAMMYS"->"Grammys";"OSCARS"->"Oscars";"MISS_UNIVERSE"->"Miss Universe";else->id
    }
    fun recordingGroup(game:LiveChannel,category:LiveCategory?):String {
        val title=RecordingSportRP97.group(game.name)
        return if(title!="other") title else RecordingSportRP97.group(game.description,category?.name,category?.id,category?.genre)
    }
    fun recordingMode(league:String,category:LiveCategory?,games:List<LiveChannel>)=league=="Others" &&
        (SportsRecordingPolicyRP72.recording(category?.name,category?.id,category?.genre) || games.any {SportsRecordingPolicyRP72.recording(it.name,it.description)})
    fun logosByDay(events:List<SportEventRP120>)=events.groupBy {it.day()}.mapValues {(_,rows)->
        rows.map {it.logos.firstOrNull() ?: CalendarSourcesRP120.logo(it.league)}.filter {it.isNotBlank()}.distinct()
    }
    fun dailyLogos(events:List<SportEventRP120>,date:String)=logosByDay(events)[date].orEmpty()
}
