package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.MediaDetails
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

@Composable
internal fun CinemaBanner(kicker:String,title:String,subtitle:String) {
    Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xff352443),Color(0xff171721))),RoundedCornerShape(20.dp))
        .border(1.dp,Accent.copy(alpha=.25f),RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(kicker,color=Accent,fontSize=10.sp)
        Text(title,color=Color.White,fontSize=25.sp)
        Text(subtitle,color=Muted,fontSize=13.sp)
    }
}

@Composable
private fun MetricTile(label:String,value:String,modifier:Modifier=Modifier) {
    Column(modifier.background(Color(0xff24212f),RoundedCornerShape(18.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(value,color=Color.White,fontSize=27.sp)
        Text(label,color=Accent,fontSize=11.sp)
    }
}

@Composable
private fun NightFilmCard(item:MediaSummary,detail:MediaDetails,selected:Boolean,onVote:()->Unit,onSeen:(()->Unit)?) {
    Row(Modifier.fillMaxWidth().background(Color(0xff211e2a),RoundedCornerShape(18.dp)).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        AsyncImage(item.poster ?: item.backdrop,null,Modifier.width(76.dp).height(110.dp),contentScale=ContentScale.Crop)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(item.title,color=Color.White,fontSize=17.sp,maxLines=2)
            Text("${item.year} · ${detail.runtimeMinutes} min · ★ ${"%.1f".format(item.rating)}",color=Muted,fontSize=12.sp)
            FocusButton(if(selected) tr("♥ Mi voto","♥ My vote") else tr("Votar","Vote"),primary=selected,onClick=onVote)
            onSeen?.let {FocusButton(tr("Ya la vi · Excluir","Seen · Exclude"),horizontalPadding=12.dp,onClick=it)}
        }
    }
}

@Composable
internal fun CinemaHub(state:AppState,close:()->Unit) {
    var panel by remember {mutableStateOf("")}
    when(panel) {
        "stats" -> CinemaStatsPanel(state) {panel=""}
        "roulette" -> MovieNightPanel(state,close) {panel=""}
        "journeys" -> JourneyPanel(state,close) {panel=""}
        else -> PlayerSideSheet(tr("Mi universo de cine","My cinema hub"),close) {
            item {CinemaBanner("BRUNIO · YOUR EXPERIENCE",tr("Tu historia con el cine","Your story with cinema"),tr("Descubre tus patrones, comparte una noche y recorre tus sagas.","Explore your patterns, plan a movie night and journey through sagas."))}
            item {FeatureActionCard(tr("Mi vida cinematográfica","My cinema life"),tr("Generar reporte local","Generate an on-device report"),"◷") {panel="stats"}}
            item {FeatureActionCard(tr("Ruleta para una noche de cine","Movie night roulette"),tr("De 2 a 6 personas · votos privados por turnos","2–6 people · private turn-based votes"),"✧") {panel="roulette"}}
            item {FeatureActionCard(tr("Itinerarios de sagas","Saga journeys"),tr("Orden de estreno o de la historia","Release order or story order"),"↝") {panel="journeys"}}
        }
    }
}

@Composable
private fun StatBar(label:String,value:String,fraction:Float) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("$label  ·  $value",color=Color.White,fontSize=14.sp)
        Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xff302937))) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f,1f)).height(5.dp).background(Accent))
        }
    }
}

