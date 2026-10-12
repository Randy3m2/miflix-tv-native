package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.miflix.native2.BuildConfig
import com.miflix.native2.data.UpdateRepository

@Composable
fun AppUpdateDialog(state: AppState, close: () -> Unit) {
    val context=LocalContext.current
    val first=remember { FocusRequester() }
    var diagnostics by remember { mutableStateOf(false) }
    val installed=remember { runCatching { UpdateRepository().installationDetails(context) }.getOrDefault("") }
    LaunchedEffect(Unit) { withFrameNanos { };first.requestFocus();state.checkForUpdates() }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.widthIn(max=720.dp).fillMaxWidth(.9f).heightIn(max=480.dp)
            .background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(tr("Actualizaciones","Updates"),color=Color.White,fontSize=26.sp)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                item { Text("${tr("Versión instalada","Installed version")}: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",color=Muted) }
                item { FocusButton(if(state.updateChecking) tr("Comprobando…","Checking…") else tr("Buscar actualizaciones","Check for Updates"),modifier=Modifier.focusRequester(first)) {
                    if(!state.updateChecking && !state.updateDownloading) state.launch { state.checkForUpdates() }
                } }
                state.updateInfo?.let { info ->
                    item { Text(if(info.isNewer) "${tr("Disponible","Available")}: ${info.version}" else tr("Tienes la versión más reciente.","You're on the latest build."),color=Color.White) }
                    if(info.isNewer && !state.updateDownloading) item {
                        if(state.downloadedUpdate!=null) FocusButton(tr("Instalar actualización","Install update")) { state.installUpdate(context) }
                        else FocusButton(tr("Descargar actualización","Download update")) { state.downloadUpdate() }
                    }
                }
                if(state.updateDownloading) {
                    item { Text("${tr("Descargando","Downloading")}: ${state.updateBytes/1048576} / ${state.updateTotal/1048576} MB",color=Color.White) }
                    item { FocusButton(tr("Cancelar descarga","Cancel download")) { state.cancelUpdateDownload() } }
                }
                if(state.updateStatus.isNotBlank()) item { Text(state.updateStatus,color=Color.White,modifier=Modifier.fillMaxWidth().focusable(),fontSize=15.sp,lineHeight=21.sp) }
                state.error?.let { message -> item { Text(message,color=Color(0xFFFFC8C8),modifier=Modifier.fillMaxWidth().focusable(),fontSize=14.sp,lineHeight=20.sp) } }
                item { FocusButton(tr("Detalles de instalación","Installation details")) { diagnostics=!diagnostics } }
                if(diagnostics) item { Text(installed,color=Muted,fontSize=13.sp,lineHeight=19.sp,modifier=Modifier.fillMaxWidth().focusable()) }
            }
            FocusButton(tr("Cerrar","Close")) { close() }
        }
    }
}
