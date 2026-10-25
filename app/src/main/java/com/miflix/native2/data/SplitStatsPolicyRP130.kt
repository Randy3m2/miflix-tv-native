package com.miflix.native2.data

internal object SplitStatsPolicyRP130 {
    fun mode(count:Int,fullScreen:Boolean=false)=if(fullScreen) 0 else when(count) {2->2;3->3;else->0}
    fun preferred(title:String):String=when {
        Regex("(?i)lidom|licey|aguilas|águilas|gigantes|escogido|estrellas|toros").containsMatchIn(title)->"LIDOM"
        Regex("(?i)mlb|baseball|béisbol|beisbol").containsMatchIn(title)->"MLB"
        else->"NBA"
    }
    fun match(title:String,games:List<LiveScoreRP11>):LiveScoreRP11? {
        SportsStatsRP95.match(title,games)?.let {return it}
        // A team-owned LIDOM channel is unambiguous only with a single matching fixture.
        if(preferred(title)!="LIDOM") return null
        fun normal(s:String)=java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"),"").lowercase(java.util.Locale.ROOT)
        val hay=normal(title)
        return games.filter {it.league=="LIDOM" && listOf(it.away,it.home).any {team->
            normal(team.name).split(" ").filter {it.length>=5}.any {it in hay}
        }}.singleOrNull()
    }
}