@Composable
private fun CinemaStatsPanel(state:AppState,close:()->Unit) {
    var period by remember {mutableStateOf("month")}
    var report by remember {mutableStateOf<JSONObject?>(null)}
    var friendInput by remember {mutableStateOf("")}
    var comparison by remember {mutableStateOf("")}
    var showRatings by remember {mutableStateOf(false)}
    val now=java.text.SimpleDateFormat("yyyy-MM",java.util.Locale.US).format(java.util.Date())
    val months=report?.keys()?.asSequence()?.sorted()?.filter {when(period) {"month" ->it==now;"year" ->it.startsWith(now.take(4));else ->true}}?.toList().orEmpty()
    val genres=linkedMapOf<String,Long>();val directors=linkedMapOf<String,Long>();val titles=linkedMapOf<String,JSONObject>()
    var total=0L
    months.forEach {month ->
        val m=report!!.getJSONObject(month);total+=m.optLong("ms")
        val rows=m.optJSONObject("titles") ?: JSONObject()
        rows.keys().forEach {id ->
            val row=rows.getJSONObject(id);titles[id]=row
            for(field in listOf("genres","directors")) {
                val a=row.optJSONArray(field) ?: continue
                for(i in 0 until a.length()) {
                    val name=a.optString(i);val map=if(field=="genres") genres else directors
                    map[name]=(map[name] ?: 0L)+row.optLong("ms")/a.length().coerceAtLeast(1)
                }
            }
        }
    }
    val mine=state.ratings.filterKeys {it in titles}
    PlayerSideSheet(tr("Mi vida cinematográfica","My cinema life"),close) {
        item {CinemaBanner("BRUNIO · REWIND",tr("Así ves el mundo","How you watch the world"),tr("Tu actividad, convertida en una historia visual.","Your viewing, turned into a visual story."))}
        item {Text(tr("Reporte manual. Horas medidas desde RC93 en este dispositivo; no reconstruye horas históricas a partir del progreso.","Manual report. Hours measured on this device since RC93; historical hours are not inferred from playback position."),color=Muted,fontSize=13.sp)}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("month" to tr("Mes","Month"),"year" to tr("Año","Year"),"all" to tr("Todo","All")).forEach {(id,label)->FocusButton(label,primary=period==id) {period=id}}
        }}
        item {FocusButton(tr("Generar / Actualizar reporte","Generate / Refresh report"),primary=true) {report=state.cinemaReport()}}
        if(report!=null) {
            item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                MetricTile(tr("TIEMPO REPRODUCIDO","WATCH TIME"),"${total/3600000}h ${(total/60000)%60}m",Modifier.weight(1f))
                MetricTile(tr("TÍTULOS REGISTRADOS","RECORDED TITLES"),titles.size.toString(),Modifier.weight(1f))
            }}
            item {Text("${titles.size} "+tr("títulos registrados","recorded titles")+" · "+tr("Tu nota media: ","Your average rating: ")+(if(mine.isEmpty()) "—" else "%.1f / 10".format(mine.values.average())),color=Muted)}
            item {Text(tr("Géneros favoritos","Favorite genres"),color=Color.White,fontSize=20.sp)}
            items(genres.entries.sortedByDescending {it.value}.take(5)) {e ->StatBar(e.key,"${e.value/60000} min",e.value.toFloat()/(genres.values.maxOrNull() ?: 1L).coerceAtLeast(1))}
            item {Text(tr("Directores recurrentes","Recurring directors"),color=Color.White,fontSize=20.sp)}
            items(directors.entries.sortedByDescending {it.value}.take(5)) {e ->StatBar(e.key,"${e.value/60000} min",e.value.toFloat()/(directors.values.maxOrNull() ?: 1L).coerceAtLeast(1))}
            item {Text(tr("Evolución mensual","Monthly viewing"),color=Color.White,fontSize=20.sp)}
            items(months) {month ->val ms=report!!.getJSONObject(month).optLong("ms");StatBar(month,"${ms/3600000} h ${(ms/60000)%60} min",ms.toFloat()/months.maxOf {report!!.getJSONObject(it).optLong("ms")}.coerceAtLeast(1))}
            val decades=mine.keys.groupBy {id ->titles[id]?.optString("year")?.take(3)?.plus("0s") ?: "—"}.mapValues {(_,ids)->ids.mapNotNull {mine[it]}.average()}.entries.sortedByDescending {it.value}
            if(decades.isNotEmpty()) item {Text(tr("Tus mejores notas por década: ","Your highest-rated decade: ")+decades.first().key+" · %.1f".format(decades.first().value),color=Accent)}
        }
        item {Text(tr("Comparar con un amigo · sin servidor","Compare with a friend · offline"),color=Color.White,fontSize=20.sp)}
        item {Text(tr("Intercambien sus notas por texto/QR y pega las suyas. No consulta ni publica datos de amigos automáticamente.","Exchange rating text/QR and paste theirs. No automatic friend-data reads or publishing."),color=Muted,fontSize=13.sp)}
        item {FocusButton(tr("Mostrar mis notas para compartir","Show my ratings to share")) {showRatings=!showRatings}}
        if(showRatings) item {val payload=JSONObject(state.ratings.entries.take(30).associate {it.key to it.value}).toString();QrCode(payload,180.dp);Text(payload,color=Muted,fontSize=11.sp)}
        item {NativeTextField(friendInput,{friendInput=it},tr("Notas de tu amigo (JSON)","Friend ratings (JSON)"),modifier=Modifier.fillMaxWidth())}
        item {FocusButton(tr("Comparar ahora","Compare now")) {
            comparison=runCatching {
                require(friendInput.length<=16000)
                val other=JSONObject(friendInput);val common=state.ratings.keys.filter {other.optInt(it) in 1..10}
                if(common.isEmpty()) tr("No hay notas en común.","No shared ratings.") else {
                    val gap=common.map {k ->kotlin.math.abs(state.ratings.getValue(k)-other.getInt(k))}.average()
                    "${common.size} "+tr("títulos en común · Diferencia media: ","shared titles · Average rating gap: ")+"%.1f / 10".format(gap)
                }
            }.getOrElse {tr("Texto de notas no válido.","Invalid ratings text.")}
        }}
        if(comparison.isNotBlank()) item {Text(comparison,color=Accent)}
    }
}

