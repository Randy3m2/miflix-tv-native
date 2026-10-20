package com.miflix.native2.ui

import android.content.Context
import com.miflix.native2.data.CustomChannelServerRP45
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.StreamChoice
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

internal data class CustomChannelRP45(val id:String,val name:String,val url:String)
/** Device-local storage, explicitly scoped to account and profile. Stream tokens never enter Supabase. */
internal object CustomChannelsRP45 {
    const val provider="custom:rp45"
    fun owner(state:AppState)="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    private fun prefs(context:Context)=context.getSharedPreferences("brunio_custom_channels_rp45",Context.MODE_PRIVATE)
    @Synchronized fun list(context:Context,owner:String):List<CustomChannelRP45> = try {
        val array=JSONArray(prefs(context).getString(owner,"[]"))
        (0 until array.length()).mapNotNull {i ->
            val o=array.getJSONObject(i)
            val name=o.optString("name");val url=o.optString("url")
            if(CustomChannelServerRP45.valid(name,url)) CustomChannelRP45(o.getString("id"),name,url) else null
        }
    } catch(_:Exception) {emptyList()}
    @Synchronized fun save(context:Context,owner:String,id:String?,name:String,url:String):Boolean {
        if(!CustomChannelServerRP45.valid(name.trim(),url.trim())) return false
        val rows=list(context,owner).toMutableList()
        val index=if(id!=null) rows.indexOfFirst {it.id==id} else rows.indexOfFirst {it.url==url.trim()}
        if(id!=null && index<0) return false
        if(index<0 && rows.size>=100) return false
        val value=CustomChannelRP45(if(index>=0) rows[index].id else java.util.UUID.randomUUID().toString(),name.trim(),url.trim())
        if(index>=0) rows[index]=value else rows.add(value)
        return write(context,owner,rows)
    }
    @Synchronized fun remove(context:Context,owner:String,id:String)=write(context,owner,list(context,owner).filter {it.id!=id})
    private fun write(context:Context,owner:String,rows:List<CustomChannelRP45>):Boolean {
        val array=JSONArray();rows.forEach {array.put(JSONObject().put("id",it.id).put("name",it.name).put("url",it.url))}
        return prefs(context).edit().putString(owner,array.toString()).commit()
    }
    fun request(channel:CustomChannelRP45):PlayerRequest {
        val path=runCatching {URI(channel.url).path.orEmpty().lowercase(java.util.Locale.ROOT)}.getOrDefault("")
        val mime=when {path.endsWith(".m3u8")->"application/x-mpegURL";path.endsWith(".mpd")->"application/dash+xml";else->""}
        return PlayerRequest(MediaSummary(channel.id.hashCode(),"live",channel.name,"",null,null,0.0,"LIVE"),StreamChoice(channel.name,channel.url,channel.name),0,0,0L,
            live=true,liveChannelId=channel.id,liveProvider=provider,liveType="tv",liveMime=mime,returnScreen=Screen.LIVE_TV)
    }
    fun next(context:Context,state:AppState,request:PlayerRequest,direction:Int):PlayerRequest? {
        val rows=list(context,owner(state));val index=rows.indexOfFirst {it.id==request.liveChannelId}
        if(index<0) return null
        return rows.getOrNull(index+direction)?.let {CustomChannelsRP45.request(it)}
    }
}
