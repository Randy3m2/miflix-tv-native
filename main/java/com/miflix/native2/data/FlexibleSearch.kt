package com.miflix.native2.data

import com.miflix.native2.model.MediaSummary
import java.text.Normalizer
import java.util.Locale

/** Accent/punctuation-insensitive title matching, with small spelling errors and partial words. */
internal object FlexibleSearch {
    fun normalize(value: String): String = Normalizer.normalize(value,Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+")," ").trim().replace(Regex("\\s+")," ")
    private fun distance(a: String,b: String): Int {
        var beforePrevious: IntArray?=null
        var previous=IntArray(b.length+1) { it }
        for(i in a.indices) {
            val next=IntArray(b.length+1); next[0]=i+1
            for(j in b.indices) {
                next[j+1]=minOf(next[j]+1,previous[j+1]+1,previous[j]+if(a[i]==b[j]) 0 else 1)
                if(i>0 && j>0 && a[i]==b[j-1] && a[i-1]==b[j])
                    next[j+1]=minOf(next[j+1],beforePrevious!![j-1]+1)
            }
            beforePrevious=previous
            previous=next
        }
        return previous[b.length]
    }
    fun score(title: String,query: String): Int {
        val t=normalize(title).take(160);val q=normalize(query).take(120)
        if(q.isBlank()) return 0
        if(t==q) return 1000
        if(t.replace(" ","")==q.replace(" ","")) return 950
        if(t.contains(q)) return 850
        val stop=setOf("the","a","an","of","and","el","la","los","las","de","del","un","una","y")
        val words=q.split(' ').filter {it !in stop}.ifEmpty {q.split(' ')}
        val candidates=t.split(' ')
        val hits=words.count { word -> candidates.any { candidate ->
            candidate.startsWith(word) || word.startsWith(candidate) && candidate.length>=3 ||
            word.length>=4 && distance(word,candidate)<=if(word.length>=12) 3 else if(word.length>=7) 2 else 1
        } }
        if(hits==words.size) return 750
        if(q.length>=4 && distance(t,q)<=if(q.length>=12) 3 else if(q.length>=8) 2 else 1) return 700
        if(words.size>=3 && hits>=2 && hits*4>=words.size*3) return 660
        return if(hits>0) 300+300*hits/words.size else 0
    }
    fun rank(rows: List<MediaSummary>,query: String): List<MediaSummary> = rows.distinctBy { it.cloudId }
        .sortedWith(compareByDescending<MediaSummary> { score(it.title,query) }.thenByDescending { it.rating })
    fun variants(query: String): List<String> {
        val stop=setOf("the","a","an","of","and","el","la","los","las","de","del","un","una","y")
        val words=normalize(query).split(' ').filter {it.isNotBlank() && it !in stop}.sortedByDescending {it.length}
        if(words.isEmpty()) return emptyList()
        // Small, bounded provider fallback rather than fetching a large catalog.
        val normalized=normalize(query)
        val collapsed=normalized.replace(Regex("(\\p{L})\\1+"),"$1").takeIf {it!=normalized && it.length>=2}
        return listOfNotNull(collapsed,words.first().takeIf {words.size>1},words.first().take(4).takeIf {words.first().length>=6},
            words.getOrNull(1)?.takeIf {it.length>=4}).distinct().filter {it!=normalize(query)}.take(2)
    }
}
