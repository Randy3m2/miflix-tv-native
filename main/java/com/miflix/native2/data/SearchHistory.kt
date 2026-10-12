package com.miflix.native2.data

internal object SearchHistory {
    fun remember(existing: List<String>, query: String): List<String> {
        val clean=query.trim().replace(Regex("\\s+")," ").take(120)
        if(clean.length<2) return existing.take(10)
        return (listOf(clean)+existing).distinctBy { it.lowercase(java.util.Locale.ROOT) }.take(10)
    }
}
