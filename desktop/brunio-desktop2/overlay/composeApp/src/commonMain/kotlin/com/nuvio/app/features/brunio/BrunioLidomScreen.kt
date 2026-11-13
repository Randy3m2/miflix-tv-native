package com.nuvio.app.features.brunio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.trailer.TrailerPlaybackResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

private val inkLidom = Color(0xFF0B0F19)
private val mutedLidom = Color(0xFFADB9CC)
private val accentLidom = Color(0xFF4BD6EE)

internal data class BrunioLidomTeam(
    val id: String, val name: String, val handle: String, val mlb: Int, val tint: Color,
)
private val teamsLidom = listOf(
    BrunioLidomTeam("aguilas", "Águilas Cibaeñas", "https://www.youtube.com/@%C3%81guilasCibae%C3%B1asoficial", 667, Color(0xFFE6B52B)),
    BrunioLidomTeam("licey", "Tigres del Licey", "https://www.youtube.com/@TigresDelLicey1", 672, Color(0xFF327FE7)),
    BrunioLidomTeam("estrellas", "Estrellas Orientales", "https://www.youtube.com/@EstrellasBaseballClub", 669, Color(0xFF29A166)),
    BrunioLidomTeam("toros", "Toros del Este", "https://www.youtube.com/@torosdeleste", 668, Color(0xFFFC8A36)),
    BrunioLidomTeam("escogido", "Leones del Escogido · Abriendo Sports", "https://www.youtube.com/@abriendosports", 671, Color(0xFFEB4352)),
    BrunioLidomTeam("gigantes", "Gigantes del Cibao", "https://www.youtube.com/@GigantesCibao", 670, Color(0xFFA85A83)),
)

private data class LIdomStand(val id: Int, val wins: Int, val losses: Int, val pct: String, val behind: String)
private data class LIdomLive(val videoId: String, val title: String)

