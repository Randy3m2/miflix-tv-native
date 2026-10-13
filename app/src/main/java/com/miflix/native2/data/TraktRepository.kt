package com.miflix.native2.data
import com.miflix.native2.model.CloudSession
import com.miflix.native2.model.MediaSummary
import org.json.JSONObject

class TraktRepository(private val url: String, private val key: String) {
 private val commentCache=linkedMapOf<String,Pair<Long,List<TraktComment>>>()
 suspend fun call(session: CloudSession, action: String, fields: JSONObject = JSONObject()): JSONObject {
  val text=Http.text("$url/functions/v1/miflix-trakt","POST",mapOf("apikey" to key,"Authorization" to "Bearer ${session.accessToken}"),fields.put("action",action).toString(),30000)
  return JSONObject(text)
 }
 suspend fun comments(session: CloudSession, item: MediaSummary): List<TraktComment> {
  val cacheKey="${session.userId}:${item.cloudId}"
  commentCache[cacheKey]?.takeIf { android.os.SystemClock.elapsedRealtime()-it.first<300_000L }?.let { return it.second }
  val result=call(session,"comments",JSONObject().put("tmdb",item.id).put("type",item.type))
  if(result.has("error")) throw IllegalStateException(result.optString("error"))
  val rows=result.optJSONArray("items") ?: return emptyList()
  val comments=(0 until rows.length()).mapNotNull { index ->
   val row=rows.optJSONObject(index) ?: return@mapNotNull null
   val text=row.optString("text")
   if(text.isBlank()) return@mapNotNull null
   TraktComment(row.optLong("id"),row.optString("author"),text,row.optBoolean("spoiler") ||
    Regex("\\[spoiler(?:=[^\\]]*)?\\]",RegexOption.IGNORE_CASE).containsMatchIn(text),row.optInt("likes"),row.optString("date"))
  }
  if(commentCache.size>=24) commentCache.remove(commentCache.keys.first())
  commentCache[cacheKey]=android.os.SystemClock.elapsedRealtime() to comments
  return comments
 }
}
data class TraktComment(val id: Long,val author: String,val text: String,val spoiler: Boolean,val likes: Int,val date: String)
