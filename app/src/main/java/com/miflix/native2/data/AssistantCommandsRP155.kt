package com.miflix.native2.data

import java.time.*
import java.time.temporal.TemporalAdjusters
import org.json.JSONObject

internal enum class AssistantActionRP155 { HELP, NAVIGATE, CHANNEL, GAME, STANDINGS, REMINDER, SIMILAR, REFINE, COMPARE, EXPLAIN, NIGHT, PLAYBACK, TOOL }
internal data class AssistantCommandRP155(val action:AssistantActionRP155,val target:String="",val date:String="",val time:String="",val raw:String="")
internal object AssistantCommandsRP155 {
    val destinations=linkedMapOf("home" to listOf("inicio","home"),"search" to listOf("buscador","buscar","search"),"live" to listOf("canales","tv en vivo","live tv"),"favorites" to listOf("favoritos","mi lista","lista","favorites","my list"),"calendar" to listOf("calendario","calendar"),"queue" to listOf("cola","queue"),"notifications" to listOf("notificaciones","avisos","notifications"),"settings" to listOf("ajustes","configuracion","settings"),"profiles" to listOf("perfiles","profiles"),"friends" to listOf("amigos","party","friends"),"franchises" to listOf("franquicias","sagas","franchises"),"downloads" to listOf("descargas","downloads"),"addons" to listOf("complementos","addons","add ons"),"language" to listOf("idioma","idiomas","language"),"playback" to listOf("reproduccion","playback"),"appearance" to listOf("apariencia","appearance"))
    val capabilities="""Available BruniO actions: TOOL target ANALYZE/LIVE_FAVORITES/BOOKMARK/JUMP/HOME/ZAP/MULTIVIEW/FRANCHISE/VISUAL/HISTORY/AUTOPLAY/TRAILERS/EVENTS/STORY_ORDER/LISTS for the matching app function (story/character episode lists use STORY_ORDER, never a scene timestamp); HELP; NAVIGATE target home/search/live/favorites/calendar/queue/notifications/settings/profiles/friends/franchises/downloads/addons/language/playback/appearance; CHANNEL target channel name; GAME target team name; STANDINGS target NBA/MLB/LIDOM/F1; REMINDER target user-provided title, date ISO yyyy-MM-dd or literal weekday, time HH:mm only if explicitly provided; SIMILAR target title or empty for current title; REFINE target natural filter request; COMPARE target two or three titles separated by | or empty for current results; EXPLAIN current recommendations; NIGHT movie-night selection; PLAYBACK target pause/resume/next/subtitles_es/subtitles_off/audio_es/previous_channel. Never invent a channel, stream, live-game status, episode timestamp, reminder time or app feature. Actions are proposals and must be run via the app controls."""
    fun normalize(text:String)=DiscoveryMood.normalize(text)
    fun parse(text:String):AssistantCommandRP155? {
        val q=normalize(text);if(q.length<3) return null
        fun command(a:AssistantActionRP155,t:String="")=AssistantCommandRP155(a,t,raw=text)
        val reminder=Regex("(?i)(?:pon(?:me)?|crea|haz|programa|agrega|set|create|add)?\\s*(?:un\\s+)?(?:recordatorio|reminder)(?:\\s+(?:para|de|for|to))?\\s+(.+)").find(text)
        if(reminder!=null) {
            var target=reminder.groupValues[1].trim()
            val date=Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b").find(target)?.value ?: Regex("(?i)\\b(?:lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo|hoy|ma[nñ]ana|monday|tuesday|wednesday|thursday|friday|saturday|sunday|today|tomorrow)\\b").find(target)?.value.orEmpty()
            val time=Regex("(?i)\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b|\\b(?:1[0-2]|[1-9])(?::[0-5]\\d)?\\s*[ap]\\.?m\\.?").find(target)?.value.orEmpty()
            if(date.isNotBlank()) target=target.substringBefore(date).trim().replace(Regex("(?i)\\s+(?:el|este|on|this)$"),"")
            else if(time.isNotBlank()) target=target.substringBefore(time).trim().replace(Regex("(?i)\\s+(?:a las|at)$"),"")
            return AssistantCommandRP155(AssistantActionRP155.REMINDER,target.take(120),date,time,text)
        }
        if(q.contains("posiciones") || q.contains("standings")) return command(AssistantActionRP155.STANDINGS,when {q.contains("lidom")->"LIDOM";q.contains("mlb")->"MLB";q.contains("f1")->"F1";else->"NBA"})
        if(q in listOf("ayuda","help","que puedes hacer","que sabes hacer","funciones de la app","app commands")) return command(AssistantActionRP155.HELP)
        val playback=mapOf("pause" to listOf("pausa","pausar","pause"),"resume" to listOf("continua reproduccion","reanuda","resume playback"),"next" to listOf("siguiente episodio","next episode"),"subtitles_es" to listOf("subtitulos en espanol","subtitles in spanish"),"subtitles_off" to listOf("quita los subtitulos","apaga subtitulos","subtitles off"),"audio_es" to listOf("audio en espanol","audio in spanish"),"previous_channel" to listOf("canal anterior","previous channel"))
        playback.entries.firstOrNull {(_,aliases)->aliases.any {q==it || q=="pon $it" || q=="pon los $it"}}?.let {return command(AssistantActionRP155.PLAYBACK,it.key)}
        if(q.startsWith("compara") || q.startsWith("compare")) return command(AssistantActionRP155.COMPARE,text.replaceFirst(Regex("(?i)^compara(?:me)?\\s*|^compare\\s*"),""))
        if(q.startsWith("por que") || q.startsWith("why this") || q.startsWith("explica")) return command(AssistantActionRP155.EXPLAIN)
        if(q.contains("como esta") || q.contains("like this") || q.contains("mas como") || q.contains("similar a") || q.startsWith("algo como") || q.startsWith("something like")) return command(AssistantActionRP155.SIMILAR,text.replaceFirst(Regex("(?i)^.*?(?:algo como|mas como|similar a|something like|more like)\\s*"),"").takeIf {it!=text && normalize(it) !in listOf("esta","this","esa","this one")}?.substringBefore(",") ?: "")
        if(q.startsWith("esas no") || q.startsWith("mas corta") || q.startsWith("mas oscuro") || q.startsWith("algo mas") || q.startsWith("not those") || q.startsWith("shorter") || q.startsWith("more recent")) return command(AssistantActionRP155.REFINE,text)
        if(q.contains("noche de cine") || q.contains("movie night") || q.contains("para el grupo") || q.contains("for the group")) return command(AssistantActionRP155.NIGHT,text)
        val opening=Regex("^(?:abre|abrir|muestra|muestrame|ve a|vete a|open|show|go to)\\s+(?:la |el |los |las |mi |mis |the |my )?(.+)$").find(q)
        if(opening!=null) destinations.entries.firstOrNull {(_,aliases)->aliases.any {it==opening.groupValues[1]}}?.let {return command(AssistantActionRP155.NAVIGATE,it.key)}
        val play=Regex("(?i)^(?:pon(?:me)?|reproduce|sintoniza|play|tune(?: to)?|watch|abre)\\s+(?:(?:el|la|the)\\s+)?(.+)$").find(text.trim())
        if(play!=null) {
            val part=play.groupValues[1].trim();val n=normalize(part)
            if(n.startsWith("juego ") || n.startsWith("partido ") || n.contains(" game") || n.startsWith("game ")) {
                val team=part.replace(Regex("(?i)^(?:juego|partido|game)\\s*(?:de los\\s+|del\\s+|de\\s+|los\\s+)?"),"").replace(Regex("(?i)\\s+(?:que esta.*|que est[aá].*|ahora.*|now.*|en vivo.*|live.*|game.*)$"),"").trim()
                return command(AssistantActionRP155.GAME,team)
            }
            if(opening==null || n.startsWith("canal ")) return command(AssistantActionRP155.CHANNEL,part.replaceFirst(Regex("(?i)^canal\\s+"),""))
        }
        return null
    }
    fun fromJson(j:JSONObject?,raw:String):AssistantCommandRP155? {
        if(j==null || j.optString("kind").isBlank() || j.optString("kind")=="DISCOVER") return null
        val a=runCatching {AssistantActionRP155.valueOf(j.getString("kind"))}.getOrNull() ?: return null
        val target=j.optString("target").take(200);val date=j.optString("date").take(30);val time=j.optString("time").take(12)
        if(target.contains("://") || target.any {it.isISOControl()}) return null
        if(a==AssistantActionRP155.TOOL && target !in AssistantToolRP158.entries.map {it.name}) return null
        if(a==AssistantActionRP155.NAVIGATE && target !in destinations) return null
        if(a==AssistantActionRP155.STANDINGS && target !in listOf("NBA","MLB","LIDOM","F1")) return null
        if(a==AssistantActionRP155.PLAYBACK && target !in listOf("pause","resume","next","subtitles_es","subtitles_off","audio_es","previous_channel")) return null
        if(a==AssistantActionRP155.REMINDER) {
            val explicitDate=Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b|(?i)\\b(?:lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo|hoy|ma[nñ]ana|monday|tuesday|wednesday|thursday|friday|saturday|sunday|today|tomorrow)\\b").find(raw)?.value.orEmpty()
            val explicitTime=Regex("(?i)\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b|\\b(?:1[0-2]|[1-9])(?::[0-5]\\d)?\\s*[ap]\\.?m\\.?").find(raw)?.value.orEmpty()
            return AssistantCommandRP155(a,target,explicitDate,explicitTime,raw)
        }
        return AssistantCommandRP155(a,target,date,time,raw)
    }
    fun reminderAt(date:String,time:String,now:ZonedDateTime):ZonedDateTime {
        val q=normalize(date);val weekdays=listOf(listOf("lunes","monday"),listOf("martes","tuesday"),listOf("miercoles","wednesday"),listOf("jueves","thursday"),listOf("viernes","friday"),listOf("sabado","saturday"),listOf("domingo","sunday"))
        val weekday=weekdays.indexOfFirst {q in it}.takeIf {it>=0}?.let {DayOfWeek.of(it+1)}
        val day=when {weekday!=null ->now.toLocalDate().with(TemporalAdjusters.nextOrSame(weekday));q in listOf("hoy","today")->now.toLocalDate();q in listOf("manana","tomorrow")->now.toLocalDate().plusDays(1);else->LocalDate.parse(date)}
        val raw=time.lowercase().replace(".","").replace(" ","")
        val clock=if(raw.endsWith("am") || raw.endsWith("pm")) {val afternoon=raw.endsWith("pm");val h=raw.dropLast(2).substringBefore(":").toInt();require(h in 1..12);LocalTime.of(h%12+if(afternoon) 12 else 0,raw.dropLast(2).substringAfter(":","0").toInt())} else {require(Regex("(?:[01]?\\d|2[0-3]):[0-5]\\d").matches(raw));LocalTime.parse(raw.padStart(5,'0'))}
        var at=ZonedDateTime.of(day,clock,now.zone)
        if(weekday!=null && !at.isAfter(now)) at=at.plusWeeks(1)
        require(at.isAfter(now)) {"Reminder must be in the future"}
        return at
    }
}
