@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.miflix.native2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.BuildConfig
import com.miflix.native2.R
import com.miflix.native2.data.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun AppUpdateDialog(state:AppState,close:()->Unit) {
    val context=LocalContext.current
    val first=remember {FocusRequester()}
    var diagnostics by remember {mutableStateOf(false)}
    var history by remember {mutableStateOf(false)}
    val installed by produceState("",diagnostics) {
        if(diagnostics) value=withContext(Dispatchers.IO) {runCatching {UpdateRepository().installationDetails(context)}.getOrDefault("")}
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching {first.requestFocus()}
        state.checkForUpdates()
    }
    val info=state.updateInfo
    val ready=state.downloadedUpdate!=null && info?.isNewer==true && !state.updateDownloading
    val title=when {
        state.updateDownloading ->tr("Preparando tu actualización","Getting your update ready")
        state.updateChecking ->tr("Buscando novedades…","Checking for something new…")
        ready ->tr("Todo listo para instalar","Ready to install")
        state.error!=null ->tr("No pudimos completar la actualización","We couldn't complete the update")
        info?.isNewer==true ->tr("Hay una nueva versión","A new version is available")
        info!=null ->tr("BruniO está al día","BruniO is up to date")
        else ->tr("Tu próxima versión de BruniO","Your next BruniO update")
    }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) {
            Column(Modifier.widthIn(max=820.dp).fillMaxWidth().heightIn(max=maxHeight)
                .background(Bg,RoundedCornerShape(28.dp)).border(1.dp,Color(0xFF383838),RoundedCornerShape(28.dp)).padding(24.dp),
                verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Image(painterResource(R.drawable.brunio_icon),"BruniO",Modifier.size(48.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(tr("Actualizaciones","Updates"),color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
                        Text(tr("Versión instalada · ","Installed version · ")+BuildConfig.VERSION_NAME,color=Muted,fontSize=13.sp)
                    }
                    FocusButton("×",horizontalPadding=16.dp,onClick=close)
                }
                LazyColumn(Modifier.weight(1f,false).fillMaxWidth(),contentPadding=PaddingValues(vertical=6.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    item {
                        Column(Modifier.fillMaxWidth().background(GlassActionBrushRP108(false,true),RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text(title,color=Color.White,fontSize=24.sp,lineHeight=30.sp,fontWeight=FontWeight.Bold)
                            Text(when {
                                state.updateDownloading ->tr("Puedes seguir usando BruniO mientras termina la descarga.","You can keep using BruniO while the download finishes.")
                                ready ->tr("La descarga se verificó. Android te pedirá confirmar la instalación.","The download was verified. Android will ask you to confirm installation.")
                                info?.isNewer==true ->tr("Disponible · ","Available · ")+info.version
                                state.updateChecking ->tr("Comprobando la versión disponible.","Looking up the available release.")
                                info!=null && state.error==null ->tr("Ya tienes la versión más reciente disponible para este dispositivo.","You have the latest available release for this device.")
                                else ->tr("Vuelve a comprobar cuando lo desees.","Check again whenever you're ready.")
                            },color=Muted,fontSize=14.sp,lineHeight=20.sp)
                            if(state.updateDownloading) {
                                val total=state.updateTotal
                                val progress=if(total>0) (state.updateBytes.toDouble()/total).coerceIn(0.0,1.0).toFloat() else 0f
                                Box(Modifier.fillMaxWidth().height(7.dp).background(Color(0xFF454545),RoundedCornerShape(4.dp))) {
                                    if(total>0) Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color.White,RoundedCornerShape(4.dp)))
                                }
                                Text(if(total>0) "${(progress*100).toInt()}% · ${updateSizeRP127(state.updateBytes)} / ${updateSizeRP127(total)}" else updateSizeRP127(state.updateBytes),color=Color.White,fontSize=13.sp)
                            }
                        }
                    }
                    if(state.updateStatus.isNotBlank()) item {Text(state.updateStatus,color=Color.White,fontSize=14.sp,lineHeight=21.sp,modifier=Modifier.fillMaxWidth().focusable())}
                    state.error?.let {message ->item {
                        Text(message,color=Color(0xFFFFCBCB),fontSize=14.sp,lineHeight=21.sp,modifier=Modifier.fillMaxWidth().background(Color(0xFF2A1B20),RoundedCornerShape(14.dp)).padding(16.dp).focusable())
                    }}
                    item {
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                            FocusButton(if(state.updateChecking) tr("Comprobando…","Checking…") else tr("Buscar actualización","Check for update"),modifier=Modifier.focusRequester(first)) {
                                if(!state.updateChecking && !state.updateDownloading) state.launch {state.checkForUpdates()}
                            }
                            if(state.updateDownloading) FocusButton(tr("Cancelar descarga","Cancel download"),destructive=true) {state.cancelUpdateDownload()}
                            else if(info?.isNewer==true && !state.updateChecking) {
                                FocusButton(if(ready) tr("Instalar actualización","Install update") else tr("Descargar actualización","Download update"),primary=true) {
                                    if(ready) state.installUpdate(context) else state.downloadUpdate()
                                }
                            }
                            FocusButton(tr("Ver novedades","What's new")) {history=true}
                        }
                    }
                    item {FocusButton((if(diagnostics) "− " else "+ ")+tr("Detalles de instalación","Installation details")) {diagnostics=!diagnostics}}
                    if(diagnostics) item {Text(installed.ifBlank {tr("Cargando detalles…","Loading details…")},color=Muted,fontSize=12.sp,lineHeight=18.sp,modifier=Modifier.fillMaxWidth().background(Color(0xFF171717),RoundedCornerShape(14.dp)).padding(16.dp).focusable())}
                }
            }
        }
    }
    if(history) ChangelogDialog(state) {history=false}
}

private fun updateSizeRP127(bytes:Long)=String.format(Locale.getDefault(),"%.1f MB",bytes.coerceAtLeast(0)/1048576.0)
