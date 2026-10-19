package com.miflix.native2.data

import com.miflix.native2.model.CastMember
import org.json.JSONObject

/** Uses the credits already returned with details; does not turn creators/producers into directors. */
internal object DetailPeopleRP18 {
    fun directors(credits:JSONObject?,photo:(String)->String?):List<CastMember> {
        val crew=credits?.optJSONArray("crew") ?: return emptyList()
        return (0 until crew.length()).mapNotNull {i ->
            val row=crew.optJSONObject(i) ?: return@mapNotNull null
            val name=row.optString("name").trim()
            if(row.optString("job")!="Director" || row.optInt("id")<=0 || name.isBlank() || row.optBoolean("adult")) return@mapNotNull null
            CastMember(name,"Director",photo(row.optString("profile_path")),row.getInt("id"),creditRole="director")
        }.distinctBy {it.id}
    }
}
