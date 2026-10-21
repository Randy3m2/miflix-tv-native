package com.miflix.native2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
internal fun ProfileArtworkBackdropRP63(state:AppState) {
    val locale=state.tmdb.language
    var titles by remember(locale) {mutableStateOf(if(ProfileArtCacheRP56.language==locale) ProfileArtCacheRP56.titles else emptyList())}
    var index by remember(locale) {mutableIntStateOf(0)}
    val active=state.appForeground
    LaunchedEffect(locale,active) {
        if(!active) return@LaunchedEffect
        if(ProfileArtCacheRP56.language!=locale || System.currentTimeMillis()-ProfileArtCacheRP56.fetchedAt>=6*60*60_000L) {
            try {
                val loaded=state.tmdb.trending().filter {it.backdrop!=null}.distinctBy {it.cloudId}.take(10)
                if(loaded.isNotEmpty() && state.tmdb.language==locale) {
                    ProfileArtCacheRP56.language=locale;ProfileArtCacheRP56.titles=loaded;ProfileArtCacheRP56.fetchedAt=System.currentTimeMillis();titles=loaded
                }
            } catch(e:CancellationException) {throw e} catch(_:Exception) { }
        }
    }
    LaunchedEffect(active,titles) {if(active && titles.size>1) while(true) {delay(12_000);index=(index+1)%titles.size}}
    val current=titles.getOrNull(index%titles.size.coerceAtLeast(1))
    val drift=remember {Animatable(0f)}
    LaunchedEffect(current?.cloudId,active) {if(active) {drift.snapTo(0f);drift.animateTo(1f,tween(12_000,easing=LinearEasing))}}
    Box(Modifier.fillMaxSize().background(Color.Black).clipToBounds()) {
        Crossfade(current?.backdrop,animationSpec=tween(1100),label="manageArtworkRP63") {url ->
            if(url!=null) AsyncImage(url,null,Modifier.fillMaxSize().graphicsLayer {
                scaleX=1.065f;scaleY=1.065f;translationX=(drift.value-.5f)*size.width*.035f
            },contentScale=ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xED050505),Color(0x8A050505),Color(0x70050505)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66050505),Color.Transparent,Color(0xE6050505)))))
    }
}

