package com.miflix.native2.data
import com.miflix.native2.model.CloudSession
import org.json.JSONObject

class TraktRepository(private val url: String, private val key: String) {
 suspend fun call(session: CloudSession, action: String, fields: JSONObject = JSONObject()): JSONObject {
  val text=Http.text("$url/functions/v1/miflix-trakt","POST",mapOf("apikey" to key,"Authorization" to "Bearer ${session.accessToken}"),fields.put("action",action).toString(),30000)
  return JSONObject(text)
 }
}
