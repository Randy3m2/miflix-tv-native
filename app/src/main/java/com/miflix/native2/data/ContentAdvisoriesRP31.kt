package com.miflix.native2.data

import org.json.JSONObject

internal object ContentAdvisoriesRP31 {
    data class Warning(val category:String,val severity:String)
    val categories=listOf("PROFANITY","VIOLENCE","SEXUAL_CONTENT","ALCOHOL_DRUGS","FRIGHTENING_INTENSE_SCENES")
    /** Preserve reported community votes; a missing category is not a 'none' rating. */
    fun parse(json:JSONObject):List<Warning> {
        val rows=json.optJSONArray("parentsGuide") ?: return emptyList()
        return (0 until rows.length()).mapNotNull {index ->
            val row=rows.optJSONObject(index) ?: return@mapNotNull null
            val category=row.optString("category")
            if(category !in categories) return@mapNotNull null
            val votes=row.optJSONArray("severityBreakdowns") ?: return@mapNotNull null
            val levels=listOf("none","mild","moderate","severe")
            val winner=(0 until votes.length()).mapNotNull {i ->
                val v=votes.optJSONObject(i) ?: return@mapNotNull null
                val level=v.optString("severityLevel").lowercase(java.util.Locale.ROOT)
                val count=v.optLong("voteCount",0)
                if(level !in levels || count<=0) null else level to count
            }.maxWithOrNull(compareBy<Pair<String,Long>> {it.second}.thenBy {levels.indexOf(it.first)})
            winner?.takeIf {it.first!="none"}?.let {Warning(category,it.first)}
        }.distinctBy {it.category}.sortedBy {categories.indexOf(it.category)}
    }
}