@Composable
internal fun ModernProfilesScreenRP63(state:AppState,onBack:()->Unit) {
    var managing by remember {mutableStateOf(false)}
    var picking by remember {mutableStateOf(false)}
    var selectedId by remember {mutableStateOf(state.activeProfile.id)}
    var creating by remember {mutableStateOf(false)}
    var editingAvatar by remember {mutableStateOf<Profile?>(null)}
    var deleting by remember {mutableStateOf<Profile?>(null)}
    var chooseNewAvatar by remember {mutableStateOf(false)}
    var newName by remember {mutableStateOf("")}
    var newAvatar by remember {mutableStateOf("ai:astronaut")}
    var inherit by remember {mutableStateOf(true)}
    var uploadAfterCreate by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    val selected=state.profiles.firstOrNull {it.id==selectedId} ?: state.profiles.firstOrNull()
    var editName by remember(selected?.id,selected?.name) {mutableStateOf(selected?.name.orEmpty())}
    if(!managing) {
        ProfilePickerRP56(state,picking,onSelect={p ->if(!picking) {picking=true;state.launch {try {state.selectProfile(p)} finally {picking=false}}}},
            onManage={managing=true;creating=true},onEdit={p ->selectedId=p.id;managing=true},onBack=onBack)
        return
    }
    BackHandler {if(creating) creating=false else managing=false}
    val first=remember {FocusRequester()}
    LaunchedEffect(managing,creating) {if(!creating) {withFrameNanos {};runCatching {first.requestFocus()}}}
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ProfileArtworkBackdropRP63(state)
        Column(Modifier.fillMaxSize().padding(horizontal=30.dp,vertical=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Administrar perfiles","Manage profiles"),color=Color.White,fontSize=31.sp,fontWeight=FontWeight.Bold)
                    Text(tr("Un espacio para cada quien.","A space for everyone."),color=Muted,fontSize=14.sp)
                }
                ModernButtonRP63(tr("Listo","Done"),action={managing=false})
            }
            Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(26.dp)) {
                LazyVerticalGrid(GridCells.Adaptive(122.dp),Modifier.weight(.57f).fillMaxHeight(),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(6.dp)) {
                    items(state.profiles,key={it.id}) {p ->
                        var focused by remember(p.id) {mutableStateOf(false)}
                        Column(Modifier.then(if(p.id==state.profiles.firstOrNull()?.id) Modifier.focusRequester(first) else Modifier)
                            .onFocusChanged {focused=it.isFocused;if(it.isFocused) selectedId=p.id}.focusable().tvClick {selectedId=p.id}.clickable {selectedId=p.id}
                            .clip(RoundedCornerShape(16.dp)).background(if(selectedId==p.id) Color(0xB31D1C23) else Color(0x66050505))
                            .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else if(selectedId==p.id) Accent else ModernBorderRP63,RoundedCornerShape(16.dp)).padding(12.dp),
                            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            ProfileAvatar(p,Modifier.size(92.dp))
                            Text(p.name,color=Color.White,fontSize=17.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text(tr("Editar perfil","Edit profile"),color=Muted,fontSize=11.sp)
                        }
                    }
                    item {
                        ModernButtonRP63("+  "+tr("Agregar perfil","Add profile"),Modifier.fillMaxWidth().heightIn(min=130.dp),action={creating=true})
                    }
                }
                Column(Modifier.weight(.43f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Color(0xF0101114))
                    .border(1.dp,ModernBorderRP63,RoundedCornerShape(20.dp)).verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    selected?.let {p ->
                        Text(tr("Editar perfil","Edit profile"),color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            ProfileAvatar(p,Modifier.size(70.dp))
                            Text(p.name,color=Color.White,fontSize=21.sp,maxLines=2)
                        }
                        NativeTextField(editName,{editName=it},tr("Nombre del perfil","Profile name"),modifier=Modifier.fillMaxWidth())
                        ModernButtonRP63(tr("Guardar nombre","Save name"),Modifier.fillMaxWidth(),primary=true,enabled=!busy && editName.isNotBlank(),action={busy=true;state.launch {try {state.socialAction {state.renameProfile(p,editName)}} finally {busy=false}}})
                        ModernButtonRP63(tr("Elegir avatar","Choose avatar"),Modifier.fillMaxWidth(),enabled=!busy,action={editingAvatar=p})
                        ModernButtonRP63(tr("Subir foto desde el celular","Upload photo from phone"),Modifier.fillMaxWidth(),enabled=!busy,action={state.launch {state.socialAction {state.startAvatarUpload(p)}}})
                        if(state.profiles.size>1 && !p.isKids && p.id!=com.miflix.native2.data.KidsContentPolicy.PROFILE_ID)
                            ModernButtonRP63(tr("Eliminar perfil","Delete profile"),Modifier.fillMaxWidth(),destructive=true,enabled=!busy,action={deleting=p})
                        state.error?.let {Text(it,color=Color(0xFFFFA1A7),fontSize=12.sp)}
                    }
                }
            }
        }
    }
    if(creating) {
        val nameFocus=remember {FocusRequester()}
        LaunchedEffect(Unit) {withFrameNanos {};runCatching {nameFocus.requestFocus()}}
        ModernDialogRP63(close={if(!busy) creating=false},width=.57f) {
            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Agregar perfil","Add profile"),color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Bold)
                        Text(tr("Hazlo tuyo.","Make it yours."),color=Muted,fontSize=14.sp)
                    }
                    ModernButtonRP63("×",enabled=!busy,action={creating=false})
                }
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    ProfileAvatar(Profile("new",newName,newAvatar),Modifier.size(76.dp))
                    ModernButtonRP63(tr("Elegir avatar","Choose avatar"),Modifier.weight(1f),enabled=!busy,action={chooseNewAvatar=true})
                }
                NativeTextField(newName,{newName=it},tr("Nombre del perfil","Profile name"),modifier=Modifier.fillMaxWidth().focusRequester(nameFocus))
                ModernButtonRP63(if(uploadAfterCreate) "✓  "+tr("Foto desde el celular seleccionada","Photo from phone selected") else tr("Subir foto desde el celular","Upload photo from phone"),Modifier.fillMaxWidth(),enabled=!busy,action={uploadAfterCreate=!uploadAfterCreate})
                if(uploadAfterCreate) Text(tr("El QR se abrirá al crear el perfil.","The QR opens after the profile is created."),color=Muted,fontSize=12.sp)
                ModernSettingsGroupRP63(tr("Usar add-ons del perfil principal","Use main profile add-ons")) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text(if(inherit) tr("Empieza con las mismas fuentes.","Start with the same sources.") else tr("Fuentes propias, más las de fábrica.","Own sources, plus bundled defaults."),color=Muted,fontSize=12.sp,modifier=Modifier.weight(1f))
                        ModernButtonRP63(if(inherit) tr("✓ Activado","✓ On") else tr("Desactivado","Off"),primary=inherit,enabled=!busy,action={inherit=!inherit})
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ModernButtonRP63(if(busy) tr("Creando…","Creating…") else if(uploadAfterCreate) tr("Crear y subir foto","Create & upload photo") else tr("Crear perfil","Create profile"),Modifier.weight(1f),primary=true,enabled=!busy && newName.isNotBlank(),action={
                        busy=true
                        val existing=state.profiles.map {it.id}.toSet();val owner=state.session?.userId
                        state.launch {try {state.socialAction {
                            state.createProfile(newName,newAvatar,inherit)
                            val created=state.profiles.firstOrNull {it.id !in existing}
                            if(created!=null && state.session?.userId==owner) {
                                creating=false;selectedId=created.id
                                if(uploadAfterCreate) state.startAvatarUpload(created)
                                newName="";newAvatar="ai:astronaut";inherit=true;uploadAfterCreate=false
                            }
                        }} finally {busy=false}}
                    })
                    ModernButtonRP63(tr("Cancelar","Cancel"),enabled=!busy,action={creating=false})
                }
                state.error?.let {Text(it,color=Color(0xFFFFA1A7),fontSize=12.sp)}
            }
        }
    }
    if(chooseNewAvatar) AvatarPickerDialog(newAvatar,hidden=state.hiddenAvatars,remove=state::hideAvatar,restore=state::restoreAvatars,choose={newAvatar=it;chooseNewAvatar=false},close={chooseNewAvatar=false})
    editingAvatar?.let {p ->AvatarPickerDialog(p.avatarValue,hidden=state.hiddenAvatars,remove=state::hideAvatar,restore=state::restoreAvatars,choose={avatar ->state.launch {state.socialAction {state.setProfileAvatar(p,avatar);editingAvatar=null}}},close={editingAvatar=null},customUpload={editingAvatar=null;state.launch {state.socialAction {state.startAvatarUpload(p)}}})}
    deleting?.let {p ->ModernDialogRP63(close={if(!busy) deleting=null},width=.50f) {
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(tr("Eliminar perfil","Delete profile"),color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold)
            Text(p.name,color=Color.White,fontSize=20.sp)
            Text(tr("Se eliminarán su progreso, favoritos y puntuaciones.","This deletes its progress, favorites and ratings."),color=Muted)
            ModernButtonRP63(tr("Cancelar","Cancel"),Modifier.fillMaxWidth(),primary=true,enabled=!busy,action={deleting=null})
            ModernButtonRP63(tr("Eliminar perfil","Delete profile"),Modifier.fillMaxWidth(),destructive=true,enabled=!busy,action={busy=true;state.launch {try {state.socialAction {state.deleteProfile(p);deleting=null}} finally {busy=false}}})
        }
    }}
}