internal val NightGenres=listOf(28 to "Action",35 to "Comedy",18 to "Drama",27 to "Horror",10749 to "Romance",878 to "Sci-Fi",53 to "Thriller",16 to "Animation",99 to "Documentary",10751 to "Family")

@Composable
private fun MovieNightPanel(state:AppState,exit:()->Unit,back:()->Unit) {
    var count by remember {mutableIntStateOf(2)};var maxMinutes by remember {mutableIntStateOf(120)}
    var excludeSeen by remember {mutableStateOf(true)};var phase by remember {mutableStateOf("setup")}
    var person by remember {mutableIntStateOf(0)}
    var likes by remember {mutableStateOf(List(6) {emptySet<Int>()})};var vetoes by remember {mutableStateOf(List(6) {emptySet<Int>()})}
    var candidates by remember {mutableStateOf<List<Pair<MediaSummary,MediaDetails>>>(emptyList())}
    var votes by remember {mutableStateOf(List(6) {emptySet<String>()})};var excluded by remember {mutableStateOf(emptySet<String>())}
    var message by remember {mutableStateOf("")};var winner by remember {mutableStateOf<MediaSummary?>(null)}
    var spinLabel by remember {mutableStateOf("")}
    LaunchedEffect(phase) {
        if(phase=="loading") {
            try {
                val veto=vetoes.take(count).flatten().toSet()
                val loaded=mutableListOf<Pair<MediaSummary,MediaDetails>>()
                val pool=(state.movies+state.topRated+state.action+state.comedy+state.thrillers+state.sciFi+state.romancePicks).filter {it.type=="movie"}.distinctBy {it.cloudId}
                kotlinx.coroutines.withTimeoutOrNull(25_000) {
                for(item in pool.take(40)) {
                    if(excludeSeen && (state.progress[item.cloudId]?.percent ?: 0.0)>=80) continue
                    val detail=try {state.tmdb.details(item)} catch(e:CancellationException) {throw e} catch(_:Exception) {continue}
                    if(!com.miflix.native2.data.MovieNightPolicy.allowed(detail.runtimeMinutes,maxMinutes,detail.genreIds.toIntArray(),veto.toIntArray(),false,false)) continue
                    loaded.add(item to detail)
                    if(loaded.size>=12) break
                }
                }
                candidates=loaded.sortedWith(compareByDescending<Pair<MediaSummary,MediaDetails>> {(_,d)->likes.take(count).count {fav ->fav.any {it in d.genreIds}}}.thenByDescending {it.first.rating}).take(6)
                message=if(candidates.isEmpty()) tr("No hay opciones con estos filtros. Amplía la duración o quita vetos.","No matches. Increase runtime or relax vetoes.") else ""
                person=0;phase=if(candidates.isEmpty()) "setup" else "vote"
            } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo preparar la selección. Reintenta.","Could not prepare the shortlist. Retry.");phase="setup"}
        }
        if(phase=="spin") {
            val available=candidates.filter {it.first.cloudId !in excluded}
            if(available.isEmpty()) {message=tr("Todos fueron excluidos.","All titles were excluded.");phase="setup";return@LaunchedEffect}
            val scores=available.associate {it.first.cloudId to votes.take(count).count {v ->it.first.cloudId in v}}
            val indices=com.miflix.native2.data.MovieNightPolicy.tieIndices(available.map {scores[it.first.cloudId] ?: 0}.toIntArray(),BooleanArray(available.size))
            val tied=indices.map {available[it]}
            val chosen=tied.random().first
            repeat(if(tied.size>1) 18 else 3) {i ->spinLabel=tied.random().first.title;kotlinx.coroutines.delay(50L+i*10L)}
            winner=chosen;phase="result"
        }
    }
    PlayerSideSheet(tr("Ruleta · Noche de cine","Movie night roulette")+if(phase in setOf("preferences","vote")) " · ${person+1}/$count" else "",back,resetKey="$phase:$person") {
        if(message.isNotBlank()) item {Text(message,color=Accent)}
        when(phase) {
            "setup" -> {
                item {CinemaBanner("BRUNIO · MOVIE NIGHT",tr("Una película. Todos de acuerdo.","One movie. Everyone in."),tr("Gustos compartidos, vetos respetados y un poco de azar.","Shared tastes, respected vetoes and a little luck."))}
                item {Text(tr("Mismo dispositivo · preferencias y votos privados por turnos.","Same device · private preferences and votes, one person at a time."),color=Muted)}
                item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FocusButton("−") {count=(count-1).coerceAtLeast(2)};Text("$count "+tr("personas","people"),color=Color.White,modifier=Modifier.padding(top=12.dp));FocusButton("+") {count=(count+1).coerceAtMost(6)}}}
                item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf(90,120,150,180).forEach {n ->FocusButton("${n}m",primary=maxMinutes==n) {maxMinutes=n}}}}
                item {FocusButton(tr("Excluir vistas de este perfil: ","Exclude this profile's watched titles: ")+if(excludeSeen) "ON" else "OFF") {excludeSeen=!excludeSeen}}
                item {FocusButton(tr("Empezar preferencias privadas","Start private preferences"),primary=true) {person=0;phase="preferences";likes=List(6) {emptySet()};vetoes=List(6) {emptySet()};votes=List(6) {emptySet()};excluded=emptySet()}}
            }
            "preferences" -> {
                item {Text(tr("Turno de persona ","Person ")+"${person+1}/$count",color=Color.White,fontSize=22.sp)}
                item {Text(tr("Cada toque cambia: neutral → me gusta → veto. Los demás miran a otro lado.","Each press cycles: neutral → like → veto. Other people look away."),color=Muted)}
                items(NightGenres) {(id,label)->FocusButton((if(id in vetoes[person]) "✕ " else if(id in likes[person]) "♥ " else "○ ")+label) {
                    val l=likes.toMutableList();val v=vetoes.toMutableList()
                    when {id in v[person] ->v[person]=v[person]-id;id in l[person] ->{l[person]=l[person]-id;v[person]=v[person]+id};else ->l[person]=l[person]+id}
                    likes=l;vetoes=v
                }}
                item {FocusButton(tr("Guardar y pasar al siguiente","Lock and pass to next person"),primary=true) {if(person<count-1) person++ else phase="loading"}}
            }
            "loading" -> item {Text(tr("Preparando selección compartida…","Preparing shared picks…"),color=Muted)}
            "vote" -> {
                item {Text(tr("Voto privado de persona ","Private ballot for person ")+"${person+1}/$count",color=Color.White,fontSize=22.sp)}
                item {Text(tr("Elige todas las que te gusten. No se muestran votos anteriores.","Choose every title you would enjoy. Earlier votes stay hidden."),color=Muted)}
                items(candidates.filter {it.first.cloudId !in excluded},key={it.first.cloudId}) {(item,d)->
                    NightFilmCard(item,d,item.cloudId in votes[person],onVote={val copy=votes.toMutableList();copy[person]=if(item.cloudId in copy[person]) copy[person]-item.cloudId else copy[person]+item.cloudId;votes=copy},onSeen=if(excludeSeen) ({excluded=excluded+item.cloudId}) else null)
                }
                item {FocusButton(tr("Cerrar mi voto","Lock my ballot"),primary=true) {if(person<count-1) person++ else phase="spin"}}
            }
            "spin" -> item {Text("✧  $spinLabel",color=Accent,fontSize=24.sp)}
            "result" -> {
                item {winner?.let {chosen ->
                    Column(Modifier.fillMaxWidth().background(Color(0xff211b2d),RoundedCornerShape(20.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        AsyncImage(chosen.backdrop ?: chosen.poster,null,Modifier.fillMaxWidth().height(180.dp),contentScale=ContentScale.Crop)
                        Text(tr("LA ELEGIDA","TONIGHT'S PICK"),color=Accent,fontSize=10.sp)
                        Text(chosen.title,color=Color.White,fontSize=25.sp)
                    }
                }}
                item {Text(tr("Mayor número de votos; los empates se resuelven al azar.","Most votes wins; ties are resolved randomly."),color=Muted)}
                winner?.let {chosen ->
                    item {FocusButton(tr("Ver detalles","View details")) {exit();state.launch {state.open(chosen)}}}
                    item {FocusButton(tr("Reproducir","Watch title"),primary=true) {exit();state.launch {state.surpriseDiscovery(listOf(chosen))}}}
                }
                item {FocusButton(tr("Otra ronda","Another round")) {phase="setup"}}
            }
        }
    }
}
