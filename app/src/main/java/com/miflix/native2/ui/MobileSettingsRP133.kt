package com.miflix.native2.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.BuildConfig
import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal object MobileUiPrefsRP133 {
    var scale by mutableFloatStateOf(1f)
    fun load(context:android.content.Context,owner:String) {scale=context.getSharedPreferences("brunio_mobile",0).getFloat("font:$owner",1f).coerceIn(1f,1.25f)}
    fun save(context:android.content.Context,owner:String,value:Float) {scale=value;context.getSharedPreferences("brunio_mobile",0).edit().putFloat("font:$owner",value).apply()}
}

@Composable
private fun MobileSettingRowRP133(title:String,value:String,click:()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=52.dp).clickable(onClick=click).padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,color=Color.White,fontSize=15.sp,modifier=Modifier.weight(1f))
        Text(value,color=MobileTintRP133,fontSize=12.sp,maxLines=2,modifier=Modifier.widthIn(max=125.dp))
        MobileIconRP133("chevron",Modifier.size(16.dp))
    }
}

@Composable
private fun MobileSettingsCardRP133(title:String,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().mobileGlassRP133().padding(15.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(title,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(bottom=7.dp));content()
    }
}

@Composable
internal fun MobileSettingsRP133(state:AppState) {
    val context=LocalContext.current
    var sheet by remember {mutableStateOf<String?>(null)}
    var email by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var key by remember {mutableStateOf("")}
    var update by remember {mutableStateOf(false)}
    val langs=listOf("es" to "Español latino","en" to "English","ko" to "한국어 · Korean","pt" to "Português","fr" to "Français")
    fun language(code:String)=langs.firstOrNull {it.first==code}?.second ?: tr("Automático","Auto")
    if(update) MobileUpdateRP133(state) {update=false}
    if(sheet=="addons") MobileAddonsRP133(state) {sheet=null}
    if(sheet in listOf("interface","audio","secondary","subtitles","size","text")) MobileSheetRP133(when(sheet) {"size"->tr("Límite por archivo","File size limit");"text"->tr("Tamaño del texto","Text size");else->tr("Idioma","Language")},{sheet=null}) {
        val choices=when(sheet) {
            "interface"->listOf("es" to "Español","en" to "English")
            "secondary"->listOf("" to tr("Desactivado","Off"))+langs
            "subtitles"->listOf("off" to tr("Desactivados","Off"))+langs
            "size"->listOf("0" to tr("Sin límite","Unlimited"),"1" to "1 GB","2" to "2 GB","4" to "4 GB","8" to "8 GB","15" to "15 GB","30" to "30 GB")
            "text"->listOf("1" to tr("Normal","Normal"),"1.15" to tr("Grande","Large"),"1.25" to tr("Muy grande","Extra large"))
            else->listOf("" to tr("Automático","Auto"))+langs
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {items(choices) {(value,label)->MobileButtonRP133(label,modifier=Modifier.fillMaxWidth()) {
            when(sheet) {
                "interface"->state.launch {state.setInterfaceLanguage(value)}
                "audio"->state.savePlaybackPreferences(value,state.subtitleLanguage,state.autoplayNext)
                "secondary"->state.saveSecondaryAudioRP62(value)
                "subtitles"->state.savePlaybackPreferences(state.audioLanguage,value,state.autoplayNext)
                "size"->state.saveStreamSizeLimit(value.toDouble())
                "text"->MobileUiPrefsRP133.save(context,state.localExtrasScope(),value.toFloat())
            };sheet=null
        }}}
    }
    if(sheet=="torbox") MobileSheetRP133("BruniO + TorBox",{sheet=null;key=""}) {
        Text(tr("La clave se guarda con tu configuración existente. No la compartas.","Your key uses the existing secure settings flow. Do not share it."),color=Muted,fontSize=13.sp)
        MobileFieldRP133(key,{key=it},"API key",password=true)
        MobileButtonRP133(tr("Guardar y activar","Save and enable"),"check",Modifier.fillMaxWidth(),enabled=key.isNotBlank()) {val value=key;key="";sheet=null;state.launch {state.saveNativeSourceKey(value,true,false)}}
        MobileButtonRP133(if(state.nativeSourceEnabled) tr("Desactivar","Disable") else tr("Activar","Enable"),modifier=Modifier.fillMaxWidth()) {state.toggleNativeSources()}
    }
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Row(Modifier.fillMaxWidth().mobileGlassRP133().clickable {state.screen=Screen.PROFILES}.padding(15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ProfileAvatar(state.activeProfile,Modifier.size(52.dp))
            Column(Modifier.weight(1f)) {Text(state.activeProfile.name,color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold);Text(tr("Perfiles y fotos","Profiles and avatars"),color=Muted,fontSize=12.sp)}
            MobileIconRP133("chevron")
        }}
        item {Text(tr("Ajustes","Settings"),color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Bold)}
        item {MobileSettingsCardRP133(tr("Cuenta","Account")) {
            if(state.session==null || state.sessionNeedsSignIn) {
                MobileFieldRP133(email,{email=it},tr("Correo electrónico","Email"),keyboard=androidx.compose.ui.text.input.KeyboardType.Email)
                Spacer(Modifier.height(8.dp));MobileFieldRP133(password,{password=it},tr("Contraseña","Password"),password=true)
                Spacer(Modifier.height(8.dp))
                MobileButtonRP133(tr("Iniciar sesión","Sign in"),modifier=Modifier.fillMaxWidth(),selected=true,enabled=email.isNotBlank() && password.isNotBlank()) {val pass=password;password="";state.launch {state.login(email.trim(),pass)}}
            } else {
                Text(state.session?.email.orEmpty(),color=Muted,fontSize=13.sp)
                MobileSettingRowRP133(tr("Sincronizar cuenta","Sync account"),"↻") {state.launch {state.syncFromCloud();state.saveSettingsNowRP83()}}
                MobileSettingRowRP133(tr("Cerrar sesión","Sign out"),"") {state.signOut()}
            }
        }}
        item {MobileSettingsCardRP133(tr("Reproducción","Playback")) {
            MobileSettingRowRP133(tr("Audio principal","Primary audio"),language(state.audioLanguage)) {sheet="audio"}
            MobileSettingRowRP133(tr("Audio secundario","Secondary audio"),language(state.secondaryAudioLanguageRP62)) {sheet="secondary"}
            MobileSettingRowRP133(tr("Subtítulos","Subtitles"),if(state.subtitleLanguage=="off") tr("Desactivados","Off") else language(state.subtitleLanguage)) {sheet="subtitles"}
            MobileSettingRowRP133(tr("Límite de reproducción del celular","Phone playback size limit"),if(state.streamSizeLimitGb==0.0) tr("Sin límite","Unlimited") else "${state.streamSizeLimitGb} GB") {sheet="size"}
            MobileSettingRowRP133(tr("Precarga en detalles","Preload in details"),if(state.sourcePrefetchEnabledRP90) "ON" else "OFF") {state.saveSourcePrefetchRP90(!state.sourcePrefetchEnabledRP90)}
            MobileSettingRowRP133(tr("Siguiente episodio","Next episode"),if(state.autoplayNext) "ON" else "OFF") {state.savePlaybackPreferences(state.audioLanguage,state.subtitleLanguage,!state.autoplayNext)}
            MobileSettingRowRP133(tr("Probar otra fuente si falla","Try next source on failure"),if(state.autoplayNextLink) "ON" else "OFF") {state.saveAutoplayNextLink(!state.autoplayNextLink)}
            MobileSettingRowRP133(tr("Avisos de contenido","Content warnings"),if(state.contentWarningsRP31) "ON" else "OFF") {state.saveContentWarningsRP31(!state.contentWarningsRP31)}
        }}
        item {MobileSettingsCardRP133("Add-ons") {
            Text(tr("Tus fuentes y prioridades de reproducción.","Your playback sources and priorities."),color=Muted,fontSize=13.sp)
            Spacer(Modifier.height(7.dp))
            MobileButtonRP133(tr("Administrar add-ons","Manage add-ons"),"grid",Modifier.fillMaxWidth()) {sheet="addons"}
            MobileSettingRowRP133("BruniO + TorBox",if(state.nativeSourceEnabled) "ON" else "OFF") {sheet="torbox"}
        }}
        item {MobileSettingsCardRP133(tr("Experiencia","Experience")) {
            MobileSettingRowRP133(tr("Idioma de interfaz","Interface language"),if(state.interfaceLanguage=="es") "Español" else "English") {sheet="interface"}
            MobileSettingRowRP133(tr("Tamaño del texto","Text size"),if(MobileUiPrefsRP133.scale>1f) tr("Grande","Large") else tr("Normal","Normal")) {sheet="text"}
            MobileSettingRowRP133(tr("Modo fácil","Easy mode"),if(state.easyModeRP79) "ON" else "OFF") {state.launch {state.setEasyModeRP79(!state.easyModeRP79)}}
            MobileSettingRowRP133(tr("Limpiar caché de imágenes","Clear artwork cache"),"") {state.launch {state.clearCache()}}
            MobileSettingRowRP133(tr("Actualizar canales","Refresh channels"),"") {state.launch {withContext(Dispatchers.IO) {FinalCatalogRP97.refresh()}}}
        }}
        item {MobileButtonRP133(tr("Buscar actualizaciones","Check updates"),"refresh",Modifier.fillMaxWidth()) {update=true;state.launch {state.checkForUpdates()}}}
        item {Text("BruniO Mobile · "+BuildConfig.VERSION_NAME,color=Muted,fontSize=12.sp)}
    }
}

