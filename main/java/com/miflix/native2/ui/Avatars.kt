package com.miflix.native2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.R
import com.miflix.native2.model.Profile
import kotlinx.coroutines.delay

data class AvatarOption(val key: String, val label: String, val image: Int)
val ProfileAvatars = listOf(
    AvatarOption("ai:astronaut","Astronauta",R.drawable.brunio_avatar_astronaut),
    AvatarOption("ai:fox","Zorro",R.drawable.brunio_avatar_fox),
    AvatarOption("ai:robot","Robot",R.drawable.brunio_avatar_robot),
    AvatarOption("ai:dragon","Dragón",R.drawable.brunio_avatar_dragon),
    AvatarOption("special:photo","Especial",R.drawable.brunio_avatar_special),
    AvatarOption("special:extra","Especial 2",R.drawable.brunio_avatar_extra),
    AvatarOption("special:extra3","Especial 3",R.drawable.brunio_avatar_special3),
    AvatarOption("special:extra4","Especial 4",R.drawable.brunio_avatar_special4),
    AvatarOption("special:extra5","Especial 5",R.drawable.brunio_avatar_special5),
    AvatarOption("special:extra6","Especial 6",R.drawable.brunio_avatar_special6),
    AvatarOption("special:extra7","Especial 7",R.drawable.brunio_avatar_special7),
    AvatarOption("special:extra8","Especial 8",R.drawable.brunio_avatar_special8),
    AvatarOption("special:extra9","Especial 9",R.drawable.brunio_avatar_special9),
    AvatarOption("special:extra10","Especial 10",R.drawable.brunio_avatar_special10),
    AvatarOption("special:extra11","Especial 11",R.drawable.brunio_avatar_special11),
    AvatarOption("special:extra12","Especial 12",R.drawable.brunio_avatar_special12),
    AvatarOption("special:extra13","Especial 13",R.drawable.brunio_avatar_special13),
    AvatarOption("special:extra14","Especial 14",R.drawable.brunio_avatar_special14),
    AvatarOption("special:extra15","Especial 15",R.drawable.brunio_avatar_special15)
)

@Composable
fun ProfileAvatar(profile: Profile, modifier: Modifier = Modifier) {
    val option=ProfileAvatars.firstOrNull { it.key==profile.avatarValue }
        ?: ProfileAvatars[(profile.id.hashCode() and Int.MAX_VALUE) % 4]
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Panel)) {
        if(profile.avatarValue=="none") {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text(profile.name.take(1).uppercase(),color=Color.White,fontSize=24.sp, lineHeight=29.sp) }
        } else if(profile.avatarValue?.startsWith("https://")==true) {
            AsyncImage(profile.avatarValue,profile.name,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        } else Image(painterResource(option.image),profile.name,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
    }
}

