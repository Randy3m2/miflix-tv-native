package com.miflix.native2.data
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject
import java.time.*

class AssistantCommandsRP155Test {
    @Test fun requestedAppCommandsResolveToWhitelistedActions() {
        val game=AssistantCommandsRP155.parse("Pon el juego de Lakers que está now")!!
        assertEquals(AssistantActionRP155.GAME,game.action);assertEquals("Lakers",game.target)
        assertEquals("LIDOM",AssistantCommandsRP155.parse("Abre la tabla de posiciones de lidom")!!.target)
        assertEquals("favorites",AssistantCommandsRP155.parse("Abre mi lista")!!.target)
        assertEquals("subtitles_es",AssistantCommandsRP155.parse("Pon subtítulos en español")!!.target)
        assertEquals("ESPN",AssistantCommandsRP155.parse("Pon el canal ESPN")!!.target)
    }
    @Test fun reminderRequiresAnExplicitHourAndPreservesTitle() {
        val command=AssistantCommandsRP155.parse("Pon un recordatorio para Vision Quest el viernes")!!
        assertEquals("Vision Quest",command.target);assertEquals("viernes",command.date);assertEquals("",command.time)
        val now=ZonedDateTime.of(2026,10,29,18,0,0,0,ZoneId.of("America/Santo_Domingo"))
        assertThrows(Exception::class.java) {AssistantCommandsRP155.reminderAt(command.date,command.time,now)}
        assertEquals("2026-10-30T20:00-04:00[America/Santo_Domingo]",AssistantCommandsRP155.reminderAt(command.date,"8 pm",now).toString())
    }
    @Test fun weekdayInPastMovesToNextWeekButExplicitPastDateIsRejected() {
        val now=ZonedDateTime.of(2026,10,30,21,0,0,0,ZoneId.of("America/Santo_Domingo"))
        assertEquals(LocalDate.of(2026,11,6),AssistantCommandsRP155.reminderAt("friday","20:00",now).toLocalDate())
        assertThrows(Exception::class.java) {AssistantCommandsRP155.reminderAt("2026-10-29","20:00",now)}
    }
    @Test fun modelCannotSupplyUnknownRoutesUrlsOrInventReminderHour() {
        assertNull(AssistantCommandsRP155.fromJson(JSONObject("""{"kind":"NAVIGATE","target":"DELETE_ACCOUNT"}"""),"request"))
        assertNull(AssistantCommandsRP155.fromJson(JSONObject("""{"kind":"CHANNEL","target":"https://example.test"}"""),"request"))
        val command=AssistantCommandsRP155.fromJson(JSONObject("""{"kind":"REMINDER","target":"Vision Quest","date":"2026-10-30","time":"20:00"}"""),"Avísame de Vision Quest el viernes")!!
        assertEquals("viernes",command.date);assertEquals("",command.time)
    }
    @Test fun normalMovieSearchDoesNotTriggerPlaybackOrCalendar() {
        assertNull(AssistantCommandsRP155.parse("Terror de los 90"))
        assertEquals(AssistantActionRP155.SIMILAR,AssistantCommandsRP155.parse("Algo como Interstellar, pero más corto")!!.action)
        assertEquals(AssistantActionRP155.REFINE,AssistantCommandsRP155.parse("Esas no, algo más reciente")!!.action)
        assertEquals(AssistantActionRP155.COMPARE,AssistantCommandsRP155.parse("Compara estas tres sin spoilers")!!.action)
    }
}