@Composable
private fun MobileAddonsRP133(state:AppState,close:()->Unit) {
    var url by remember {mutableStateOf("")}
    var checked by remember {mutableStateOf<AddonManifestRP126?>(null)}
    val metadata=remember {mutableStateMapOf<String,AddonManifestRP126>()}
    var language by remember {mutableStateOf(state.audioLanguage)}
    var status by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    var remove by remember {mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    val permits=remember {Semaphore(3)}
    LaunchedEffect(state.addonManifests.toList()) {
        launch {runCatching {state.inspectAddonOrderRP2()}}
        state.addonManifests.forEach {manifest->launch {permits.withPermit {runCatching {AddonArtworkRP126.load(manifest)}.getOrNull()?.let {metadata[manifest]=it}}}}
    }
    if(remove!=null) MobileSheetRP133(tr("Eliminar add-on","Remove add-on"),{remove=null}) {
        MobileButtonRP133(tr("Confirmar eliminación","Confirm removal"),"close",Modifier.fillMaxWidth()) {val value=remove!!;remove=null;state.launch {state.removeAddonManifest(value)}}
    }
    MobileSheetRP133("Add-ons",close) {
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {MobileFieldRP133(url,{url=it;checked=null},"https://…/manifest.json",keyboard=androidx.compose.ui.text.input.KeyboardType.Uri)}
            item {MobileButtonRP133(if(busy) tr("Comprobando…","Checking…") else tr("Comprobar add-on","Check add-on"),"link",Modifier.fillMaxWidth(),enabled=!busy && url.isNotBlank()) {
                busy=true;status=""
                scope.launch {try {checked=AddonArtworkRP126.load(AddonManifestRP126.normalize(url))} catch(e:CancellationException) {throw e} catch(e:Exception) {status=e.message.orEmpty()} finally {busy=false}}
            }}
            if(status.isNotBlank()) item {Text(status,color=Muted,fontSize=13.sp)}
            checked?.let {addon->item {Column(Modifier.fillMaxWidth().mobileGlassRP133().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(addon.name,color=Color.White,fontWeight=FontWeight.Bold);Text(addon.description,color=Muted,fontSize=12.sp)
                MobileButtonRP133(tr("Instalar","Install"),"plus",Modifier.fillMaxWidth()) {state.launch {state.addAddonManifest(addon.url);checked=null;url=""}}
            }}}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                MobileButtonRP133("Español",selected=language=="es") {language="es"}
                MobileButtonRP133("English",selected=language=="en") {language="en"}
            }}
            items(state.orderedSourceAddonsRP2(language),key={it.id}) {addon->
                val manifest=state.addonManifests.firstOrNull {AddonPriorityRP2.id(it)==addon.id}
                val details=metadata[manifest]
                Column(Modifier.fillMaxWidth().mobileGlassRP133().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(!details?.logo.isNullOrBlank()) AsyncImage(details?.logo,addon.name,Modifier.size(42.dp))
                        Text(details?.name ?: addon.name,color=Color.White,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        MobileIconButtonRP133("back",tr("Subir prioridad","Raise priority")) {state.moveSourceAddonRP2(addon.id,-1,language)}
                        MobileIconButtonRP133("chevron",tr("Bajar prioridad","Lower priority")) {state.moveSourceAddonRP2(addon.id,1,language)}
                    }
                    if(manifest!=null) {
                        LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {items(listOf(0,5,10,15,25)) {limit->MobileButtonRP133(if(limit==0) "∞" else limit.toString(),selected=state.addonLimitRP7(manifest)==limit) {state.saveAddonLimitRP7(manifest,limit)}}}
                        MobileButtonRP133(tr("Eliminar","Remove"),"close",Modifier.fillMaxWidth()) {remove=manifest}
                    }
                }
            }
        }
    }
}

