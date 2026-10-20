package com.miflix.native2.ui

import android.content.Context
import com.miflix.native2.data.CustomChannelServerRP45
import com.miflix.native2.data.ChannelMetadataRP50
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.StreamChoice
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import com.miflix.native2.data.LiveCategory
import com.miflix.native2.data.LiveChannel

internal data class CustomChannelRP45(val id:String,val name:String,val url:String,val number:Int=ChannelMetadataRP50.number(url),val logo:String?=null,val category:String="General")
/** Device-local account/profile edits. Included channels are read once from an asset, never polled. */
internal object CustomChannelsRP45 {
    const val provider="custom:rp45"
    private const val FACTORY="magma:rp51:"
    private const val OLD_FACTORY="tvclub:rp50:"
    val changesRP48=MutableStateFlow(0L)
    private var included:List<CustomChannelRP45>?=null
    private var logoIndexRP50:JSONObject?=null
    private fun logoRP50(context:Context,url:String,name:String):String? {
        val index=logoIndexRP50 ?: runCatching {JSONObject(context.assets.open("channel_logos_rp50.json").bufferedReader().use {it.readText()})}.getOrNull()?.also {logoIndexRP50=it} ?: return null
        val uri=runCatching {URI(url)}.getOrNull() ?: return null
        val key=uri.host.orEmpty().lowercase(java.util.Locale.ROOT)+uri.rawPath.orEmpty()
        val direct=index.optJSONObject("urls")?.optString(key).orEmpty()
        val simple=java.text.Normalizer.normalize(name.replace(Regex("\\s+HD$",RegexOption.IGNORE_CASE),"" ).lowercase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replace(Regex("[\\p{M}]"),"").replace(Regex("[^a-z0-9]"),"")
        return (direct.ifBlank {index.optJSONObject("names")?.optString(simple).orEmpty()}).takeIf {it.startsWith("https://")}
    }
    fun categoryRP48()=LiveCategory("tv","custom-rp48","Others")
    fun categoriesRP51()=listOf("Todos","General","Cine","24/7","Deportes").map {LiveCategory("tv","tirso:"+it,if(it=="Todos") "Tirso TV" else "Tirso TV · "+it)}
    fun labelRP50(c:CustomChannelRP45)=if(c.number>0) "${c.number} · ${c.name}" else c.name
    fun channelsRP48(context:Context,owner:String)=list(context,owner).map {LiveChannel(it.id,"tv",it.name,it.logo,"Tirso TV",number=it.number)}
    fun findRP48(context:Context,owner:String,id:String)=list(context,owner).firstOrNull {it.id==id}
    fun owner(state:AppState)="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    private fun prefs(context:Context)=context.getSharedPreferences("brunio_custom_channels_rp45",Context.MODE_PRIVATE)
    private fun bundled(context:Context):List<CustomChannelRP45> {
        included?.let {return it}
        return try {
            val a=JSONArray(context.assets.open("tvclub_channels_rp50.json").bufferedReader().use {it.readText()})
            (0 until a.length()).mapNotNull {i ->
                val o=a.getJSONObject(i);val n=o.optInt("number");val name=o.optString("name");val url=o.optString("url")
                if(n>0 && CustomChannelServerRP45.valid(name,url)) CustomChannelRP45(FACTORY+n,name,url,n,o.optString("logo").takeIf {it.startsWith("https://") } ?: logoRP50(context,url,name),o.optString("category","General")) else null
            }.distinctBy {it.id}.sortedBy {it.number}.also {included=it}
        } catch(_:Exception) {emptyList()}
    }
    private fun stored(context:Context,owner:String):List<CustomChannelRP45> = try {
        val array=JSONArray(prefs(context).getString(owner,"[]"))
        (0 until array.length()).mapNotNull {i ->
            val o=array.getJSONObject(i);val name=o.optString("name");val url=o.optString("url")
            if(CustomChannelServerRP45.valid(name,url)) CustomChannelRP45(o.getString("id"),name,url,ChannelMetadataRP50.number(url),logoRP50(context,url,name),o.optString("category","Personal")) else null
        }
    } catch(_:Exception) {emptyList()}
    private fun removed(context:Context,owner:String):Set<String> = try {
        val a=JSONArray(prefs(context).getString(owner+":removedRP50","[]"))
        (0 until a.length()).map {a.getString(it)}.toSet()
    } catch(_:Exception) {emptySet()}
    @Synchronized fun list(context:Context,owner:String):List<CustomChannelRP45> {
        val edits=stored(context,owner).filterNot {it.id.startsWith(OLD_FACTORY)};val hidden=removed(context,owner)
        val ids=edits.map {it.id}.toSet();val urls=edits.map {it.url}.toSet()
        val defaults=bundled(context).filter {it.id !in hidden && it.id !in ids && it.url !in urls}
        return (edits+defaults).sortedWith(compareBy<CustomChannelRP45> {if(it.number>0) it.number else Int.MAX_VALUE}.thenBy {it.name})
    }
    @Synchronized fun save(context:Context,owner:String,id:String?,name:String,url:String):Boolean {
        if(!CustomChannelServerRP45.valid(name.trim(),url.trim())) return false
        val visible=list(context,owner)
        val existing=if(id!=null) visible.firstOrNull {it.id==id} else visible.firstOrNull {it.url==url.trim()}
        if(id!=null && existing==null) return false
        val rows=stored(context,owner).toMutableList()
        if(existing==null && rows.count {!it.id.startsWith(FACTORY)}>=100) return false
        val value=CustomChannelRP45(existing?.id ?: java.util.UUID.randomUUID().toString(),name.trim(),url.trim(),logo=logoRP50(context,url.trim(),name.trim()),category=existing?.category ?: "Personal")
        rows.removeAll {it.id==value.id};rows.add(value)
        return write(context,owner,rows,removed(context,owner)-value.id)
    }
    @Synchronized fun remove(context:Context,owner:String,id:String):Boolean {
        val hidden=removed(context,owner).toMutableSet();if(id.startsWith(FACTORY)) hidden.add(id)
        return write(context,owner,stored(context,owner).filter {it.id!=id},hidden)
    }
    private fun write(context:Context,owner:String,rows:List<CustomChannelRP45>,hidden:Set<String>):Boolean {
        val array=JSONArray();rows.forEach {array.put(JSONObject().put("id",it.id).put("name",it.name).put("url",it.url).put("category",it.category))}
        val deleted=JSONArray();hidden.forEach {deleted.put(it)}
        val saved=prefs(context).edit().putString(owner,array.toString()).putString(owner+":removedRP50",deleted.toString()).commit()
        if(saved) changesRP48.value=changesRP48.value+1
        return saved
    }
    fun request(channel:CustomChannelRP45):PlayerRequest {
        val path=runCatching {URI(channel.url).path.orEmpty().lowercase(java.util.Locale.ROOT)}.getOrDefault("")
        val mime=when {path.endsWith(".m3u8")->"application/x-mpegURL";path.endsWith(".mpd")->"application/dash+xml";else->""}
        return PlayerRequest(MediaSummary(channel.id.hashCode(),"live",channel.name,"",channel.logo,channel.logo,0.0,"LIVE"),StreamChoice(channel.name,channel.url,channel.name),0,0,0L,
            live=true,liveChannelId=channel.id,liveProvider=provider,liveType="tv",liveMime=mime,returnScreen=Screen.LIVE_TV)
    }
    fun next(context:Context,state:AppState,request:PlayerRequest,direction:Int):PlayerRequest? {
        val rows=list(context,owner(state));val index=rows.indexOfFirst {it.id==request.liveChannelId}
        if(index<0) return null
        return rows.getOrNull(index+direction)?.let {CustomChannelsRP45.request(it)}
    }
}
