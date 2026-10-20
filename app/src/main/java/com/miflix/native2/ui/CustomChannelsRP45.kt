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

internal data class CustomChannelRP45(val id:String,val name:String,val url:String,val number:Int=ChannelMetadataRP50.number(url),val logo:String?=null,val category:String="General",val headers:Map<String,String> = emptyMap(),val sourceName:String="")
/** Device-local account/profile edits. Included channels are read once from an asset, never polled. */
internal object CustomChannelsRP45 {
    const val provider="custom:rp45"
    private const val FACTORY="magma:rp51:"
    private const val PLUS="iptvplus:rp61:"
    private fun includedIdRP61(id:String)=id.startsWith(FACTORY) || id.startsWith(PLUS) || id.startsWith("m3ulist:")
    private const val OLD_FACTORY="tvclub:rp50:"
    val changesRP48=MutableStateFlow(0L)
    private var appContextRP61:Context?=null
    fun findForStateRP61(state:AppState,id:String)=appContextRP61?.let {findRP48(it,owner(state),id)}
    private var included:List<CustomChannelRP45>?=null
    private var logoIndexRP50:JSONObject?=null
    private fun logoRP50(context:Context,url:String,name:String):String? {
        val index=logoIndexRP50 ?: runCatching {JSONObject(context.assets.open("channel_logos_rp50.json").bufferedReader().use {it.readText()})}.getOrNull()?.also {logoIndexRP50=it} ?: return null
        val uri=runCatching {URI(url)}.getOrNull() ?: return null
        val key=uri.host.orEmpty().lowercase(java.util.Locale.ROOT)+uri.rawPath.orEmpty()
        val direct=index.optJSONObject("urls")?.optString(key).orEmpty()
        val simple=java.text.Normalizer.normalize(name.replace(Regex("\\s+HD$",RegexOption.IGNORE_CASE),"" ).lowercase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replace(Regex("[\\p{M}]"),"").replace(Regex("[^a-z0-9]"),"")
        return (direct.ifBlank {index.optJSONObject("names")?.optString(simple).orEmpty()}).takeIf {it.startsWith("https://") || it.startsWith("file:///android_asset/")}
    }
    fun categoryRP48()=LiveCategory("tv","custom-rp48","Others")
    fun categoriesRP51()=listOf("Todos","General","Cine","24/7","Deportes","Infantiles","España","Cultura").map {LiveCategory("tv","tirso:"+it,if(it=="Todos") "Tirso TV" else it)}
    fun categoriesRP61()=listOf("Todos","General","Cine","Series","Deportes","Infantiles","España","Cultura","Noticias","Novelas","Música","Religión","Adultos").map {
        LiveCategory("tv","iptvplus:"+it,if(it=="Todos") "IPTV +" else "IPTV + · "+it)
    }
    fun matchesRP61(c:CustomChannelRP45,category:String):Boolean = when {
        category.startsWith("m3ulist:") -> M3UListsRP61.matches(c,category)
        category=="tirso:Todos" -> c.id.startsWith(FACTORY)
        category.startsWith("tirso:") -> c.id.startsWith(FACTORY) && c.category==category.removePrefix("tirso:")
        category=="iptvplus:Todos" -> c.id.startsWith(PLUS)
        category.startsWith("iptvplus:") -> c.id.startsWith(PLUS) && c.category==category.removePrefix("iptvplus:")
        else -> !includedIdRP61(c.id)
    }
    fun sourceRP61(c:CustomChannelRP45)=if(c.sourceName.isNotBlank()) c.sourceName else if(c.id.startsWith(PLUS)) "IPTV +" else if(c.id.startsWith(FACTORY)) "Tirso TV" else "Others"
    fun labelRP50(c:CustomChannelRP45)=if(c.number>0) "${c.number} · ${c.name}" else c.name
    fun channelsRP48(context:Context,owner:String)=list(context,owner).map {LiveChannel(it.id,"tv",it.name,it.logo,"Tirso TV",number=it.number)}
    fun findRP48(context:Context,owner:String,id:String)=list(context,owner,true).firstOrNull {it.id==id}
    fun owner(state:AppState)="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    private fun prefs(context:Context)=context.getSharedPreferences("brunio_custom_channels_rp45",Context.MODE_PRIVATE)
    private fun bundled(context:Context):List<CustomChannelRP45> {
        included?.let {return it}
        return try {
            listOf("tvclub_channels_rp50.json" to FACTORY,"iptv_plus_rp61.json" to PLUS).flatMap { (asset,prefix) ->
                val a=runCatching {JSONArray(context.assets.open(asset).bufferedReader().use {it.readText()})}.getOrElse {JSONArray()}
                (0 until a.length()).mapNotNull {i ->
                    val o=a.getJSONObject(i);val n=o.optInt("number");val name=o.optString("name");val url=o.optString("url")
                    if(n>0 && CustomChannelServerRP45.valid(name,url)) CustomChannelRP45(prefix+n,name,url,n,
                        o.optString("logo").takeIf {it.startsWith("https://") || it.startsWith("file:///android_asset/") } ?: logoRP50(context,url,name),o.optString("category","General")) else null
                }
            }.distinctBy {it.id}.also {included=it}
        } catch(_:Exception) {emptyList()}
    }
    private fun stored(context:Context,owner:String):List<CustomChannelRP45> = try {
        val array=JSONArray(prefs(context).getString(owner,"[]"))
        (0 until array.length()).mapNotNull {i ->
            val o=array.getJSONObject(i);val name=o.optString("name");val url=o.optString("url")
            if(CustomChannelServerRP45.valid(name,url)) CustomChannelRP45(o.getString("id"),name,url,ChannelMetadataRP50.number(url),logoRP50(context,url,name),o.optString("category","Personal"),o.optJSONObject("headers")?.let {h ->h.keys().asSequence().associateWith {h.getString(it)}} ?: emptyMap(),o.optString("sourceName")) else null
        }
    } catch(_:Exception) {emptyList()}
    private fun removed(context:Context,owner:String):Set<String> = try {
        val a=JSONArray(prefs(context).getString(owner+":removedRP50","[]"))
        (0 until a.length()).map {a.getString(it)}.toSet()
    } catch(_:Exception) {emptySet()}
    @Synchronized fun list(context:Context,owner:String,includeIptvPlusRP61:Boolean=false):List<CustomChannelRP45> {
        appContextRP61=context.applicationContext
        val imported=M3UListsRP61.channels(context,owner);val importedIds=imported.map {it.id}.toSet()
        val edits=stored(context,owner).filterNot {it.id.startsWith(OLD_FACTORY)}.filter { !it.id.startsWith("m3ulist:") || it.id in importedIds };val hidden=removed(context,owner)
        val ids=edits.map {it.id}.toSet();val urls=edits.map {it.url}.toSet()
        val defaults=(bundled(context)+imported).filter {it.id !in hidden && it.id !in ids && it.url !in urls}
        return (edits+defaults).filter {includeIptvPlusRP61 || (!it.id.startsWith(PLUS) && !it.id.startsWith("m3ulist:"))}.sortedWith(compareBy<CustomChannelRP45> {if(it.number>0) it.number else Int.MAX_VALUE}.thenBy {it.name})
    }
    @Synchronized fun save(context:Context,owner:String,id:String?,name:String,url:String):Boolean {
        if(!CustomChannelServerRP45.valid(name.trim(),url.trim())) return false
        val visible=list(context,owner,true)
        val existing=if(id!=null) visible.firstOrNull {it.id==id} else visible.firstOrNull {it.url==url.trim()}
        if(id!=null && existing==null) return false
        val rows=stored(context,owner).toMutableList()
        if(existing==null && rows.count {!includedIdRP61(it.id)}>=100) return false
        val value=CustomChannelRP45(existing?.id ?: java.util.UUID.randomUUID().toString(),name.trim(),url.trim(),logo=logoRP50(context,url.trim(),name.trim()),category=existing?.category ?: "Personal",headers=existing?.headers ?: emptyMap(),sourceName=existing?.sourceName.orEmpty())
        rows.removeAll {it.id==value.id};rows.add(value)
        return write(context,owner,rows,removed(context,owner)-value.id)
    }
    @Synchronized fun remove(context:Context,owner:String,id:String):Boolean {
        val hidden=removed(context,owner).toMutableSet();if(includedIdRP61(id)) hidden.add(id)
        return write(context,owner,stored(context,owner).filter {it.id!=id},hidden)
    }
    private fun write(context:Context,owner:String,rows:List<CustomChannelRP45>,hidden:Set<String>):Boolean {
        val array=JSONArray();rows.forEach {array.put(JSONObject().put("id",it.id).put("name",it.name).put("url",it.url).put("category",it.category).put("headers",JSONObject(it.headers)).put("sourceName",it.sourceName))}
        val deleted=JSONArray();hidden.forEach {deleted.put(it)}
        val saved=prefs(context).edit().putString(owner,array.toString()).putString(owner+":removedRP50",deleted.toString()).commit()
        if(saved) changesRP48.value=changesRP48.value+1
        return saved
    }
    fun request(channel:CustomChannelRP45):PlayerRequest {
        val path=runCatching {URI(channel.url).path.orEmpty().lowercase(java.util.Locale.ROOT)}.getOrDefault("")
        val mime=when {path.endsWith(".m3u8")->"application/x-mpegURL";path.endsWith(".mpd")->"application/dash+xml";else->""}
        return PlayerRequest(MediaSummary(channel.id.hashCode(),"live",channel.name,sourceRP61(channel),channel.logo,channel.logo,0.0,"LIVE"),StreamChoice(channel.name,channel.url,channel.name,requestHeaders=channel.headers),0,0,0L,
            live=true,liveChannelId=channel.id,liveProvider=provider,liveType="tv",liveMime=mime,returnScreen=Screen.LIVE_TV)
    }
    fun next(context:Context,state:AppState,request:PlayerRequest,direction:Int):PlayerRequest? {
        val family=request.liveChannelId.substringBeforeLast(":","")
        val rows=list(context,owner(state),true).filter {it.id.substringBeforeLast(":","")==family};val index=rows.indexOfFirst {it.id==request.liveChannelId}
        if(index<0) return null
        return rows.getOrNull(index+direction)?.let {CustomChannelsRP45.request(it)}
    }
}