@Composable
fun AvatarPickerDialog(selected: String?, choose: (String) -> Unit, close: () -> Unit,
                       hidden: Set<String> = emptySet(), remove: ((String) -> Unit)? = null,
                       restore: (() -> Unit)? = null) {
    val options=ProfileAvatars.filterNot { it.key in hidden }
    val first=remember { FocusRequester() }
    LaunchedEffect(options.map { it.key }) { if(options.isNotEmpty()) { delay(100); first.requestFocus() } }
    Dialog(onDismissRequest=close) {
        Column(Modifier.width(420.dp).heightIn(max=480.dp).background(Panel,RoundedCornerShape(24.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Elige tu avatar","Choose your avatar"),color=Color.White,fontSize=26.sp, lineHeight=31.sp)
            FocusButton(tr("Quitar foto del perfil","Remove profile picture")) { choose("none") }
            LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                options.chunked(2).forEachIndexed { row,group -> item {
                    Row(horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        group.forEachIndexed { col,option ->
                            var focused by remember(option.key) { mutableStateOf(false) }
                            Column(Modifier.width(150.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.fillMaxWidth().then(if(row==0 && col==0) Modifier.focusRequester(first) else Modifier)
                                    .onFocusChanged { focused=it.isFocused }.focusable().tvClick { choose(option.key) }
                                    .clickable { choose(option.key) }
                                    .border(if(focused) 3.dp else if(option.key==selected) 2.dp else 0.dp,
                                        if(focused) Color.White else Color(0xFF7EDAFF),RoundedCornerShape(18.dp)).padding(6.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally) {
                                    Image(painterResource(option.image),avatarLabel(option.key),Modifier.size(92.dp).clip(RoundedCornerShape(16.dp)),contentScale=ContentScale.Crop)
                                    Text(avatarLabel(option.key),color=Color.White,fontSize=16.sp, lineHeight=19.sp)
                                }
                                if(remove!=null) FocusButton(tr("Eliminar","Remove"),modifier=Modifier.fillMaxWidth()) { remove(option.key) }
                            }
                        }
                    }
                } }
            }
            if(hidden.isNotEmpty() && restore!=null) FocusButton(tr("Restaurar galería","Restore gallery")) { restore() }
            FocusButton(tr("Cancelar","Cancel")) { close() }
        }
    }
}

fun avatarLabel(key: String): String = when(key) {
    "ai:astronaut" -> tr("Astronauta","Astronaut")
    "ai:fox" -> tr("Zorro","Fox")
    "ai:dragon" -> tr("Dragón","Dragon")
    "special:photo" -> tr("Especial","Special")
    "special:extra" -> tr("Especial 2","Special 2")
    "special:extra3" -> tr("Especial 3","Special 3")
    "special:extra4" -> tr("Especial 4","Special 4")
    "special:extra5" -> tr("Especial 5","Special 5")
    "special:extra6" -> tr("Especial 6","Special 6")
    "special:extra7" -> tr("Especial 7","Special 7")
    "special:extra8" -> tr("Especial 8","Special 8")
    "special:extra9" -> tr("Especial 9","Special 9")
    "special:extra10" -> tr("Especial 10","Special 10")
    "special:extra11" -> tr("Especial 11","Special 11")
    "special:extra12" -> tr("Especial 12","Special 12")
    "special:extra13" -> tr("Especial 13","Special 13")
    "special:extra14" -> tr("Especial 14","Special 14")
    "special:extra15" -> tr("Especial 15","Special 15")
    else -> "Robot"
}


@Composable
fun AvatarUploadDialog(state: AppState) {
    val profile=state.avatarUploadProfile ?: return
    LaunchedEffect(state.avatarUploadRequest?.code) {
        if(state.avatarUploadRequest!=null) {
            val deadline=System.currentTimeMillis()+10*60*1000L
            try {
                while(state.avatarUploadRequest!=null && System.currentTimeMillis()<deadline) {
                    delay(2000)
                    if(state.pollAvatarUpload()) return@LaunchedEffect
                }
                if(state.avatarUploadRequest!=null) {
                    state.avatarUploadStatus=tr("QR expirado. Genera uno nuevo.","QR expired. Create a new one.")
                    state.avatarUploadRequest=null
                }
            } catch(e: kotlinx.coroutines.CancellationException) { throw e } catch(e: Exception) {
                state.avatarUploadStatus=e.message ?: tr("No se pudo subir el avatar","Avatar upload failed")
            }
        }
    }
    Dialog(onDismissRequest=state::closeAvatarUpload) {
        Column(Modifier.width(440.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(tr("Tu avatar","Your avatar")+" · "+profile.name,color=Color.White,fontSize=24.sp)
            state.avatarUploadRequest?.let { QrCode(it.url,220.dp) }
            Text(state.avatarUploadStatus,color=Color.White,fontSize=15.sp)
            FocusButton(tr("Nuevo QR","New QR")) { state.launch { state.socialAction { state.startAvatarUpload(profile) } } }
            FocusButton(tr("Cerrar","Close")) { state.closeAvatarUpload() }
        }
    }
}
