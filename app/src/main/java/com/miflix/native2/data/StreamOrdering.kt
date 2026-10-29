package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import java.util.Locale

/** Audio preference first when Spanish is selected; resolution and size break ties. */
internal object StreamOrdering {
    private val fallbackLimit=com.miflix.native2.player.SourceFallbackBudget.MAX_ATTEMPTS
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
    fun languageRank(stream: StreamChoice): Int = StreamLanguagePriority.rank(
        stream.audioLanguages.joinToString(" "), "${stream.name}\n${stream.title}\n${stream.filename}")
    fun hasEnglishAudio(stream: StreamChoice): Boolean = StreamLanguagePriority.hasEnglishAudio(
        stream.audioLanguages.joinToString(" "),"${stream.name}\n${stream.title}\n${stream.filename}")
    fun nativeRP77(stream:StreamChoice)=NativeVodPolicyRP77.nativeEntry(stream.nativeSource!=null,stream.url,stream.sourceUrl)
    fun nativeAllowedRP77(stream:StreamChoice,preferredAudio:String,spanishOnly:Boolean)=!nativeRP77(stream) || NativeVodPolicyRP77.accepts(
        preferredAudio,spanishOnly,stream.audioLanguages.joinToString(" "),"${stream.name}\n${stream.title}\n${stream.filename}")
    private fun hasAudioRP62(stream:StreamChoice,language:String)=StreamLanguagePriority.hasAudio(
        stream.audioLanguages.joinToString(" "),"${stream.name}\n${stream.title}\n${stream.filename}",language)
    /** Provider-neutral automatic selection: language precedes resolution, size and add-on order. */
    fun best(streams: List<StreamChoice>,preferredAudio: String = "",bandwidthBps: Long=0,durationSeconds: Int=0,providerOrder:List<String> = emptyList(),secondaryAudio:String = "",nativeSizeAscending:Boolean=false,nativeSpanishOnly:Boolean=false): StreamChoice? =
        sorted(streams,preferredAudio,bandwidthBps,durationSeconds,providerOrder,secondaryAudio,nativeSizeAscending,nativeSpanishOnly).firstOrNull()
    fun fallbackCandidates(streams: List<StreamChoice>,preferredAudio: String,bandwidthBps: Long=0,durationSeconds: Int=0,providerOrder:List<String> = emptyList(),secondaryAudio:String = "",nativeSizeAscending:Boolean=false,nativeSpanishOnly:Boolean=false): List<StreamChoice> {
        val rows=sorted(streams,preferredAudio,bandwidthBps,durationSeconds,providerOrder,secondaryAudio,nativeSizeAscending,nativeSpanishOnly)
        if(secondaryAudio.isNotBlank()) {
            // Exhaust primary-language sources before secondary, then unknown releases.
            val primary=rows.filter {hasAudioRP62(it,preferredAudio)}.distinctBy {it.url}
            val secondary=rows.filter {!hasAudioRP62(it,preferredAudio) && hasAudioRP62(it,secondaryAudio)}.distinctBy {it.url}.take(fallbackLimit)
            val rest=rows.filter {it !in primary && it !in secondary}.distinctBy {it.url}.take((fallbackLimit-secondary.size).coerceAtLeast(0))
            return (primary+secondary+rest).distinctBy {it.url}
        }
        if(!StreamLanguagePriority.prefersSpanish(preferredAudio)) return rows.take(fallbackLimit)
        // Exhaust every returned Spanish source across providers before English.
        val spanish=rows.filter {languageRank(it)==0}.distinctBy {it.url}
        val english=rows.filter {languageRank(it)==1}.distinctBy {it.url}.take(fallbackLimit)
        val unknown=rows.filter {languageRank(it)==2}.distinctBy {it.url}.take((fallbackLimit-english.size).coerceAtLeast(0))
        return (spanish+english+unknown).distinctBy {it.url}
    }
    fun sorted(streams: List<StreamChoice>, preferredAudio: String = "",bandwidthBps: Long=0,durationSeconds: Int=0,providerOrder:List<String> = emptyList(),secondaryAudio:String = "",nativeSizeAscending:Boolean=false,nativeSpanishOnly:Boolean=false): List<StreamChoice> {
        val spanish = StreamLanguagePriority.prefersSpanish(preferredAudio)
        val english = StreamLanguagePriority.prefersEnglish(preferredAudio)
        val preferredOther = preferredAudio.isNotBlank() && !spanish && !english
        return streams.filter { playable(it) && nativeAllowedRP77(it,preferredAudio,nativeSpanishOnly) }
            .sortedWith(compareBy<StreamChoice> { if(secondaryAudio.isNotBlank()) {
                    when {hasAudioRP62(it,preferredAudio) ->0;hasAudioRP62(it,secondaryAudio) ->1;languageRank(it)==2 ->2;else ->3}
                } else if(spanish) languageRank(it) else if(english) {if(hasEnglishAudio(it)) 0 else if(languageRank(it)==2) 1 else 2}
                else if(preferredOther) {if(hasAudioRP62(it,preferredAudio)) 0 else if(languageRank(it)==2) 1 else 2} else 0 }
                .thenBy {if((spanish || StreamLanguagePriority.prefersSpanish(secondaryAudio)) && languageRank(it)==0) StreamLanguagePriority.spanishVariantRank(
                    it.audioLanguages.joinToString(" "),"${it.name}\n${it.title}\n${it.filename}") else 0}
                .thenBy {providerOrder.indexOf(it.addonName).let {index ->if(index<0) Int.MAX_VALUE else index}}
                .thenBy {if(bandwidthBps>0 && !AdaptiveSourcePolicy.fits(AdaptiveSourcePolicy.rate(bytes(it),durationSeconds,quality(it)),bandwidthBps)) 1 else 0}
                .thenBy {if(bandwidthBps>0 && !AdaptiveSourcePolicy.fits(AdaptiveSourcePolicy.rate(bytes(it),durationSeconds,quality(it)),bandwidthBps)) AdaptiveSourcePolicy.rate(bytes(it),durationSeconds,quality(it)) else 0L}
                .thenByDescending { quality(it) }.thenByDescending { NativeVodPolicyRP77.sizeKey(bytes(it),nativeSizeAscending && nativeRP77(it)) })
            .distinctBy { it.addonName to it.url }
    }
}
