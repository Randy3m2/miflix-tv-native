package com.miflix.native2.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

internal data class FavoriteChannel(val provider: String,val channel: LiveChannel)
internal object LiveLibrary {
    var revision by mutableIntStateOf(0)
    fun prefs(context: Context)=context.getSharedPreferences("brunio_live_library",Context.MODE_PRIVATE)
    private fun key(state: AppState)="favorites:${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    fun favorites(context: Context,state: AppState): List<FavoriteChannel> = runCatching {
        val array=JSONArray(prefs(context).getString(key(state),"[]"))
        (0 until array.length()).map { i -> val o=array.getJSONObject(i)
            FavoriteChannel(o.getString("provider"),LiveChannel(o.getString("id"),o.optString("type","tv"),o.getString("name"),o.optString("poster").takeIf { it.isNotBlank() },o.optString("description"),o.optInt("number"),o.optString("tvgId"))) }
    }.getOrDefault(emptyList())
    fun toggle(context: Context,state: AppState,provider: String,channel: LiveChannel) {
        val rows=favorites(context,state).toMutableList()
        if(!rows.removeAll { it.provider==provider && it.channel.id==channel.id }) rows.add(FavoriteChannel(provider,channel))
        val a=JSONArray();rows.forEach { (p,c) -> a.put(JSONObject().put("provider",p).put("id",c.id).put("type",c.type).put("name",c.name).put("poster",c.poster.orEmpty()).put("description",c.description).put("number",c.number).put("tvgId",c.tvgId)) }
        prefs(context).edit().putString(key(state),a.toString()).apply();revision++
    }
}
@Composable internal fun RegionTabs(provider: String,onSelect:(String)->Unit) {
    if(provider.startsWith("iptv")) LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        items(IptvRepository.sources.entries.toList()) { (id,value) -> FocusButton(value.first,primary=provider==id || provider=="iptv" && id=="iptv:do") { onSelect(id) } }
    }
}
@Composable internal fun rememberLiveGuide(): EpgGuide {
    val context=LocalContext.current
    val revision=LiveLibrary.revision
    var guide by remember { mutableStateOf(EpgGuide()) }
    val source=LiveLibrary.prefs(context).getString("epg_source",EpgRepository.DEFAULT_URL)!!
    LaunchedEffect(source,revision) {
        try { guide=EpgRepository.load(source) } catch(e: CancellationException) { throw e } catch(_: Exception) { guide=EpgGuide() }
    }
    return guide
}
@Composable internal fun ChannelSchedule(provider: String,channel: LiveChannel,guide: EpgGuide,color: Color=Muted) {
    val context=LocalContext.current;val revision=LiveLibrary.revision
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(channel.id) { while(true) { now=System.currentTimeMillis();delay(60000) } }
    val mapping=LiveLibrary.prefs(context).getString("epg:$provider:${channel.id}",null)
    val (current,next)=guide.nowNext(guide.match(channel,mapping).orEmpty(),now)
    Column {
        Text(tr("Ahora: ","Now: ")+(current?.title ?: tr("Sin guía","No guide")),color=color,fontSize=12.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        if(next!=null) Text(tr("Después: ","Next: ")+next.title,color=color,fontSize=12.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
@Composable internal fun ChannelExtras(state: AppState,provider: String,channel: LiveChannel,compact: Boolean=false) {
    val context=LocalContext.current;val revision=LiveLibrary.revision
    val favorite=remember(revision,provider,channel.id,state.session?.userId,state.activeProfile.id) { LiveLibrary.favorites(context,state).any { it.provider==provider && it.channel.id==channel.id } }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=(if(compact) Modifier else Modifier.fillMaxWidth()).padding(top=6.dp,bottom=8.dp)) {
        FocusButton(if(favorite) "★" else "☆",horizontalPadding=16.dp) { LiveLibrary.toggle(context,state,provider,channel) }
    }
}
@Composable internal fun LiveGuideDialog(provider: String="",channel: LiveChannel?=null,close:()->Unit) {
    val context=LocalContext.current;val prefs=LiveLibrary.prefs(context);val scope=rememberCoroutineScope()
    var draft by remember { mutableStateOf(prefs.getString("epg_source",EpgRepository.DEFAULT_URL)!!) }
    var message by remember { mutableStateOf("") };var busy by remember { mutableStateOf(false) }
    var mappingMode by remember { mutableStateOf(false) };var search by remember { mutableStateOf("") }
    val guide=rememberLiveGuide();val revision=LiveLibrary.revision
    val mapping=prefs.getString("epg:$provider:${channel?.id}",null)
    val selected=channel?.let { guide.match(it,mapping) }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth(.9f).heightIn(max=520.dp).background(Panel).padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(channel?.name ?: tr("Guía de canales","Channel guide"),color=Color.White,fontSize=24.sp)
            Text(tr("La fuente inicial cubre Argentina. Vincula solo la señal correcta.","The initial guide covers Argentina. Link only the matching signal."),color=Muted,fontSize=13.sp)
            if(channel==null) {
                NativeTextField(draft,{draft=it},"EPG · https://…/guide.xml",modifier=Modifier.fillMaxWidth())
                FocusButton(tr("Guardar y actualizar","Save and refresh")) { if(!busy) scope.launch {
                    busy=true;message=tr("Cargando guía…","Loading guide…")
                    try { EpgRepository.load(draft.trim(),true);prefs.edit().putString("epg_source",draft.trim()).apply();LiveLibrary.revision++;message=tr("Guía actualizada","Guide updated") }
                    catch(e: Exception) { if(e is CancellationException) throw e;message=e.message.orEmpty() } finally { busy=false }
                } }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                if(channel!=null) FocusButton(tr("Vincular señal","Link guide channel")) { mappingMode=!mappingMode }
                if(channel!=null && mapping!=null) FocusButton(tr("Quitar vínculo","Clear link")) { prefs.edit().remove("epg:$provider:${channel.id}").apply();LiveLibrary.revision++ }
                FocusButton(tr("Cerrar","Close"),primary=true,onClick=close)
            }
            if(message.isNotBlank()) Text(message,color=Color.White)
            if(mappingMode) NativeTextField(search,{search=it},tr("Buscar señal EPG","Search EPG channel"),modifier=Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=16.dp)) {
                if(mappingMode && channel!=null) {
                    items(guide.channels.entries.filter { IptvPlaylist.searchKey(it.key+" "+it.value).contains(IptvPlaylist.searchKey(search)) }) { entry ->
                        FocusButton("${entry.value} · ${entry.key}",modifier=Modifier.fillMaxWidth()) { prefs.edit().putString("epg:$provider:${channel.id}",entry.key).apply();LiveLibrary.revision++;mappingMode=false }
                    }
                } else {
                    val rows=guide.programmes[selected].orEmpty().filter { it.stop>System.currentTimeMillis() }.take(30)
                    if(channel!=null && rows.isEmpty()) item { Text(tr("Sin programación disponible para esta señal.","No schedule available for this signal."),color=Muted) }
                    items(rows) { p ->
                        Column { Text(android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(p.start))+" · "+p.title,color=Color.White);if(p.description.isNotBlank()) Text(p.description,color=Muted,maxLines=3) }
                    }
                }
            }
        }
    }
}
@Composable internal fun FavoriteChannelsDialog(state: AppState,select:(PlayerRequest)->Unit,close:()->Unit) {
    val context=LocalContext.current;val revision=LiveLibrary.revision
    val rows=remember(revision,state.session?.userId,state.activeProfile.id) { LiveLibrary.favorites(context,state) }
    var removalGate by remember { mutableStateOf<QuickReactionGestureGate?>(null) }
    val scope=rememberCoroutineScope();var busy by remember { mutableStateOf(false) };var message by remember { mutableStateOf("") }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.onPreviewKeyEvent { event -> removalGate?.consume(event.nativeKeyEvent.keyCode,event.type==KeyEventType.KeyDown,event.type==KeyEventType.KeyUp,event.nativeKeyEvent.repeatCount) ?: false }.fillMaxWidth(.9f).heightIn(max=530.dp).background(Panel).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Canales favoritos","Favorite channels"),color=Color.White,fontSize=24.sp)
            FocusButton(tr("Cerrar","Close"),onClick=close)
            if(rows.isEmpty()) Text(tr("Usa ☆ junto a un canal para guardarlo aquí.","Use ☆ beside a channel to save it here."),color=Muted)
            if(busy) Text(tr("Cargando…","Loading…"),color=Muted)
            if(message.isNotBlank()) Text(message,color=Color.White)
            LazyVerticalGrid(columns=GridCells.Adaptive(180.dp),modifier=Modifier.weight(1f,false),contentPadding=PaddingValues(bottom=20.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                gridItems(rows,key={it.provider+":"+it.channel.id}) { (provider,c) ->
                    Column {
                        LiveChannelTile(c,c.number,true,onLongClick={ removalGate=QuickReactionGestureGate();LiveLibrary.toggle(context,state,provider,c) },onClick= { if(!busy) scope.launch {
                            busy=true;message=""
                            try {
                                val stream=LiveRepository().streams(state.calendarLiveManifest(provider),c).firstOrNull() ?: error(tr("Canal no disponible","Channel unavailable"))
                                val item=MediaSummary(c.id.hashCode(),"live",c.name,c.description,null,c.poster,0.0,"LIVE")
                                select(PlayerRequest(item,stream,0,0,0,true,c.id,provider,liveType=c.type))
                            } catch(e: Exception) { if(e is CancellationException) throw e;message=e.message.orEmpty() } finally { busy=false }
                        } })
                    }
                }
            }
        }
    }
}
