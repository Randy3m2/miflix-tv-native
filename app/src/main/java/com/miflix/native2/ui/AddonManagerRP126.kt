@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class,androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.AddonArtworkRP126
import com.miflix.native2.data.AddonManifestRP126
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AddonManagerRP126(state:AppState,close:()->Unit) {
    key(state.session?.userId,state.activeProfile.id) {AddonManagerContentRP126(state,close)}
}

@Composable
private fun AddonManagerContentRP126(state:AppState,close:()->Unit) {
    val scope=rememberCoroutineScope()
    val manifests=state.addonManifests.toList()
    val metadata=remember {mutableStateMapOf<String,AddonManifestRP126>()}
    var adding by remember {mutableStateOf(false)}
    var startQr by remember {mutableStateOf(false)}
    var order by remember {mutableStateOf(false)}
    var selected by remember {mutableStateOf<String?>(null)}
    var confirmRemove by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    LaunchedEffect(manifests) {
        metadata.keys.toList().filter {it !in manifests}.forEach {metadata.remove(it)}
        // Bounded in the repository; each logo resolves independently, off the UI thread.
        manifests.forEach {url ->launch {
            try {metadata[url]=AddonArtworkRP126.load(url)}
            catch(e:CancellationException) {throw e} catch(_:Exception) { }
        }}
    }
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        CompositionLocalProvider(androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides SettingsBringRP138) {
        Column(Modifier.fillMaxSize().background(Bg).padding(horizontal=34.dp,vertical=22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Add-ons",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
                    Text(state.activeProfile.name+" · "+manifests.size+tr(" instalados"," installed"),color=Muted,fontSize=14.sp)
                }
                FocusButton("×",horizontalPadding=16.dp,onClick=close)
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                FocusButton(tr("+ Agregar add-on","+ Add add-on")) {startQr=false;adding=true}
                FocusButton(tr("Orden de reproducción","Playback priority")) {order=true}
                FocusButton(tr("Agregar por QR local","Add via local QR")) {startQr=true;adding=true}
            }
            if(!state.activeProfile.primary) {
                FocusButton((if(state.profileAddonsInherited) "✓ " else "")+tr("Usar add-ons del perfil principal","Use main profile add-ons"),primary=state.profileAddonsInherited) {
                    state.saveProfileAddonInheritance(!state.profileAddonsInherited)
                }
            }
            if(message.isNotBlank()) Text(message,color=Color.White,fontSize=13.sp)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(manifests.isEmpty()) item {Text(tr("Agrega tu primer add-on con su enlace manifest.json.","Add your first add-on using its manifest.json link."),color=Muted)}
                items(manifests,key={it}) {url ->
                    val info=metadata[url]
                    val title=addonTitleRP126(state,url,info)
                    Row(Modifier.fillMaxWidth().background(GlassActionBrushRP108(false,true),RoundedCornerShape(18.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        AddonLogoRP126(info?.logo.orEmpty(),title,Modifier.size(70.dp))
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(title,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text(info?.description?.takeIf {it.isNotBlank()} ?: tr("Fuente de enlaces de reproducción","Playback link provider"),color=Muted,fontSize=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                            val limit=state.addonLimitRP7(url)
                            Text(tr("Enlaces por tanda: ","Links per batch: ")+(if(limit==0) tr("Todos","All") else limit.toString()),color=Muted,fontSize=12.sp)
                        }
                        FocusButton(tr("Administrar","Manage")) {selected=url;confirmRemove=false;message=""}
                    }
                }
            }
        }
        }
    }
    if(order) AddonPrioritySettingsRP2(state) {order=false}
    if(adding) AddonInstallRP126(state,{adding=false},initialQr=startQr) {info ->metadata[info.url]=info}
    selected?.takeIf {it in manifests}?.let {url ->
        val title=addonTitleRP126(state,url,metadata[url])
        PlayerSideSheet(title,{if(!busy) selected=null}) {
            item {AddonLogoRP126(metadata[url]?.logo.orEmpty(),title,Modifier.size(76.dp))}
            item {Text(tr("Cantidad de enlaces por tanda","Links per batch"),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)}
            item {FlowRow(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                listOf(0,5,7,10,15).forEach {n ->FocusButton(if(n==0) tr("Todos","All") else "$n",primary=state.addonLimitRP7(url)==n,horizontalPadding=12.dp) {state.saveAddonLimitRP7(url,n)}}
            }}
            item {
                var custom by remember(url) {mutableStateOf("")}
                NativeTextField(custom,{custom=it.filter {c ->c.isDigit()}.take(4)},tr("Personalizado · 1 a 1000","Custom · 1 to 1000"),modifier=Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                FocusButton(tr("Guardar límite","Save limit")) {
                    val n=custom.toIntOrNull()
                    if(n!=null && n in 1..1000) {state.saveAddonLimitRP7(url,n);selected=null}
                    else message=tr("Escribe un número entre 1 y 1000.","Enter a number from 1 to 1000.")
                }
            }
            item {FocusButton(tr("Orden de reproducción","Playback priority")) {selected=null;order=true}}
            item {Text(tr("Para cambiar la configuración, genera otro manifest.json en la página del add-on y agrégalo aquí. Luego puedes eliminar el anterior.","To change its configuration, generate another manifest.json on the add-on website and add it here. You can then remove the old one."),color=Muted,fontSize=13.sp)}
            if(!confirmRemove) item {FocusButton(tr("Eliminar add-on","Remove add-on"),destructive=true) {confirmRemove=true}}
            else {
                item {Text(tr("¿Eliminar este add-on de la configuración de este perfil?","Remove this add-on from this profile's configuration?"),color=Color.White)}
                item {FocusButton(if(busy) tr("Eliminando…","Removing…") else tr("Confirmar eliminación","Confirm removal"),destructive=true) {
                    if(!busy) {busy=true;scope.launch {
                        try {state.removeAddonManifest(url);selected=null;message=tr("Add-on eliminado","Add-on removed")}
                        catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo guardar el cambio. Reintenta.","Could not save the change. Try again.")}
                        finally {busy=false}
                    }}
                }}
                item {FocusButton(tr("Cancelar","Cancel")) {if(!busy) confirmRemove=false}}
            }
            if(message.isNotBlank()) item {Text(message,color=Color.White)}
        }
    }
}

private fun addonTitleRP126(state:AppState,url:String,info:AddonManifestRP126?):String {
    val installed=state.streamRepo.addonLabel(url)
    return if(installed=="Mandanguita TV") installed else info?.name ?: installed
}

@Composable
private fun AddonLogoRP126(url:String,title:String,modifier:Modifier) {
    var failed by remember(url) {mutableStateOf(false)}
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF161616)),contentAlignment=Alignment.Center) {
        if(url.isBlank() || failed) Text(title.trim().split(" ").take(2).joinToString("") {it.take(1)}.uppercase(),color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)
        else AsyncImage(url,title,Modifier.fillMaxSize().padding(6.dp),contentScale=ContentScale.Fit,onError={failed=true})
    }
}

