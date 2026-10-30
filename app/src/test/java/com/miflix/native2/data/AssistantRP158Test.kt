package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class AssistantRP158Test {
    @Test fun realRequestsRouteToDistinctTools() {
        val queries=mapOf("Abre mis favoritos de Live TV" to AssistantToolRP158.LIVE_FAVORITES,"Guarda esta escena como favorita" to AssistantToolRP158.BOOKMARK,"Llévame a cuando Goku se transforma por primera vez" to AssistantToolRP158.JUMP,"Quiero episodios relacionados con la historia de Itachi" to AssistantToolRP158.STORY_ORDER,"Quiero ver canales de documentales" to AssistantToolRP158.ZAP,"Cuando termine este episodio pon uno de Dragon Ball Super" to AssistantToolRP158.AUTOPLAY)
        queries.forEach {(text,kind)->assertEquals(text,kind,AssistantToolsRP158.parse(text)?.kind)}
        assertNull(AssistantToolsRP158.parse("Películas donde sale Tom Holland"))
        assertNull(AssistantToolsRP158.parse("Quiero una película intensa de acción"))
    }
    @Test fun homeMovesKeepRowsUniqueAndRespectTheAnchor() {
        val result=AssistantToolsRP158.homeOrder("Pon Continue Watching arriba y mueve Streaming debajo de los géneros",listOf("streaming","movies","continue","genres_home"),emptyList())
        assertEquals("continue",result.first());assertEquals(result.indexOf("genres_home")+1,result.indexOf("streaming"));assertEquals(result.size,result.distinct().size)
    }
    @Test fun temporaryHalloweenRuleExpiresWithoutChangingSavedOrder() {
        val saved=listOf("continue","streaming","genres_home")
        assertEquals("horror_movies",AssistantLocalRP158.seasonalOrder(saved,true,LocalDate.of(2026,10,31))[1])
        assertEquals(saved,AssistantLocalRP158.seasonalOrder(saved,true,LocalDate.of(2026,11,1)))
        assertEquals(saved,AssistantLocalRP158.seasonalOrder(saved,false,LocalDate.of(2026,10,31)))
    }
    @Test fun lastWeekIsACalendarWeekInTheDeviceZone() {
        val zone=ZoneId.of("America/Santo_Domingo")
        val range=AssistantToolsRP158.historyRange("Qué vi la semana pasada",LocalDate.of(2026,10,29),zone)
        assertEquals(LocalDate.of(2026,10,19),Instant.ofEpochMilli(range.first).atZone(zone).toLocalDate())
        assertEquals(LocalDate.of(2026,10,25),Instant.ofEpochMilli(range.last).atZone(zone).toLocalDate())
    }
    @Test fun generatedFranchiseOrderCannotInjectOrOmitTitles() {
        val ids=setOf("tmdb:movie:1","tmdb:series:2")
        assertEquals(2,AssistantToolsRP158.checkedOrder("""{"order":["tmdb:series:2","tmdb:movie:1"]}""",ids).size)
        assertThrows(IllegalArgumentException::class.java) {AssistantToolsRP158.checkedOrder("""{"order":["tmdb:movie:999"]}""",ids)}
        assertThrows(IllegalArgumentException::class.java) {AssistantToolsRP158.checkedOrder("""{"order":["tmdb:movie:1","tmdb:movie:1"]}""",ids)}
    }
    @Test fun bookmarkJumpRequiresContentEpisodeSourceAndDuration() {
        val b=AssistantBookmarkRP158("tmdb:series:1",2,4,10000,60000,"sha256","Goku primera transformación",0)
        assertTrue(AssistantLocalRP158.compatible(b,b.copy(position=20000)))
        assertFalse(AssistantLocalRP158.compatible(b,b.copy(source="other")))
        assertFalse(AssistantLocalRP158.compatible(b,b.copy(episode=5)))
        assertFalse(AssistantLocalRP158.compatible(b,b.copy(duration=90000)))
        assertEquals(listOf(b),AssistantToolsRP158.matchingMarks("Llévame a cuando Goku se transforma por primera vez",listOf(b)))
    }
}
