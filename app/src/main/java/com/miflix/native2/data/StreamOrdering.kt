package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import java.util.Locale

/** Factory ordering across all installed stream add-ons: resolution, then known file size. */
internal object StreamOrdering {
    private val resolution=Regex("(?<!\\d)(4320|2160|1440|1080|720|576|480|360)(?:p)?(?!\\d)",RegexOption.IGNORE_CASE)
    private val size=Regex("(?<![\\d.])(\\d+(?:[.,]\\d+)?)\\s*(GiB|GB|MiB|MB|TiB|TB)\\b",RegexOption.IGNORE_CASE)
    fun quality(stream: StreamChoice): Int {
        val text="${stream.name} ${stream.title} ${stream.filename}".lowercase(Locale.ROOT)
        return maxOf(resolution.findAll(text).map { it.groupValues[1].toInt() }.maxOrNull() ?: 0, when { Regex("\\b8k\\b").containsMatchIn(text) -> 4320; Regex("\\b(4k|uhd)\\b").containsMatchIn(text) -> 2160; else -> 0 })
    }
    fun bytes(stream: StreamChoice): Long {
        if(stream.videoSize>0) return stream.videoSize
        val match=size.find(stream.title+" "+stream.name+" "+stream.filename) ?: return 0
        val value=match.groupValues[1].replace(',','.').toDoubleOrNull() ?: return 0
        val factor=when(match.groupValues[2].lowercase(Locale.ROOT)) { "tib" -> 1099511627776.0; "tb" -> 1000000000000.0; "gib" -> 1073741824.0; "gb" -> 1000000000.0; "mib" -> 1048576.0; else -> 1000000.0 }
        return (value*factor).toLong().coerceAtLeast(0)
    }
    fun playable(stream: StreamChoice): Boolean = !Regex("\\bTB[\\s\\p{P}\\p{S}\\p{M}]*Download\\b",RegexOption.IGNORE_CASE)
        .containsMatchIn(stream.name+" "+stream.title)
    fun sorted(streams: List<StreamChoice>): List<StreamChoice> = streams.filter { playable(it) }.distinctBy { it.url }
        .sortedWith(compareByDescending<StreamChoice> { quality(it) }.thenByDescending { bytes(it) })
}
