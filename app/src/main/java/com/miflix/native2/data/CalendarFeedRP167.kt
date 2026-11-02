package com.miflix.native2.data

import android.content.Context
import androidx.work.*
import com.miflix.native2.BuildConfig
import com.miflix.native2.model.CloudSession
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

internal object CalendarFeedRP167 {
 private fun prefs(c:Context)=c.getSharedPreferences("brunio_calendar_feed_rp167",0)
 fun url(c:Context,owner:String)=prefs(c).getString("url:$owner","").orEmpty()
 private fun digest(ics:String)=java.security.MessageDigest.getInstance("SHA-256").digest(ics.lineSequence().filterNot {it.startsWith("DTSTAMP:")}.joinToString("\n").toByteArray()).joinToString("") {"%02x".format(it)}
 private fun endpoint()=BuildConfig.SUPABASE_URL.trimEnd('/')+"/functions/v1/brunio-calendar-feed"
 private suspend fun post(s:CloudSession,body:JSONObject)=JSONObject(Http.text(endpoint(),"POST",mapOf("apikey" to BuildConfig.SUPABASE_KEY,"Authorization" to "Bearer ${s.accessToken}","Content-Type" to "application/json"),body.toString()))
 suspend fun create(c:Context,s:CloudSession,profile:String,ics:String):String {
  val owner="${s.userId}:$profile"
  val result=post(s,JSONObject().put("action","create").put("profile",profile).put("ics",ics));val value=result.getString("url")
  require(value.startsWith(BuildConfig.SUPABASE_URL.trimEnd('/')+"/functions/v1/brunio-calendar-feed?"))
  prefs(c).edit().putString("url:$owner",value).putString("publisher:$owner",result.getString("publisher")).putString("digest:$owner",digest(ics)).putString("snapshot:$owner",ics).apply();schedule(c);return value
 }
 suspend fun update(c:Context,s:CloudSession,profile:String,ics:String) {
  val owner="${s.userId}:$profile";if(url(c,owner).isBlank() || prefs(c).getString("digest:$owner","")==digest(ics))return
  post(s,JSONObject().put("action","update").put("profile",profile).put("ics",ics));prefs(c).edit().putString("digest:$owner",digest(ics)).putString("snapshot:$owner",ics).apply()
 }
 suspend fun backgroundUpdate(c:Context,owner:String,ics:String) {
  val key=prefs(c).getString("publisher:$owner","").orEmpty();if(key.isBlank() || prefs(c).getString("digest:$owner","")==digest(ics))return
  Http.text(endpoint(),"POST",mapOf("apikey" to BuildConfig.SUPABASE_KEY,"Content-Type" to "application/json"),JSONObject().put("action","publish").put("publisher",key).put("ics",ics).toString())
  prefs(c).edit().putString("digest:$owner",digest(ics)).putString("snapshot:$owner",ics).apply()
 }
 suspend fun revoke(c:Context,s:CloudSession,profile:String) {post(s,JSONObject().put("action","revoke").put("profile",profile));prefs(c).edit().remove("url:${s.userId}:$profile").remove("publisher:${s.userId}:$profile").remove("digest:${s.userId}:$profile").apply()}
 fun saveShared(c:Context,owner:String,entries:List<CalendarIcsRP167.Entry>) {prefs(c).edit().putString("shared:$owner",JSONArray(entries.map {JSONObject().put("id",it.id).put("title",it.title).put("day",it.day).put("starts",it.starts ?: JSONObject.NULL).put("description",it.description)}).toString()).apply()}
 fun shared(c:Context,owner:String):List<CalendarIcsRP167.Entry> = runCatching {val a=JSONArray(prefs(c).getString("shared:$owner","[]"));(0 until a.length()).map {val j=a.getJSONObject(it);CalendarIcsRP167.Entry(j.getString("id"),j.getString("title"),j.getString("day"),if(j.isNull("starts"))null else j.getLong("starts"),j.optString("description"))}}.getOrDefault(emptyList())
 fun snapshot(c:Context,owner:String)=prefs(c).getString("snapshot:$owner","").orEmpty()
 private fun schedule(c:Context) {WorkManager.getInstance(c).enqueueUniquePeriodicWork("brunio_calendar_feed_rp167",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<CalendarFeedWorkerRP167>(6,TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()).build())}
}
class CalendarFeedWorkerRP167(c:Context,p:WorkerParameters):CoroutineWorker(c,p) {
 override suspend fun doWork():Result=try {
  val c=applicationContext;val store=LocalStore(c);val session=store.loadSession()
  if(session!=null) {
   val profile=store.profileId() ?: "default";val owner="${session.userId}:$profile"
   if(CalendarFeedRP167.url(c,owner).isNotBlank()) {
    withTimeout(60000) {CalendarStoreRP120.refresh(c,owner)}
    // Preserve shared app events from the last foreground snapshot while replacing local events.
    if(store.loadSession()?.userId==session.userId && store.profileId()==profile) {
     val ics=CalendarIcsRP167.encode(owner,CalendarIcsRP167.localEntries(c,owner)+CalendarFeedRP167.shared(c,owner))
     CalendarFeedRP167.backgroundUpdate(c,owner,ics)
    }
   }
  }
  Result.success()
 }catch(e:CancellationException){throw e}catch(_:Exception){Result.retry()}
}