@Composable
private fun AddonInstallRP126(state:AppState,close:()->Unit,initialQr:Boolean=false,installed:(AddonManifestRP126)->Unit) {
    val scope=rememberCoroutineScope()
    var input by remember {mutableStateOf("")}
    var preview by remember {mutableStateOf<AddonManifestRP126?>(null)}
    var showQr by remember {mutableStateOf(initialQr)}
    var busy by remember {mutableStateOf(false)}
    var status by remember {mutableStateOf("")}
    PlayerSideSheet(tr("Agregar add-on","Add add-on"),close) {
        item {Text(tr("Pega el enlace de instalación","Paste the installation link"),color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)}
        item {Text(tr("Configúralo primero en su página y copia su URL manifest.json. También acepta enlaces stremio://.","Configure it on its website first, then copy its manifest.json URL. stremio:// links are also accepted."),color=Muted,fontSize=14.sp)}
        item {FocusButton(tr("Pegar desde el celular · QR","Paste from phone · QR")) {if(!busy) showQr=true}}
        item {NativeTextField(input,{if(!busy) {input=it;preview=null;status=""}},"https://…/manifest.json",modifier=Modifier.fillMaxWidth())}
        item {FocusButton(if(busy) tr("Comprobando…","Checking…") else tr("Comprobar add-on","Check add-on")) {
            if(!busy) {busy=true;status="";scope.launch {
                try {preview=AddonArtworkRP126.load(AddonManifestRP126.normalize(input))}
                catch(e:CancellationException) {throw e} catch(_:Exception) {preview=null;status=tr("No se pudo validar. Usa la URL configurada de manifest.json, sin parámetros después de .json, y un add-on que ofrezca enlaces.","Could not validate. Use the configured manifest.json URL without parameters after .json, from an add-on that provides playback links.")}
                finally {busy=false}
            }}
        }}
        preview?.let {info ->
            item {AddonLogoRP126(info.logo,info.name,Modifier.size(88.dp))}
            item {Text(info.name,color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)}
            item {Text(info.description,color=Muted,maxLines=5,overflow=TextOverflow.Ellipsis)}
            item {FocusButton(if(info.url in state.addonManifests) tr("Ya instalado","Already installed") else if(busy) tr("Guardando…","Saving…") else tr("Instalar add-on","Install add-on"),primary=true) {
                if(!busy && info.url !in state.addonManifests) {busy=true;scope.launch {
                    try {state.addAddonManifest(info.url);installed(info);close()}
                    catch(e:CancellationException) {throw e} catch(_:Exception) {status=if(state.session==null) tr("Inicia sesión para guardar tus add-ons.","Sign in to save your add-ons.") else tr("No se pudo guardar. Reintenta.","Could not save. Try again.")}
                    finally {busy=false}
                }}
            }}
        }
        if(status.isNotBlank()) item {Text(status,color=Color.White,fontSize=13.sp)}
    }
    if(showQr) AddonManifestQrRP128(state,received={value ->
        input=value;preview=null
        status=tr("Enlace recibido. Comprueba el add-on antes de instalar.","Link received. Check the add-on before installing.")
    },close={showQr=false})

}
