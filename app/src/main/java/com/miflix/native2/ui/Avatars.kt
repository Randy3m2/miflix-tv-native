package com.miflix.native2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
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
    val character=characterAvatar(profile.avatarValue)
    val option=ProfileAvatars.firstOrNull { it.key==profile.avatarValue }
        ?: ProfileAvatars[(profile.id.hashCode() and Int.MAX_VALUE) % 4]
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Panel)) {
        if(profile.avatarValue=="none") {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text(profile.name.take(1).uppercase(),color=Color.White,fontSize=24.sp, lineHeight=29.sp) }
        } else if(character!=null) {
            CharacterAvatarImage(character,Modifier.fillMaxSize())
        } else if(profile.avatarValue?.startsWith("https://")==true) {
            AsyncImage(profile.avatarValue,profile.name,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        } else Image(painterResource(option.image),profile.name,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
    }
}

private data class PickerAvatar(val key: String,val label: String)

@Composable
fun AvatarPickerDialog(selected: String?, choose: (String) -> Unit, close: () -> Unit,
                       hidden: Set<String> = emptySet(), remove: ((String) -> Unit)? = null,
                       restore: (() -> Unit)? = null,customUpload: (() -> Unit)? = null) {
    var category by remember { mutableStateOf(characterAvatar(selected)?.pack?.id ?: if(selected?.startsWith("https://")==true) "custom" else "brunio") }
    val categories=listOf("brunio" to "BruniO")+CharacterAvatarPacks.map { it.id to it.title }+listOf("custom" to "Custom")
    val options=if(category=="brunio") ProfileAvatars.map { PickerAvatar(it.key,avatarLabel(it.key)) }
        else CharacterAvatars.filter { it.pack.id==category }.map { PickerAvatar(it.key,it.label) }
    val visible=options.filterNot { it.key in hidden }
    val menuFocus=remember { FocusRequester() }
    val galleryFocus=remember { FocusRequester() }
    val grid=rememberLazyGridState()
    val categoryScroll=androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex=categories.indexOfFirst { it.first==category }.coerceAtLeast(0))
    LaunchedEffect(Unit) { withFrameNanos { };runCatching { menuFocus.requestFocus() } }
    LaunchedEffect(category) { grid.scrollToItem(0) }
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.widthIn(max=820.dp).fillMaxWidth(.94f).height(440.dp)
            .background(Panel,RoundedCornerShape(22.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(tr("Editar foto del perfil","Edit profile picture"),color=Color.White,fontSize=24.sp,modifier=Modifier.weight(1f))
                FocusButton(tr("Cerrar","Close"),horizontalPadding=14.dp) { close() }
            }
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                LazyColumn(Modifier.width(190.dp).fillMaxHeight().focusGroup(),state=categoryScroll,verticalArrangement=Arrangement.spacedBy(6.dp),contentPadding=PaddingValues(bottom=12.dp)) {
                    items(categories,key={it.first}) { (id,title) ->
                        FocusButton((if(category==id) "• " else "")+title,
                            modifier=Modifier.fillMaxWidth().then(if(id==categories.firstOrNull { it.first==category }?.first) Modifier.focusRequester(menuFocus) else Modifier),
                            horizontalPadding=10.dp) { category=id }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(categories.firstOrNull {it.first==category}?.second.orEmpty(),color=Accent,fontSize=18.sp)
                    if(category=="custom") {
                        selected?.takeIf { it.startsWith("https://") }?.let { url ->
                            ProfileAvatar(Profile("custom-preview","Custom",avatarValue=url),Modifier.size(112.dp))
                        }
                        Text(tr("Sube tu propia imagen desde el celular usando el QR.","Upload your own picture from your phone using the QR."),color=Muted,fontSize=15.sp,maxLines=3)
                        if(customUpload!=null) FocusButton(tr("Subir foto · QR","Upload picture · QR"),primary=true) { customUpload() }
                        else Text(tr("Crea el perfil primero y luego elige Editar foto para subir tu imagen.","Create the profile first, then choose Edit picture to upload your image."),color=Muted,fontSize=14.sp,maxLines=3)
                        FocusButton(tr("Quitar foto","Remove picture")) { choose("none") }
                    } else {
                        LazyVerticalGrid(columns=GridCells.Adaptive(120.dp),state=grid,
                            modifier=Modifier.weight(1f).fillMaxWidth().focusRequester(galleryFocus).focusGroup(),
                            horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(10.dp),
                            contentPadding=PaddingValues(6.dp)) {
                            gridItems(visible,key={it.key}) { option ->
                                var focused by remember(option.key) { mutableStateOf(false) }
                                Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    Column(Modifier.fillMaxWidth().onFocusChanged { focused=it.isFocused }.focusable()
                                        .tvClick { choose(option.key) }.clickable { choose(option.key) }
                                        .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else if(option.key==selected) Accent else PanelBorder,RoundedCornerShape(14.dp))
                                        .padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                        ProfileAvatar(Profile("preview-${option.key}",option.label,avatarValue=option.key),Modifier.size(92.dp))
                                        Text(option.label,color=Color.White,fontSize=13.sp,lineHeight=17.sp,maxLines=1,overflow=TextOverflow.Ellipsis,
                                            modifier=Modifier.padding(top=6.dp))
                                    }
                                    if(remove!=null) FocusButton(tr("Ocultar","Hide"),modifier=Modifier.fillMaxWidth(),horizontalPadding=10.dp) { remove(option.key) }
                                }
                            }
                        }
                        if(visible.isEmpty()) Text(tr("No hay avatares visibles. Restaura la galería.","No visible avatars. Restore the gallery."),color=Muted,fontSize=14.sp)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(visible.isNotEmpty()) FocusButton(tr("Elegir →","Choose →"),horizontalPadding=12.dp) { runCatching { galleryFocus.requestFocus() } }
                            if(hidden.isNotEmpty() && restore!=null) FocusButton(tr("Restaurar","Restore"),horizontalPadding=12.dp) { restore() }
                        }
                    }
                }
            }
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