private object BrunioLidomData {
    private val json = Json { ignoreUnknownKeys = true }
    private fun JsonObject.s(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.n(key: String): Int = (this[key] as? JsonPrimitive)?.intOrNull ?: 0

    suspend fun standings(): List<LIdomStand> = withContext(Dispatchers.IO) {
        val season = brunioLidomSeason()
        val body = json.parseToJsonElement(httpGetTextWithHeaders(
            "https://statsapi.mlb.com/api/v1/standings?leagueId=131&season=$season&standingsTypes=regularSeason",
            mapOf("Accept" to "application/json"),
        )) as? JsonObject ?: return@withContext emptyList()
        (body["records"] as? JsonArray).orEmpty().flatMap { group ->
            val rows = (group as? JsonObject)?.get("teamRecords") as? JsonArray
            rows.orEmpty().mapNotNull { raw ->
                val row = raw as? JsonObject ?: return@mapNotNull null
                val id = (row["team"] as? JsonObject)?.n("id") ?: return@mapNotNull null
                if (teamsLidom.none { it.mlb == id }) return@mapNotNull null
                LIdomStand(id, row.n("wins"), row.n("losses"),
                    row.s("winningPercentage") ?: "—", row.s("leagueGamesBack") ?: "—")
            }
        }.distinctBy { it.id }.sortedWith(compareByDescending<LIdomStand> { it.wins.toDouble() / (it.wins + it.losses).coerceAtLeast(1) }.thenByDescending { it.wins })
    }

    /**
     * YouTube /streams can omit an active broadcast (or use a different renderer).
     * Check the channel /live page first and require videoDetails.isLiveContent,
     * then check each stream renderer for a video-specific LIVE NOW marker.
     * Never treat an unrelated replay or channel-wide "LIVE" badge as a game.
     */
    suspend fun live(team: BrunioLidomTeam): LIdomLive? = withContext(Dispatchers.IO) {
        val headers = mapOf("Accept" to "text/html",
            "Accept-Language" to "es-DO,es;q=0.9,en;q=0.5")
        val watch = runCatching {
            httpGetTextWithHeaders(team.handle.trimEnd('/') + "/live", headers)
        }.getOrNull()
        if (watch != null) {
            val playerStart = watch.indexOf("ytInitialPlayerResponse")
            if (playerStart >= 0) {
                val player = watch.substring(playerStart).take(80000)
                val details = player.substringAfter("\"videoDetails\"", "")
                    .take(12000)
                val liveFlag = Regex(""""isLiveContent"\s*:\s*true""")
                val videoId = Regex(""""videoId"\s*:\s*"([A-Za-z0-9_-]{11})"""")
                    .find(details)?.groupValues?.getOrNull(1)
                val title = Regex(""""title"\s*:\s*"([^"]{3,160})"""")
                    .find(details)?.groupValues?.getOrNull(1) ?: team.name
                if (videoId != null && liveFlag.containsMatchIn(details)) {
                    return@withContext LIdomLive(videoId, title)
                }
            }
        }
        val streams = runCatching {
            httpGetTextWithHeaders(team.handle.trimEnd('/') + "/streams", headers)
        }.getOrNull() ?: return@withContext null
        val markers = listOf("\"videoRenderer\":", "\"gridVideoRenderer\":")
        for (marker in markers) {
            for (chunk in streams.split(marker).drop(1).take(60)) {
                val block = chunk.take(12000).substringBefore(marker)
                val liveNow = Regex(""""isLiveNow"\s*:\s*true""")
                    .containsMatchIn(block) ||
                    block.contains("BADGE_STYLE_TYPE_LIVE_NOW")
                if (!liveNow) continue
                val id = Regex(""""videoId"\s*:\s*"([A-Za-z0-9_-]{11})"""")
                    .find(block)?.groupValues?.getOrNull(1) ?: continue
                val title = Regex(""""text"\s*:\s*"([^"]{3,160})"""")
                    .find(block)?.groupValues?.getOrNull(1) ?: team.name
                return@withContext LIdomLive(id, title)
            }
        }
        null
    }
}

internal expect fun brunioLidomSeason(): Int
internal expect fun openBrunioLidomYouTube(url: String): Boolean

@Composable
internal fun BrunioLidomScreen(onBack: () -> Unit, onPlay: (String, String, String?) -> Unit) {
    var standings by remember { mutableStateOf(emptyList<LIdomStand>()) }
    var standBusy by remember { mutableStateOf(true) }
    var standError by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableStateOf(0) }
    var loadingTeam by remember { mutableStateOf<String?>(null) }
    var playingError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(revision) {
        standBusy = true
        standError = null
        try { standings = BrunioLidomData.standings() }
        catch (c: CancellationException) { throw c }
        catch (_: Exception) { standError = "No se pudieron consultar las posiciones de MLB Stats." }
        finally { standBusy = false }
    }
    BrunioReadableSurface {
        Column(Modifier.fillMaxSize().background(inkLidom)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onBack) { Text("Volver") }
                Column(Modifier.weight(1f)) {
                    Text("LIDOM · Béisbol Dominicano", color = Color.White,
                        style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Temporada actual · YouTube oficial de los equipos · MLB Stats",
                        color = mutedLidom)
                }
                OutlinedButton(onClick = { revision++ }) { Text("Actualizar") }
            }
            LazyColumn(Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(22.dp)) {
                item {
                    Text("Directos de los equipos",
                        Modifier.padding(horizontal = 32.dp), color = Color.White,
                        style = MaterialTheme.typography.headlineSmall)
                    Text("Solo se abre automáticamente un video identificado como EN DIRECTO por YouTube.",
                        Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
                        color = mutedLidom)
                }
                items(teamsLidom.chunked(3)) { line ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                        line.forEach { team ->
                            Column(Modifier.weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF151C2B))
                                .border(1.dp, team.tint.copy(alpha = .45f), RoundedCornerShape(20.dp))
                                .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                AsyncImage(
                                    model = brunioLidomLogoUri(team.id),
                                    contentDescription = "Logo de " + team.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(98.dp),
                                )
                                Text(team.name, color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Button(
                                    enabled = loadingTeam == null,
                                    onClick = {
                                        loadingTeam = team.id
                                        playingError = null
                                        scope.launch {
                                            try {
                                                val live = BrunioLidomData.live(team)
                                                if (live == null) {
                                                    playingError = "No se encontró un video marcado EN DIRECTO para ${team.name}. Puedes revisar su canal oficial."
                                                } else {
                                                    val playbackUrl = TrailerPlaybackResolver.resolveSingleUrlFromYouTubeUrl(
                                                        "https://www.youtube.com/watch?v=${live.videoId}")
                                                    if (playbackUrl.isNullOrBlank()) {
                                                        playingError = "YouTube no permitió la reproducción nativa de ${team.name}. Abre su canal oficial."
                                                    } else {
                                                        onPlay(live.title, playbackUrl, brunioLidomLogoUri(team.id))
                                                    }
                                                }
                                            } catch (cancel: CancellationException) { throw cancel }
                                            catch (_: Exception) {
                                                playingError = "No se pudo consultar el directo de ${team.name}. Prueba su canal oficial."
                                            } finally { loadingTeam = null }
                                        }
                                    },
                                ) { Text(if (loadingTeam == team.id) "Buscando LIVE…" else "▶ Ver EN DIRECTO") }
                                OutlinedButton(onClick = { openBrunioLidomYouTube(team.handle + "/live") }) {
                                    Text("Canal oficial")
                                }
                            }
                        }
                        repeat(3 - line.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (playingError != null) item {
                    Text(playingError.orEmpty(), Modifier.padding(horizontal = 32.dp),
                        color = Color(0xFFFFADB5))
                }
                item {
                    Text("Tabla de posiciones · Serie regular",
                        Modifier.padding(horizontal = 32.dp), color = Color.White,
                        style = MaterialTheme.typography.headlineSmall)
                    Text("Fuente: MLB Stats · Liga 131 · actualizado al pulsar Actualizar",
                        Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
                        color = mutedLidom)
                }
                if (standBusy) item {
                    Text("Consultando posiciones…", Modifier.padding(horizontal = 32.dp), color = mutedLidom)
                }
                if (standError != null) item {
                    Text(standError.orEmpty(), Modifier.padding(horizontal = 32.dp), color = Color(0xFFFFADB5))
                }
                if (!standBusy && standError == null && standings.isEmpty()) item {
                    Text("La fuente aún no publicó posiciones de esta temporada.",
                        Modifier.padding(horizontal = 32.dp), color = mutedLidom)
                }
                if (standings.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                            .background(Color(0xFF202D43), RoundedCornerShape(12.dp))
                            .padding(15.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("#", Modifier.width(30.dp), color = accentLidom)
                            Text("EQUIPO", Modifier.weight(1f), color = accentLidom)
                            Text("G", Modifier.width(44.dp), color = accentLidom)
                            Text("P", Modifier.width(44.dp), color = accentLidom)
                            Text("PCT", Modifier.width(62.dp), color = accentLidom)
                            Text("DIF", Modifier.width(62.dp), color = accentLidom)
                        }
                    }
                    items(standings) { record ->
                        val rank = standings.indexOf(record) + 1
                        val team = teamsLidom.firstOrNull { it.mlb == record.id }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                            .background(if (rank <= 4) Color(0xFF18293B) else Color(0xFF111824),
                                RoundedCornerShape(12.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(rank.toString(), Modifier.width(30.dp), color = Color.White)
                            AsyncImage(model = team?.let { brunioLidomLogoUri(it.id) },
                                contentDescription = null, modifier = Modifier.size(34.dp))
                            Text(team?.name ?: record.id.toString(),
                                Modifier.weight(1f), color = Color.White)
                            Text(record.wins.toString(), Modifier.width(44.dp), color = Color.White)
                            Text(record.losses.toString(), Modifier.width(44.dp), color = Color.White)
                            Text(record.pct, Modifier.width(62.dp), color = Color.White)
                            Text(record.behind, Modifier.width(62.dp), color = mutedLidom)
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}
