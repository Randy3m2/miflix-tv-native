package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice

internal object StreamFilters {
    private val year=Regex("(?<![\\p{L}\\d])((?:18|19|20)\\d{2})(?![\\p{L}\\d])")
    private val technicalSuffix=Regex("^\\s*(?:[xX×]\\s*\\d|(?:GB|GiB|MB|MiB|TB|TiB|kbps|fps)\\b)",RegexOption.IGNORE_CASE)
    private val technicalStart=Regex("(?<!\\d)(?:4320|2160|1440|1080|720|576|480|360)p?\\b|\\b(?:WEB[- .]?DL|BluRay|BDRip|REMUX|HDTV|HDRip|x26[45]|H[ .]?26[45]|Remaster(?:ed)?)\\b",RegexOption.IGNORE_CASE)
    fun releaseYear(text: String,movieTitle: String? = null): Int? {
        val raw=text.lineSequence().firstOrNull().orEmpty()
        // A number in the actual movie title (1917, 2001, Blade Runner 2049) is not its release year.
        val words=Regex("[\\p{L}\\d]+").findAll(movieTitle.orEmpty()).map { Regex.escape(it.value) }.toList()
        val prefix=words.takeIf { it.isNotEmpty() }?.joinToString("[\\W_]+")
        val title=if(prefix==null) raw else Regex("^\\s*"+prefix+"(?=[\\W_]|$)",RegexOption.IGNORE_CASE).replaceFirst(raw,"")
        val beforeTechnical=technicalStart.find(title)?.range?.first?.let { title.substring(0,it) } ?: title
        return year.findAll(beforeTechnical).filterNot { match -> technicalSuffix.containsMatchIn(beforeTechnical.substring(match.range.last+1)) }
            .map { it.groupValues[1].toInt() }.filter { it in 1888..2099 }.lastOrNull()
    }
    fun allowed(stream: StreamChoice,type: String,movieYear: String?,maxBytes: Long,movieTitle: String? = null): Boolean {
        val bytes=StreamOrdering.bytes(stream)
        // A strict size cap cannot include links whose size is unknown.
        if(maxBytes>0 && (bytes<=0 || bytes>maxBytes)) return false
        val expected=movieYear?.take(4)?.toIntOrNull()
        if(type=="movie" && expected!=null) {
            val actual=releaseYear(stream.filename,movieTitle) ?: releaseYear(stream.title,movieTitle)
            if(actual!=null && actual!=expected) return false
        }
        return StreamOrdering.playable(stream)
    }
}
