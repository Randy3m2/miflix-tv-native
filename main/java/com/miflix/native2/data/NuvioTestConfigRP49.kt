package com.miflix.native2.data

import android.content.Context
import org.json.JSONArray

data class NuvioFolderRP49(val id:String,val title:String,val poster:String?,val catalogs:List<LiveCategory>)

/** Imported folder/catalog layout, separate from the user's existing sports source. */
object NuvioTestConfigRP49 {
    const val PROVIDER="nuvio:rp49"
    const val MANIFEST="https://nuviosports.xyz/eyJ0ZWFtcyI6IkZlcnJhcmkiLCJ0aW1lem9uZSI6IkFtZXJpY2EvU2FudG9fRG9taW5nbyIsImxhbmd1YWdlcyI6IkVuZ2xpc2gsIFNwYW5pc2giLCJ3ZWJTdHJlYW1zIjoiZGlzYWJsZWQiLCJzcG9ydHMiOiJmb290YmFsbCxiYXNrZXRiYWxsLGFtZXJpY2FuX2Zvb3RiYWxsLG1vdG9yc3BvcnQsaG9ja2V5LGJhc2ViYWxsLGNvbGxlZ2UsbW1hLHRlbm5pcyxkYXJ0cyxvdGhlciJ9/manifest.json?lg=English%2C+Spanish"
    @Volatile private var cached:List<NuvioFolderRP49>?=null
    fun folders(context:Context):List<NuvioFolderRP49> = cached ?: synchronized(this) {
        cached ?: context.assets.open("nuvio_test_rp49.json").bufferedReader().use {parse(it.readText())}.also {cached=it}
    }
    fun categories(context:Context)=folders(context).flatMap {folder ->folder.catalogs.map {it.copy(name=folder.title+" · "+it.name)}}
    fun parse(text:String):List<NuvioFolderRP49> {
        val root=JSONArray(text)
        return buildList {
            for(i in 0 until root.length()) {
                val folders=root.getJSONObject(i).optJSONArray("folders") ?: continue
                for(j in 0 until folders.length()) {
                    val folder=folders.getJSONObject(j)
                    val sources=folder.optJSONArray("catalogSources") ?: continue
                    val catalogs=buildList {
                        for(k in 0 until sources.length()) {
                            val source=sources.getJSONObject(k)
                            if(source.optString("manifestUrl")!=MANIFEST) continue
                            val id=source.optString("catalogId");val type=source.optString("type","tv")
                            if(id.isBlank() || type.isBlank()) continue
                            add(LiveCategory(type,id,source.optString("catalogName").ifBlank {id},paginated=true))
                        }
                    }.distinctBy {it.type+":"+it.id}
                    if(catalogs.isNotEmpty()) add(NuvioFolderRP49(folder.optString("id"),folder.optString("title","Sports"),folder.optString("coverImageUrl").takeIf {it.startsWith("https://")},catalogs))
                }
            }
        }.distinctBy {it.id}
    }
}
