package com.miflix.native2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.SAXParserFactory

data class EpgProgramme(val channel: String,val start: Long,val stop: Long,val title: String,val description: String)
data class EpgGuide(val channels: Map<String,String> = emptyMap(),val programmes: Map<String,List<EpgProgramme>> = emptyMap()) {
    fun nowNext(id: String,now: Long): Pair<EpgProgramme?,EpgProgramme?> {
        val list=programmes[id].orEmpty()
        val current=list.firstOrNull { now>=it.start && now<it.stop }
        return current to list.firstOrNull { it.start>= (current?.stop ?: now) }
    }
    fun match(channel: LiveChannel,manual: String?): String? {
        if(manual!=null) return manual.takeIf { it in channels }
        // Preserve country suffixes. Never guess an Argentine schedule for another country's signal.
        return listOf(channel.tvgId,channel.id).map { it.substringBefore('@') }.firstOrNull { it in channels }
    }
}
object EpgParser {
    fun time(raw: String): Long? = runCatching {
        val parts=raw.trim().split(Regex("\\s+"));val stamp=parts[0]
        val pattern=when(stamp.length) { 14 -> "yyyyMMddHHmmss";12 -> "yyyyMMddHHmm";else -> return null }
        val zone=parts.getOrNull(1) ?: "+0000"
        val parser=SimpleDateFormat("$pattern Z",Locale.US).apply { isLenient=false;timeZone=TimeZone.getTimeZone("UTC") }
        parser.parse("$stamp $zone")?.time
    }.getOrNull()
    fun parse(xml: String,now: Long=System.currentTimeMillis()): EpgGuide {
        val channels=linkedMapOf<String,String>();val programmes=mutableListOf<EpgProgramme>()
        require(!Regex("<!\\s*(DOCTYPE|ENTITY)",RegexOption.IGNORE_CASE).containsMatchIn(xml)) { "EPG: DTD/entities are not supported" }
        val factory=SAXParserFactory.newInstance().apply {
            // Expat on Android does not implement every Xerces feature. Reject declarations above too.
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities",false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities",false) }
        }
        var channelId="";var programmeId="";var start:Long?=null;var stop:Long?=null
        var field="";val value=StringBuilder();var title="";var description=""
        factory.newSAXParser().parse(InputSource(StringReader(xml)),object: DefaultHandler() {
            override fun resolveEntity(publicId: String?,systemId: String?): InputSource = throw org.xml.sax.SAXException("External entities disabled")
            override fun startElement(uri: String?,localName: String?,qName: String,attributes: Attributes) {
                when(qName) {
                    "channel" -> channelId=attributes.getValue("id").orEmpty()
                    "programme" -> { programmeId=attributes.getValue("channel").orEmpty();start=time(attributes.getValue("start").orEmpty());stop=time(attributes.getValue("stop").orEmpty());title="";description="" }
                    "display-name","title","desc" -> { field=qName;value.setLength(0) }
                }
            }
            override fun characters(ch: CharArray,offset: Int,length: Int) { if(field.isNotEmpty() && value.length<12000) value.append(ch,offset,length) }
            override fun endElement(uri: String?,localName: String?,qName: String) {
                when(qName) {
                    "display-name" -> if(channelId.isNotEmpty() && channelId !in channels) channels[channelId]=value.toString().trim()
                    "title" -> if(title.isEmpty()) title=value.toString().trim()
                    "desc" -> if(description.isEmpty()) description=value.toString().trim()
                    "channel" -> channelId=""
                    "programme" -> {
                        val a=start;val b=stop
                        if(a!=null && b!=null && b>a && b>now-86400000L && a<now+3*86400000L && title.isNotBlank()) programmes.add(EpgProgramme(programmeId,a,b,title,description))
                        programmeId=""
                    }
                }
                if(qName==field) { field="";value.setLength(0) }
            }
        })
        return EpgGuide(channels,programmes.groupBy { it.channel }.mapValues { it.value.distinctBy { p -> p.start to p.title }.sortedBy { p -> p.start } })
    }
}
object EpgRepository {
    const val DEFAULT_URL="https://www.open-epg.com/files/argentina.xml"
    private val lock=Mutex()
    private var url="";private var at=0L;private var cache=EpgGuide()
    suspend fun load(source: String,force: Boolean=false): EpgGuide=lock.withLock {
        require(source.startsWith("https://")) { "EPG: HTTPS URL required" }
        if(!force && source==url && System.currentTimeMillis()-at<6*3600000L) return@withLock cache
        val raw=SourceHttp.text(source,"EPG",limit=12_000_000)
        val parsed=withContext(Dispatchers.Default) { EpgParser.parse(raw) }
        check(parsed.channels.isNotEmpty()) { "EPG: no channels found" }
        cache=parsed;url=source;at=System.currentTimeMillis();parsed
    }
}
