package com.miflix.native2.ui

import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*

internal object AssistantDiscoveryRP155 {
    suspend fun similar(state:AppState,command:AssistantCommandRP155,previous:List<AssistantPickRP150>,budget:Int):Pair<List<AssistantPickRP150>,String> {
        val named=command.target.takeIf {it.isNotBlank()}?.let {target->state.tmdb.search(target).firstOrNull {it.type in listOf("movie","series")}}
        if(command.target.isNotBlank() && named==null) return emptyList<AssistantPickRP150>() to tr("No encontré ese título. Prueba su nombre completo.","Title not found. Try its full name.")
        val seed=named ?: state.playerRequest?.item ?: previous.firstOrNull()?.item ?: state.selected
            ?: return emptyList<AssistantPickRP150>() to tr("Dime un título o abre su ficha primero.","Name a title or open its details first.")
        val q=DiscoveryMood.normalize(command.raw)
        val plan=AssistantPlanRP150.local(command.raw,budget).copy(query="")
        val base=state.tmdb.details(seed)
        val shorter=q.contains("mas corto") || q.contains("mas corta") || q.contains("shorter")
        if(shorter && base.runtimeMinutes<=0) return emptyList<AssistantPickRP150>() to tr("No conozco la duración de ese título. Indica un máximo de minutos.","That title has no known runtime. Specify a maximum number of minutes.")
        val max=if(shorter) minOf((base.runtimeMinutes-1).coerceAtLeast(1),plan.minutes.takeIf {it>0} ?: Int.MAX_VALUE) else plan.minutes
        val gate=Semaphore(3)
        val rows=coroutineScope {state.tmdb.recommendations(seed).take(15).map {item ->async {gate.withPermit {
            try {val d=state.tmdb.details(item);if(d.runtimeMinutes>0 && (max==0 || d.runtimeMinutes<=max) && (item.poster!=null || item.backdrop!=null)) AssistantPickRP150(item,d.runtimeMinutes) else null} catch(e:CancellationException) {throw e} catch(_:Exception) {null}
        }}}.awaitAll().filterNotNull()}
        return rows to tr("Similares a ","Similar to ")+seed.title+if(max>0) " · ≤ $max min" else ""
    }
    suspend fun compare(state:AppState,target:String,previous:List<AssistantPickRP150>):String {
        val pieces=target.replace(Regex("(?i)\\s*(?:sin spoilers|without spoilers|spoiler.free)\\s*"),"").split(Regex("(?i)\\s+(?:con|vs\\.?|versus|and|with|y)\\s+|\\|")).map {it.trim()}.filter {it.length>=2}
        val titles=if(pieces.size in 2..3) pieces.mapNotNull {name->state.tmdb.search(name).firstOrNull {it.type in listOf("movie","series")}} else previous.map {it.item}.take(3)
        if(titles.size<2) return tr("Necesito dos o tres títulos: Compara Interstellar con Inception.","I need two or three titles: Compare Interstellar with Inception.")
        val descriptions=mutableListOf<String>()
        for(item in titles.distinctBy {it.cloudId}) {
            val d=state.tmdb.details(item)
            descriptions+=item.title+" · "+item.year+"\n"+(if(d.runtimeMinutes>0) "${d.runtimeMinutes} min · " else "")+"★ ${"%.1f".format(item.rating)} · "+d.genres.take(3).joinToString(" / ")
        }
        return tr("Comparación sin spoilers\n\n","Spoiler-free comparison\n\n")+descriptions.joinToString("\n\n")
    }
}