@Composable
internal fun MobileProfilesRP133(state:AppState) {
    var editing by remember {mutableStateOf<Profile?>(null)}
    var deleting by remember {mutableStateOf<Profile?>(null)}
    deleting?.let {profile->MobileSheetRP133(tr("Eliminar perfil","Delete profile"),{deleting=null}) {
        Text(tr("¿Eliminar el perfil ","Delete profile ")+profile.name+"?",color=Color.White)
        MobileButtonRP133(tr("Confirmar eliminación","Confirm deletion"),"close",Modifier.fillMaxWidth()) {deleting=null;state.launch {state.deleteProfile(profile)}}
    }}
    var creating by remember {mutableStateOf(false)}
    var name by remember {mutableStateOf("")}
    var avatar by remember {mutableStateOf("ai:astronaut")}
    if(creating || editing!=null) MobileSheetRP133(if(creating) tr("Crear perfil","Create profile") else tr("Editar perfil","Edit profile"),{creating=false;editing=null}) {
        MobileFieldRP133(name,{name=it.take(24)},tr("Nombre del perfil","Profile name"))
        ProfileAvatar(Profile("preview",name,avatar),Modifier.size(90.dp))
        LazyVerticalGrid(GridCells.Adaptive(64.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            gridItems(ProfileAvatars,key={it.key}) {option->Box(Modifier.mobileGlassRP133(avatar==option.key).clickable {avatar=option.key}.padding(4.dp)) {ProfileAvatar(Profile("preview",option.label,option.key),Modifier.size(62.dp))}}
        }
        MobileButtonRP133(tr("Guardar","Save"),"check",Modifier.fillMaxWidth(),selected=true,enabled=name.isNotBlank()) {val profile=editing;creating=false;editing=null;state.launch {if(profile==null) state.createProfile(name,avatar) else {state.renameProfile(profile,name);state.setProfileAvatar(profile,avatar)}}}
        editing?.takeUnless {it.primary || it.isKids}?.let {profile->MobileButtonRP133(tr("Eliminar perfil","Delete profile"),"close",Modifier.fillMaxWidth()) {editing=null;deleting=profile}}
    }
    Box(Modifier.fillMaxSize()) {
        ProfileArtworkBackdropRP63(state)
        LazyVerticalGrid(GridCells.Adaptive(135.dp),Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),horizontalArrangement=Arrangement.spacedBy(18.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item(span={GridItemSpan(maxLineSpan)}) {Text(tr("¿Quién va a ver?","Who's watching?"),color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Bold)}
            gridItems(state.profiles,key={it.id}) {profile->Column(Modifier.fillMaxWidth().mobileGlassRP133(profile.id==state.activeProfile.id).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Box(Modifier.clickable {state.launch {state.selectProfile(profile)}}) {ProfileAvatar(profile,Modifier.size(94.dp))}
                Text(profile.name,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold)
                MobileButtonRP133(tr("Editar","Edit")) {name=profile.name;avatar=profile.avatarValue ?: "ai:astronaut";editing=profile}
            }}
            item {MobileButtonRP133(tr("Agregar perfil","Add profile"),"plus",Modifier.fillMaxWidth()) {name="";avatar="ai:astronaut";creating=true}}
        }
    }
}
