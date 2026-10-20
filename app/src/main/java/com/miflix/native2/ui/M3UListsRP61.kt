package com.miflix.native2.ui

import android.content.Context
import com.miflix.native2.data.IptvPlaylist
import com.miflix.native2.data.LiveCategory
import com.miflix.native2.data.SourceHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

internal object M3UListsRP61 {
    private fun prefs(c:Context)=c.getSharedPreferences("brunio_m3u_lists_rp61",Context.MODE_PRIVATE)
    private var cacheOwner=""
    private var cacheRaw:String?=null
    private var cacheRows:List<JSONObject> = emptyList()
    @Synchronized private fun records(c:Context,owner:String):List<JSONObject> = runCatching {
        val raw=prefs(c).getString(owner,"[]") ?: "[]"
        if(owner==cacheOwner && raw==cacheRaw) cacheRows else {
            val a=JSONArray(raw);val rows=(0 until a.length()).map {a.getJSONObject(it)}
            cacheOwner=owner;cacheRaw=raw;cacheRows=rows;rows
        }
    }.getOrDefault(emptyList())
    fun lists(c:Context,owner:String)=records(c,owner).map {Triple(it.getString("id"),it.getString("name"),it.getJSONArray("channels").length())}
    fun categories(c:Context,owner:String):List<LiveCategory> = records(c,owner).flatMap {r ->
        val id=r.getString("id");val name=r.getString("name");val a=r.getJSONArray("channels")
        val groups=(0 until a.length()).map {a.getJSONObject(it).optString("category","General")}.distinct().sorted()
        listOf(LiveCategory("tv","m3ulist:$id:Todos",name))+groups.map {LiveCategory("tv","m3ulist:$id:$it","$name · $it")}
    }
    fun matches(channel:CustomChannelRP45,category:String):Boolean {
        val parts=category.split(':',limit=3)
        return parts.size==3 && channel.id.startsWith("m3ulist:${parts[1]}:") &&
            (parts[2]=="Todos" || channel.category==parts[2])
    }
    fun channels(c:Context,owner:String):List<CustomChannelRP45> = records(c,owner).flatMap {r ->
        val a=r.getJSONArray("channels");(0 until a.length()).map {i ->
            val o=a.getJSONObject(i);val h=o.optJSONObject("headers") ?: JSONObject()
            CustomChannelRP45(o.getString("id"),o.getString("name"),o.getString("url"),i+1,
                o.optString("logo").takeIf {it.isNotBlank()},o.optString("category","General"),
                h.keys().asSequence().associateWith {h.getString(it)},r.getString("name"))
        }
    }
    /** Runs on the receiver worker, never on the UI thread. Import is an explicit snapshot. */
    fun prepare(name:String,url:String,text:String):JSONObject = runBlocking(Dispatchers.IO) {
        require(name.isNotBlank() && name.length<=80 && name.none {it.isISOControl()}) {"Revisa el nombre / Check the list name"}
        val raw=if(text.isNotBlank()) text else SourceHttp.text(url,"M3U import")
        require(raw.toByteArray(Charsets.UTF_8).size<=2*1024*1024) {"Máximo 2 MB / Maximum 2 MB"}
        val rows=IptvPlaylist.parse(raw).distinctBy {it.url}
        require(rows.isNotEmpty() && rows.size<=5000) {"Lista vacía o más de 5000 canales / Empty list or more than 5000 channels"}
        val id=UUID.randomUUID().toString();val channels=JSONArray()
        rows.forEach {row ->
            val hash=MessageDigest.getInstance("SHA-256").digest(row.url.toByteArray()).joinToString("") {"%02x".format(it)}.take(24)
            channels.put(JSONObject().put("id","m3ulist:$id:$hash").put("name",row.name).put("url",row.url)
                .put("logo",row.logo ?: "").put("category",row.groups.firstOrNull()?.takeUnless {it=="Undefined"} ?: "General")
                .put("headers",JSONObject(row.headers)))
        }
        JSONObject().put("id",id).put("name",name.trim()).put("channels",channels)
    }
    @Synchronized fun save(c:Context,owner:String,prepared:JSONObject):Boolean {
        val rows=records(c,owner);if(rows.size>=10) return false
        val ok=prefs(c).edit().putString(owner,JSONArray(rows+prepared).toString()).commit()
        if(ok) CustomChannelsRP45.changesRP48.value++
        return ok
    }
    @Synchronized fun remove(c:Context,owner:String,id:String):Boolean {
        val ok=prefs(c).edit().putString(owner,JSONArray(records(c,owner).filterNot {it.optString("id")==id}).toString()).commit()
        if(ok) CustomChannelsRP45.changesRP48.value++
        return ok
    }
}
