package com.miflix.native2.data

import org.json.JSONObject

internal data class AssistantPlanRP150(val genres:List<Int> = emptyList(),val minutes:Int=0,val decade:Int=0,val keyword:String="",val query:String="",val audio:String="",val minMinutes:Int=0) {
    fun checked():AssistantPlanRP150 {
        require(minMinutes in 0..600 && (minutes==0 || minMinutes<=minutes))
        require(minutes in 0..240 && (decade==0 || decade in 1900..2030 && decade%10==0))
        require(genres.size<=5 && genres.all {it in setOf(28,12,16,35,80,99,18,10751,14,36,27,10402,9648,10749,878,10770,53,10752,37)})
        require(keyword.length<=60 && query.length<=200 && audio in listOf("","es","en","fr","ko"))
        return this
    }
    fun constrainedBy(local:AssistantPlanRP150):AssistantPlanRP150 {
        checked();local.checked()
        val limit=if(local.minutes>0) if(minutes>0) minOf(local.minutes,minutes) else local.minutes else minutes
        return copy(genres=(local.genres+genres).distinct().take(5),minutes=if(local.minMinutes>0 && local.minutes==0) 0 else limit,minMinutes=maxOf(minMinutes,local.minMinutes),decade=local.decade.takeIf {it>0} ?: decade,keyword=local.keyword.ifBlank {keyword},audio=local.audio.ifBlank {audio},query=if(local.genres.isNotEmpty()||local.keyword.isNotBlank()||local.decade>0||local.minutes>0||local.minMinutes>0) "" else query).checked()
    }
    fun json():JSONObject {checked();return JSONObject().put("genres",org.json.JSONArray(genres)).put("minutes",minutes).put("decade",decade).put("keyword",keyword).put("query",query).put("audio",audio).put("minMinutes",minMinutes)}
    companion object {
        fun local(text:String,budget:Int=0):AssistantPlanRP150 {
            require(text.length<=500 && budget in 0..240)
            val q=DiscoveryMood.normalize(text)
            val genres=linkedMapOf(878 to listOf("ciencia ficcion","science fiction","sci fi"),28 to listOf("accion","action"),27 to listOf("terror","horror"),35 to listOf("comedia","comedy"),10752 to listOf("guerra","war"),16 to listOf("animacion","animation","anime"),9648 to listOf("misterio","mystery"),53 to listOf("suspenso","thriller"),18 to listOf("drama"),10749 to listOf("romance","romantica"),99 to listOf("documental","documentary"))
                .filterValues {words ->words.any {Regex("(?:^| )"+Regex.escape(it)+"(?: |$)").containsMatchIn(q)}}.keys.toList()
            val hours=Regex("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*(?:horas?|hours?|h)\\b").find(text)?.groupValues?.get(1)?.replace(',','.')?.toDoubleOrNull() ?: 0.0
            val minutes=Regex("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*(?:minutos?|minutes?|min)\\b").find(text)?.groupValues?.get(1)?.replace(',','.')?.toDoubleOrNull() ?: 0.0
            val time=(hours*60+minutes).toInt().coerceIn(0,240)
            val greater=q.contains("mas de ") || q.contains("more than ") || q.contains("longer than ") || q.contains("over ")
            val atLeast=q.contains("al menos ") || q.contains("at least ")
            val minimum=if(time>0 && (greater || atLeast)) time+if(greater) 1 else 0 else 0
            val year=Regex("\\b(19\\d0|20[0-3]0)\\b").find(q)?.value?.toIntOrNull()
                ?: if(Regex("\\b90s?\\b").containsMatchIn(q)) 1990 else 0
            val keyword=when {q.contains("viajes en el tiempo")||q.contains("time travel")->"time travel";q.contains("finales inesperados")||q.contains("plot twist")->"plot twist";else->""}
            val audio=when {q.contains("espanol")||q.contains("spanish")||q.contains("latino")->"es";q.contains("french")||q.contains("frances")->"fr";q.contains("korean")||q.contains("coreano")->"ko";q.contains("english")||q.contains("ingles")->"en";else->""}
            return AssistantPlanRP150(genres,if(budget>0) budget else if(minimum>0) 0 else (time-if(time>0 && (q.contains("menos de ")||q.contains("under ")||q.contains("less than "))) 1 else 0).coerceIn(0,240),year,keyword,if(genres.isEmpty()&&keyword.isEmpty()&&year==0&&time==0&&budget==0) text.trim().take(200) else "",audio,minimum).checked()
        }
        fun json(raw:String):AssistantPlanRP150 {
            val j=JSONObject(raw);val a=j.optJSONArray("genres")
            return AssistantPlanRP150((0 until (a?.length() ?: 0)).map {a!!.getInt(it)},j.optInt("minutes"),j.optInt("decade"),j.optString("keyword"),j.optString("query"),j.optString("audio"),j.optInt("minMinutes")).checked()
        }
    }
}
