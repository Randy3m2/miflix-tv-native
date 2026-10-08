package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class RatingRepository(private val url: String,private val key: String) {
    private fun headers(s: CloudSession)=mapOf("apikey" to key,"Authorization" to "Bearer ${s.accessToken}","Content-Type" to "application/json")
    suspend fun load(s: CloudSession,profile: String): Map<String,Int> {
        val id=URLEncoder.encode(profile,"UTF-8")
        val result=mutableMapOf<String,Int>()
        var offset=0
        while(true) {
            val rows=JSONArray(Http.text("$url/rest/v1/miflix_ratings?profile_id=eq.$id&select=cloud_id,score&order=cloud_id.asc&limit=200&offset=$offset",headers=headers(s)))
            for(i in 0 until rows.length()) rows.getJSONObject(i).let { x -> result[x.getString("cloud_id")]=x.getInt("score") }
            if(rows.length()<200) break
            offset+=200
        }
        return result
    }
    suspend fun save(s: CloudSession,profile: String,media: String,score: Int) {
        require(score in 1..10)
        Http.text("$url/rest/v1/miflix_ratings?on_conflict=user_id,profile_id,cloud_id","POST",headers(s)+("Prefer" to "resolution=merge-duplicates,return=minimal"),
            JSONObject().put("user_id",s.userId).put("profile_id",profile).put("cloud_id",media).put("score",score).toString())
    }
}
