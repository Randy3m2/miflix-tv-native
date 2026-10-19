package com.miflix.native2.data

import kotlinx.coroutines.withTimeout
import org.json.JSONObject

internal object ContentGuideClientRP33 {
    fun validPayload(data:JSONObject):Boolean=data.optJSONArray("parentsGuide")!=null && !data.has("error")
    suspend fun fetch(imdb:String):JSONObject {
        require(Regex("tt[0-9]{5,12}").matches(imdb))
        return withTimeout(10000) {
            val raw=Http.text("https://api.imdbapi.dev/titles/$imdb/parentsGuide",timeoutMs=8000)
            check(raw.length<=262144) {"Guide response too large"}
            val data=JSONObject(raw)
            check(validPayload(data)) {"Invalid guide response"}
            data
        }
    }
}
