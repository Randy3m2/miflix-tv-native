package com.miflix.native2.data

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

internal data class FinalEntryRP97(val id:String,val name:String,val url:String,val number:Int,val logo:String?,val category:String,val tvgId:String,val headers:Map<String,String>)
internal object FinalPlaylistRP97 {
    const val PREFIX="final:rp97:"
    fun nameKey(value:String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"),"")
    fun stationMatches(name:String,labels:List<String>):Boolean {
        val value=nameKey(name)
        return labels.any {label ->val alias=nameKey(label)
            alias.isNotBlank() && (value==alias || LidomUrlPolicy.matchesStation(name,label) || (alias.length>=6 && value.startsWith(alias)))
        }
    }
    fun withLidomBackups(rows:List<FinalEntryRP97>,backups:List<FinalEntryRP97>):List<FinalEntryRP97> {
        val missing=backups.filter {backup ->
            val labels=if(nameKey(backup.name).contains("digital15")) listOf("Digital 15") else listOf("Telemedios")
            rows.none {stationMatches(it.name,labels)}
        }
        val last=rows.maxOfOrNull {it.number} ?: 0
        return rows+missing.mapIndexed {index,row ->row.copy(number=last+index+1)}
    }
    private fun hash(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).take(12).joinToString("") {"%02x".format(it)}
    fun refreshUrl(source:String,stamp:Long):String = if(java.net.URI(source).host.equals("raw.githubusercontent.com",true))
        source+(if(source.contains('?')) "&" else "?")+"brunio_refresh="+stamp else source
    fun categoryId(name:String)="final-group:"+hash(name)
    fun parse(raw:String):List<FinalEntryRP97> {
        require(raw.toByteArray(Charsets.UTF_8).size<=4*1024*1024) {"M3U maximum: 4 MB"}
        require(raw.trimStart('\uFEFF',' ','\n','\r','\t').startsWith("#EXTM3U",true)) {"Invalid M3U header"}
        val rows=mutableListOf<FinalEntryRP97>();var attrs=emptyMap<String,String>();var name="";var headers=linkedMapOf<String,String>();var drm=false
        val attributes=Regex("([\\w-]+)=\"([^\"]*)\"")
        for(rawLine in raw.lineSequence()) {
            val line=rawLine.trim().removePrefix("\uFEFF")
            when {
                line.startsWith("#EXTINF:",true) -> {
                    attrs=attributes.findAll(line).associate {it.groupValues[1].lowercase(Locale.ROOT) to it.groupValues[2]}
                    var quoted=false
                    val comma=line.indexOfFirst {ch ->if(ch=='"') quoted=!quoted;ch==',' && !quoted}
                    name=if(comma>=0) line.substring(comma+1).trim() else attrs["tvg-name"].orEmpty()
                    headers=linkedMapOf()
                    attrs["http-user-agent"]?.let {headers["User-Agent"]=it}
                    attrs["http-referrer"]?.let {headers["Referer"]=it}
                    drm=false
                }
                line.startsWith("#EXTVLCOPT:http-user-agent=",true) -> headers["User-Agent"]=line.substringAfter('=')
                line.startsWith("#EXTVLCOPT:http-referrer=",true) -> headers["Referer"]=line.substringAfter('=')
                line.startsWith("#KODIPROP:",true) && line.contains("license",true) ->drm=true
                line.isNotBlank() && !line.startsWith('#') -> {
                    if(name.isNotBlank() && !drm && (line.startsWith("https://",true) || line.startsWith("http://",true))) {
                        val group=attrs["group-title"].orEmpty().ifBlank {"General"};val tvg=attrs["tvg-id"].orEmpty()
                        val logo=attrs["tvg-logo"]?.takeIf {it.startsWith("https://",true) || it.startsWith("http://",true)}
                        val id=PREFIX+hash(tvg+"|"+nameKey(name)+"|"+group)
                        rows.add(FinalEntryRP97(id,name,line,rows.size+1,logo,group,tvg,headers.toMap()))
                    }
                    name="";attrs=emptyMap();headers=linkedMapOf();drm=false
                }
            }
        }
        require(rows.isNotEmpty() && rows.size<=10_000) {"Empty M3U or more than 10000 channels"}
        return rows.distinctBy {it.id}
    }
}
