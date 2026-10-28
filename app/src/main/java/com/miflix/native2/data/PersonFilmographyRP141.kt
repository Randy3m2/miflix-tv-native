package com.miflix.native2.data

import org.json.JSONObject
import java.util.Locale

internal object PersonFilmographyRP141 {
    fun rows(body:JSONObject,personId:Int,role:String):List<JSONObject> {
        require(personId>0 && body.optInt("id")==personId) {"Filmography identity mismatch"}
        val director=role=="director"
        val credits=body.optJSONArray(if(director) "crew" else "cast") ?: return emptyList()
        return (0 until credits.length()).mapNotNull {credits.optJSONObject(it)}.filter {row->
            val media=row.optString("media_type")
            val genres=row.optJSONArray("genre_ids")
            val talk=media=="tv" && (0 until (genres?.length() ?: 0)).any {genres!!.optInt(it) in setOf(10763,10767)}
            val character=row.optString("character").trim().lowercase(Locale.ROOT)
            val self=Regex("^(self|himself|herself|themselves|él mismo|el mismo|ella misma)(\\s*[-–(/].*)?$").matches(character) || character.contains("archive footage")
            row.optInt("id")>0 && !row.optBoolean("adult") && media in setOf("movie","tv") &&
                (if(director) row.optString("job")=="Director" else !talk && !self)
        }
    }
}
