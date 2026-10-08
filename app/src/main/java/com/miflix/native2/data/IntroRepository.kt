package com.miflix.native2.data

import com.miflix.native2.model.*
import org.json.JSONObject

class IntroRepository {
    suspend fun segments(imdb: String, season: Int, episode: Int, movie: Boolean): PlaybackSegments {
        if (!imdb.matches(Regex("tt[0-9]+"))) return PlaybackSegments()
        val query = if(movie) "is_movie=true" else "season=$season&episode=$episode"
        val j = JSONObject(Http.text("https://api.introdb.app/segments?imdb_id=$imdb&$query"))
        fun segment(key: String): SkipSegment? {
            val x = j.optJSONObject(key) ?: return null
            val start = x.optLong("start_ms", -1); val end = x.optLong("end_ms", -1)
            return if(start >= 0 && end > start) SkipSegment(start, end) else null
        }
        return PlaybackSegments(segment("intro"), segment("outro"), segment("post_credits"))
    }
}
