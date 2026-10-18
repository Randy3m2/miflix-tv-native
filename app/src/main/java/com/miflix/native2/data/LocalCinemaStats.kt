package com.miflix.native2.data

import android.content.SharedPreferences
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.MediaDetails
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/** Compact on-device aggregates; no URLs, frames, account calls or remote storage. */
internal class LocalCinemaStats(private val prefs:SharedPreferences) {
    private var owner=""
    private var data=JSONObject()
    private var dirty=false
    private var lastSave=0L
    private fun select(key:String) {
        if(key==owner) return
        flush();owner=key
        data=runCatching {JSONObject(prefs.getString("cinema:$key","{}").orEmpty())}.getOrDefault(JSONObject())
    }
    fun record(key:String,item:MediaSummary,detail:MediaDetails?,milliseconds:Long) {
        if(milliseconds<=0) return
        select(key)
        val month=SimpleDateFormat("yyyy-MM",Locale.US).format(Date())
        val m=data.optJSONObject(month) ?: JSONObject().also {data.put(month,it)}
        m.put("ms",m.optLong("ms")+milliseconds.coerceAtMost(6000))
        val titles=m.optJSONObject("titles") ?: JSONObject().also {m.put("titles",it)}
        if(titles.has(item.cloudId) || titles.length()<200) {
            val row=titles.optJSONObject(item.cloudId) ?: JSONObject().put("title",item.title).put("year",item.year).also {titles.put(item.cloudId,it)}
            row.put("ms",row.optLong("ms")+milliseconds.coerceAtMost(6000))
            if(detail!=null) {row.put("genres",JSONArray(detail.genres));row.put("directors",JSONArray(detail.directors))}
        }
        data.keys().asSequence().sortedDescending().drop(24).toList().forEach {data.remove(it)}
        dirty=true
        if(android.os.SystemClock.elapsedRealtime()-lastSave>=60000) flush()
    }
    fun flush() {if(dirty && owner.isNotBlank()) {prefs.edit().putString("cinema:$owner",data.toString()).apply();dirty=false;lastSave=android.os.SystemClock.elapsedRealtime()}}
    fun snapshot(key:String):JSONObject {select(key);flush();return JSONObject(data.toString())}
}
