package com.miflix.native2.p2p

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/** Internal URI contains only a validated v1 hash, optional file index and trackers. */
data class TorrentSpecRP14(val hash:String,val fileIndex:Int?,val trackers:List<String>) {
    fun magnet():String="magnet:?xt=urn:btih:$hash"+trackers.joinToString("") {"&tr="+encode(it)}
    fun uri():String="brunio-p2p://$hash/${fileIndex ?: -1}"+trackers.joinToString("&",prefix=if(trackers.isEmpty()) "" else "?") {"tr="+encode(it)}
    companion object {
        private fun encode(s:String)=URLEncoder.encode(s,"UTF-8")
        private fun query(raw:String?):List<Pair<String,String>> = raw.orEmpty().split('&').mapNotNull {
            val parts=it.split('=',limit=2)
            if(parts.size!=2) null else URLDecoder.decode(parts[0],"UTF-8") to URLDecoder.decode(parts[1],"UTF-8")
        }
        fun create(hash:String,index:Int?,trackers:List<String>):TorrentSpecRP14? {
            val h=hash.lowercase(java.util.Locale.ROOT)
            if(!Regex("[0-9a-f]{40}").matches(h) || (index!=null && index<0)) return null
            val safe=trackers.filter {it.length<=2048 && runCatching {URI(it).let {u ->u.scheme in listOf("udp","http","https") && !u.host.isNullOrBlank() && u.userInfo==null}}.getOrDefault(false)}.distinct().take(16)
            return TorrentSpecRP14(h,index,safe)
        }
        fun fromMagnet(value:String,index:Int?=null):TorrentSpecRP14? = runCatching {
            if(!value.startsWith("magnet:?")) return null
            val q=query(value.substringAfter('?'))
            val hash=q.firstOrNull {it.first=="xt" && it.second.startsWith("urn:btih:")}?.second?.substringAfter("urn:btih:") ?: return null
            create(hash,index,q.filter {it.first=="tr"}.map {it.second})
        }.getOrNull()
        fun parse(value:String):TorrentSpecRP14? = runCatching {
            val u=URI(value)
            if(u.scheme!="brunio-p2p") return null
            val i=u.path.removePrefix("/").toIntOrNull() ?: return null
            if(i< -1) return null
            create(u.host.orEmpty(),i.takeIf {it>=0},query(u.rawQuery).filter {it.first=="tr"}.map {it.second})
        }.getOrNull()
    }
}
