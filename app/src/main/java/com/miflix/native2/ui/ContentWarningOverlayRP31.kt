package com.miflix.native2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.ContentAdvisoriesRP31
import com.miflix.native2.data.Http
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

@Composable
internal fun ContentWarningOverlayRP31(state:AppState,request:PlayerRequest,started:Boolean,visible:Boolean,modifier:Modifier=Modifier) {
    val context=LocalContext.current.applicationContext
    var rows by remember(request.item.cloudId,request.season,request.episode) {mutableStateOf<List<ContentAdvisoriesRP31.Warning>>(emptyList())}
    var count by remember(request.item.cloudId,request.season,request.episode) {mutableIntStateOf(0)}
    var showGuide by remember(request.item.cloudId,request.season,request.episode) {mutableStateOf(false)}
    var attempted by remember(request.item.cloudId,request.season,request.episode) {mutableStateOf(false)}
    LaunchedEffect(started,state.contentWarningsRP31,request.item.cloudId,request.season,request.episode) {
        if(!started || request.live || !state.contentWarningsRP31 || attempted) return@LaunchedEffect
        attempted=true
        try {
            val found=withTimeoutOrNull(6500) {
                val imdb=(state.details?.takeIf {it.summary.cloudId==request.item.cloudId} ?: state.tmdb.details(request.item)).imdbId
                if(imdb==null || !Regex("tt[0-9]{5,12}").matches(imdb)) return@withTimeoutOrNull emptyList()
                val prefs=context.getSharedPreferences("brunio_content_guide_rp31",0)
                val stored=runCatching {JSONObject(prefs.getString(imdb,"{}").orEmpty())}.getOrDefault(JSONObject())
                val age=System.currentTimeMillis()-stored.optLong("at",0)
                val ttl=if(stored.optBoolean("unavailable")) 3_600_000L else 7*24*3_600_000L
                val cached=stored.optJSONObject("data")
                if(age in 0 until ttl && cached!=null) return@withTimeoutOrNull ContentAdvisoriesRP31.parse(cached)
                val data=try {JSONObject(Http.text("https://api.imdbapi.dev/titles/$imdb/parentsGuide",timeoutMs=5000).take(262144))} catch(e:CancellationException) {throw e} catch(_:Exception) {JSONObject()}
                val warnings=ContentAdvisoriesRP31.parse(data)
                val editor=prefs.edit()
                if(prefs.all.size>=60) prefs.all.keys.filter {it!=imdb}.take(20).forEach {editor.remove(it)}
                // Store only normalized categories/votes, never scene descriptions or profile data.
                val normalized=JSONObject().put("parentsGuide",org.json.JSONArray(warnings.map {w -> JSONObject().put("category",w.category)
                    .put("severityBreakdowns",org.json.JSONArray().put(JSONObject().put("severityLevel",w.severity).put("voteCount",1)))}))
                editor.putString(imdb,JSONObject().put("at",System.currentTimeMillis()).put("unavailable",warnings.isEmpty()).put("data",normalized).toString()).apply()
                warnings
            }.orEmpty()
            rows=found
            if(found.isNotEmpty()) {
                showGuide=true
                for(i in 1..found.size) {count=i;delay(260)}
                delay(3500);showGuide=false
            }
        } catch(e:CancellationException) {showGuide=false;throw e} catch(_:Exception) {showGuide=false}
    }
    AnimatedVisibility(showGuide && visible && state.contentWarningsRP31,enter=fadeIn(tween(180)),exit=fadeOut(tween(300)),modifier=modifier) {
        Row(Modifier.background(Color(0x99000000)).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(2.dp).height((rows.size*21+22).dp).background(Color.White))
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(if(request.item.type=="series") tr("Guía de la serie","Series guide") else tr("Guía de contenido","Content guide"),color=Color(0xFFCCCCCC),fontSize=10.sp)
                rows.forEachIndexed {index,w ->
                    AnimatedVisibility(index<count,enter=fadeIn(tween(160))+slideInVertically(tween(160)) {it/3}) {
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text(when(w.category) {
                                "PROFANITY"->tr("Lenguaje fuerte","Profanity")
                                "VIOLENCE"->tr("Violencia","Violence")
                                "SEXUAL_CONTENT"->tr("Sexo/desnudez","Sex/nudity")
                                "ALCOHOL_DRUGS"->tr("Alcohol/drogas","Alcohol/drugs")
                                else->tr("Escenas intensas","Frightening scenes")
                            },color=Color.White,fontSize=12.sp)
                            Text("· "+when(w.severity) {"mild"->tr("Leve","Mild");"moderate"->tr("Moderado","Moderate");else->tr("Intenso","Severe")},color=Color(0xFFBBBBBB),fontSize=12.sp)
                        }
                    }
                }
            }
        }
    }
}
