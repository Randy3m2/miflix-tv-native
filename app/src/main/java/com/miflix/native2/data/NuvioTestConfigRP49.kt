package com.miflix.native2.data

import android.content.Context
import org.json.JSONArray

data class NuvioFolderRP49(val id:String,val title:String,val poster:String?,val catalogs:List<LiveCategory>)

/** Imported folder/catalog layout, separate from the user's existing sports source. */
object NuvioTestConfigRP49 {
    const val PROVIDER="nuvio:rp49"
    const val MANIFEST="retired:nuvio:rp49"
    @Volatile private var cached:List<NuvioFolderRP49>?=null
    fun folders(context:Context):List<NuvioFolderRP49> = emptyList() // Retired: no catalog asset or network access.
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
