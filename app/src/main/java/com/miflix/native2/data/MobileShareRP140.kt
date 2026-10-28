package com.miflix.native2.data

import com.miflix.native2.model.MediaSummary
import java.net.URI

internal object MobileShareRP140 {
    fun link(item:MediaSummary)="https://miflix-tv-native-web.vercel.app/watch.html?type=${if(item.type=="series") "series" else "movie"}&id=${item.id}"
    fun imdbLink(id:String?)=id?.takeIf {it.matches(Regex("tt[0-9]{7,12}"))}?.let {"https://www.imdb.com/title/$it/"}
    fun text(item:MediaSummary,url:String=link(item))=listOf(item.title,item.overview.trim(),url).filter {it.isNotBlank()}.joinToString("\n\n")
    fun cloudId(raw:String?):String?=runCatching {
        val uri=URI(raw ?: return null)
        if(uri.userInfo!=null || uri.fragment!=null) return null
        val parts=if(uri.scheme=="https" && uri.host=="miflix-tv-native-web.vercel.app" && uri.path=="/watch.html") {
            val query=uri.rawQuery.orEmpty().split('&').map {it.split('=',limit=2)}
            if(query.size!=2 || query.any {it.size!=2} || query.map {it[0]}.toSet()!=setOf("type","id")) return null
            val args=query.associate {it[0] to it[1]};listOf(args["type"].orEmpty(),args["id"].orEmpty())
        } else {
            if(uri.scheme!="brunio" || uri.host!="content" || uri.query!=null) return null
            uri.path.split('/').filter {it.isNotEmpty()}
        }
        if(parts.size!=2 || parts[0] !in listOf("movie","series")) return null
        val id=parts[1].toIntOrNull()?.takeIf {it>0} ?: return null
        "tmdb:${parts[0]}:$id"
    }.getOrNull()
}
