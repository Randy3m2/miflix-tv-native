package com.miflix.native2.data

import java.text.Normalizer
import java.util.Locale

internal object NativeSourceMatching {
    fun normalize(value: String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"")
        .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\d]+")," ").trim().replace(Regex("\\s+")," ")
    fun titleMatches(release: String,titles: List<String>): Boolean {
        val clean=normalize(release.replace(Regex("^\\s*\\[[^]]+]\\s*"),""))
        return titles.map(::normalize).filter { it.isNotBlank() }.any { clean==it || clean.startsWith("$it ") }
    }
    fun episodeMatches(name: String,season: Int,episode: Int): Boolean {
        val base=name.substringAfterLast('/').substringAfterLast('\\')
        val pairs=Regex("(?i)(?<![a-z0-9])s(\\d{1,2})[ ._-]*e(\\d{1,3})(?!\\d)|(?<!\\d)(\\d{1,2})x(\\d{1,3})(?!\\d)").findAll(base).toList()
        if(pairs.size!=1 || Regex("(?i)e\\d{1,3}[- ._]*e\\d|e\\d{1,3}-\\d{1,3}").containsMatchIn(base)) return false
        val p=pairs.single().groupValues
        return (p[1].ifBlank { p[3] }).toInt()==season && (p[2].ifBlank { p[4] }).toInt()==episode
    }
    fun video(name: String): Boolean {
        val base=name.substringAfterLast('/').substringAfterLast('\\')
        return Regex("(?i)\\.(mkv|mp4|m4v|avi|mov|ts|m2ts|webm)$").containsMatchIn(base) &&
            !Regex("(?i)(^|[ ._\\-])(sample|trailer|featurette)([ ._\\-]|$)").containsMatchIn(base)
    }
}
