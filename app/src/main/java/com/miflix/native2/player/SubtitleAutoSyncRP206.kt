package com.miflix.native2.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/** On-device, explicit-only audio energy capture. Never records or uploads audio bytes.
 * Capture works for decoded PCM; offloaded, passthrough, and DRM streams may be unsupported.
 */
internal class SubtitleAutoSyncRP206 {
    private val lock=Any()
    @Volatile var capturing=false
        private set
    @Volatile var playerPositionMs=0L
    private val buckets=linkedMapOf<Long,Double>()
    fun start(positionMs:Long) {
        synchronized(lock) {buckets.clear();playerPositionMs=positionMs;capturing=true}
    }
    fun stop():Map<Long,Double> = synchronized(lock) {
        capturing=false
        buckets.toMap()
    }
    fun add(sampleEnergy:Double) {
        if(!capturing || !sampleEnergy.isFinite()) return
        val key=playerPositionMs.coerceAtLeast(0L)/250L
        synchronized(lock) {
            if(!capturing) return
            buckets[key]=maxOf(buckets[key] ?: 0.0,sampleEnergy)
            while(buckets.size>240) buckets.remove(buckets.keys.first())
        }
    }
}

@UnstableApi
internal class SubtitleVoiceTapRP206(private val capture:SubtitleAutoSyncRP206):BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat:AudioProcessor.AudioFormat):AudioProcessor.AudioFormat=inputAudioFormat

    override fun queueInput(inputBuffer:ByteBuffer) {
        val length=inputBuffer.remaining()
        if(length<=0) return
        if(capture.capturing && inputAudioFormat.encoding==C.ENCODING_PCM_16BIT && length>=32) {
            val pcm=inputBuffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder())
            val stride=maxOf(2,(length/768/2)*2)
            var sum=0.0
            var count=0
            while(pcm.remaining()>=2) {
                val sample=pcm.short.toInt()
                sum+=sample.toDouble()*sample
                count++
                val skip=(stride-2).coerceAtMost(pcm.remaining())
                pcm.position(pcm.position()+skip)
            }
            if(count>0) capture.add(sqrt(sum/count)/32768.0)
        }
        val output=replaceOutputBuffer(length)
        output.put(inputBuffer)
        output.flip()
    }
}

internal data class SubtitleCueRP206(val startMs:Long,val endMs:Long)
internal data class SubtitleAlignmentRP206(val delayMs:Long,val confidence:Double,val matches:Int)

/** Supports normal SRT and WebVTT timestamps; limited to a capped number of cues. */
internal object SubtitleAutoSyncMathRP206 {
    private val arrow=Regex("""(\d{1,2}:)?\d{2}:\d{2}[.,]\d{3}\s*-->\s*(\d{1,2}:)?\d{2}:\d{2}[.,]\d{3}""")
    private val stamp=Regex("""(?:(\d{1,2}):)?(\d{2}):(\d{2})[.,](\d{3})""")
    fun parse(text:String):List<SubtitleCueRP206> {
        if(text.length>1_000_000) return emptyList()
        return arrow.findAll(text).take(2000).mapNotNull {match->
            val marks=stamp.findAll(match.value).toList()
            if(marks.size!=2) return@mapNotNull null
            fun ms(x:MatchResult):Long {
                val (hours,minutes,seconds,milliseconds)=x.destructured
                return ((hours.toLongOrNull() ?: 0L)*3600+(minutes.toLongOrNull() ?: 0L)*60+
                    (seconds.toLongOrNull() ?: 0L))*1000+(milliseconds.toLongOrNull() ?: 0L)
            }
            val start=ms(marks[0]);val end=ms(marks[1])
            if(end>start && end-start<=15_000L) SubtitleCueRP206(start,end) else null
        }.toList()
    }

    /**
     * Search small constant shifts by matching likely spoken segments to
     * audio-activity and silent windows. Silence is weighted heavily to
     * avoid blindly aligning subtitles with a loud soundtrack.
     * Deliberately conservative: often returns null instead of a risky shift.
     */
    fun estimate(cues:List<SubtitleCueRP206>,raw:Map<Long,Double>,currentDelayMs:Long):SubtitleAlignmentRP206? {
        if(cues.size<5 || raw.size<48) return null
        val energies=raw.values.filter {it>0.0001}
        if(energies.size<48) return null
        val sorted=energies.sorted()
        val cutoff=sorted[(sorted.size*.45).toInt().coerceIn(sorted.indices)].coerceAtLeast(.008)
        val voice=raw.mapValues {(_,value)->value>cutoff}
        val earliest=(raw.keys.minOrNull() ?: return null)*250L
        val latest=(raw.keys.maxOrNull() ?: return null)*250L
        if(latest-earliest<12_000L) return null
        val usable=cues.filter {it.endMs+currentDelayMs >=earliest-12_000 &&
            it.startMs+currentDelayMs<=latest+12_000}
        if(usable.size<5) return null
        val candidates=(-12_000L..12_000L step 250L).map {shift->
            val total=currentDelayMs+shift
            var positive=0;var negatives=0;var match=0;var correct=0
            for(cue in usable) {
                val duration=(cue.endMs-cue.startMs).coerceIn(300L,4000L)
                val mid=cue.startMs+duration/2+total
                val idx=mid/250
                val sample=voice[idx]
                if(sample!=null) {positive++;if(sample) {match++;correct++}}
                val before=voice[(cue.startMs+total-750L)/250L]
                if(before!=null) {negatives++;if(!before) correct++}
            }
            val measurements=positive+negatives
            Triple(shift,if(measurements>=12 && positive>=5 && negatives>=5)
                correct.toDouble()/measurements else -1.0,match)
        }.sortedByDescending {it.second}
        val best=candidates.firstOrNull() ?: return null
        val rivals=candidates.filter {abs(it.first-best.first)>=1500L}
        val second=rivals.maxOfOrNull {it.second} ?: 0.0
        val baseline=candidates.firstOrNull {it.first==0L }?.second ?: 0.0
        if(best.second<.78 || best.second-second<.10 || best.second-baseline<.10 ||
            best.third<5 || abs(best.first)<500) return null
        return SubtitleAlignmentRP206(currentDelayMs+best.first,best.second,best.third)
    }
}

/** Explicitly requested only: bounded subtitle download, never background polling. */
internal suspend fun fetchSubtitleTextRP206(url:String):String? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val parsed=runCatching {java.net.URL(url)}.getOrNull() ?: return@withContext null
        if(parsed.protocol!="https" || parsed.userInfo!=null ||
            parsed.port !in listOf(-1,443)) return@withContext null
        val connection=(parsed.openConnection() as? java.net.HttpURLConnection)
            ?: return@withContext null
        try {
            connection.connectTimeout=6500
            connection.readTimeout=6500
            connection.instanceFollowRedirects=false
            connection.setRequestProperty("Accept","text/plain,text/vtt,application/x-subrip")
            if(connection.responseCode !in 200..299 ||
                connection.contentLengthLong>800_000L) return@withContext null
            val bytes=connection.inputStream.use {input->
                val out=java.io.ByteArrayOutputStream()
                val buffer=ByteArray(8192)
                while(true) {
                    val n=input.read(buffer)
                    if(n<0) break
                    if(out.size()+n>800_000) return@withContext null
                    out.write(buffer,0,n)
                }
                out.toByteArray()
            }
            bytes.toString(Charsets.UTF_8).takeIf {it.isNotBlank()}
        } catch(_:Exception) {null}
        finally {connection.disconnect()}
    }
