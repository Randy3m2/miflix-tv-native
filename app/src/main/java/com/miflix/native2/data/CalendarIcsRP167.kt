package com.miflix.native2.data

import android.content.*
import androidx.core.content.FileProvider
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

internal object CalendarIcsRP167 {
 data class Entry(val id:String,val title:String,val day:String,val starts:Long?,val description:String="")
 private fun escape(value:String)=value.replace("\\","\\\\").replace(";","\\;").replace(",","\\,").replace("\r\n","\n").replace("\r","\n").replace("\n","\\n")
 private val utc=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
 fun encode(owner:String,rows:List<Entry>,stamp:Long=System.currentTimeMillis()):String {
  val lines=mutableListOf("BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//BruniO//Calendar RP167//EN","CALSCALE:GREGORIAN","METHOD:PUBLISH","X-WR-CALNAME:BruniO","X-PUBLISHED-TTL:PT1H")
  rows.distinctBy {it.id}.forEach {e->
   val date=runCatching {LocalDate.parse(e.day)}.getOrNull() ?: return@forEach
   lines+="BEGIN:VEVENT";lines+="UID:${UUID.nameUUIDFromBytes((owner+":"+e.id).toByteArray())}@brunio";lines+="DTSTAMP:${utc.format(Instant.ofEpochMilli(stamp))}"
   if(e.starts!=null) {lines+="DTSTART:${utc.format(Instant.ofEpochMilli(e.starts))}";lines+="DTEND:${utc.format(Instant.ofEpochMilli(e.starts+60*60*1000L))}"}
   else {lines+="DTSTART;VALUE=DATE:${date.format(DateTimeFormatter.BASIC_ISO_DATE)}";lines+="DTEND;VALUE=DATE:${date.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)}"}
   lines+="SUMMARY:${escape(e.title)}";lines+="DESCRIPTION:${escape(e.description)}";lines+="END:VEVENT"
  }
  lines+="END:VCALENDAR"
  // RFC 5545 limits content lines to 75 UTF-8 octets, without splitting a code point.
  fun fold(line:String):String {val out=StringBuilder();var count=0;var at=0;while(at<line.length){val cp=line.codePointAt(at);val part=String(Character.toChars(cp));val size=part.toByteArray(Charsets.UTF_8).size;if(count+size>75){out.append("\r\n ");count=1};out.append(part);count+=size;at+=Character.charCount(cp)};return out.toString()}
  return lines.joinToString("\r\n",postfix="\r\n",transform=::fold)
 }
 fun localEntries(c:Context,owner:String)=CalendarStoreRP120.events(c,owner).map {Entry(it.key,it.title,it.day(),it.starts.takeIf {_ ->it.timed},it.league)}
 fun share(c:Context,ics:String) {
  val dir=java.io.File(c.cacheDir,"shares").apply {mkdirs()};val file=java.io.File(dir,"BruniO-calendar.ics");file.writeText(ics,Charsets.UTF_8)
  val uri=FileProvider.getUriForFile(c,c.packageName+".updates",file)
  val send=Intent(Intent.ACTION_SEND).setType("text/calendar").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply {clipData=ClipData.newUri(c.contentResolver,"BruniO Calendar",uri)}
  c.startActivity(Intent.createChooser(send,"BruniO · Calendar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
 }
}
