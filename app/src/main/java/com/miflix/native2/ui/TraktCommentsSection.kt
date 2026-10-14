package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.miflix.native2.data.TraktComment
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.CancellationException

@Composable
fun TraktCommentsSection(state: AppState, item: MediaSummary,compact: Boolean=false) {
    val owner=state.session?.userId
    var rows by remember(item.cloudId,owner) { mutableStateOf<List<TraktComment>>(emptyList()) }
    var loading by remember(item.cloudId,owner) { mutableStateOf(false) }
    var failure by remember(item.cloudId,owner) { mutableStateOf<String?>(null) }
    var retry by remember(item.cloudId,owner) { mutableStateOf(0) }
    var selected by remember(item.cloudId,owner) { mutableStateOf<TraktComment?>(null) }
    LaunchedEffect(item.cloudId,owner,retry) {
        if(owner==null) return@LaunchedEffect
        loading=true;failure=null
        try {
            state.refreshAccountSession()
            val account=state.session?.takeIf { it.userId==owner } ?: return@LaunchedEffect
            rows=state.trakt.comments(account,item)
        } catch(e: CancellationException) { throw e }
        catch(_: Exception) { failure=tr("No se pudieron cargar los comentarios.","Unable to load comments.") }
        finally { loading=false }
    }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(tr("Opiniones · Trakt","Community reviews · Trakt"),color=Color.White,fontSize=if(compact) 22.sp else 27.sp)
            FocusButton(tr("Actualizar comentarios","Refresh comments")) { if(!loading) retry++ }
        }
        when {
            owner==null -> Text(tr("Inicia sesión en BruniO para leer comentarios de Trakt.","Sign in to BruniO to read Trakt comments."),color=Muted)
            loading -> Text(tr("Cargando comentarios…","Loading comments…"),color=Muted)
            failure!=null -> {
                Text(failure.orEmpty(),color=Muted)
                FocusButton(tr("Reintentar","Retry")) { retry++ }
            }
            rows.isEmpty() -> Text(tr("Todavía no hay comentarios disponibles en Trakt para este título.","No Trakt comments available for this title yet."),color=Muted)
            else -> LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(end=40.dp)) {
                items(rows,key={ it.id }) { row ->
                    Column(Modifier.width(if(compact) 280.dp else 370.dp).height(if(compact) 240.dp else 275.dp).background(Panel,RoundedCornerShape(14.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text(row.author,color=Color.White,fontSize=18.sp,maxLines=1)
                        Text("♥ ${row.likes} · ${row.date}",color=Muted,fontSize=13.sp)
                        Text(if(row.spoiler) tr("⚠ Contiene spoilers","⚠ Contains spoilers") else row.text,
                            color=Color.White,fontSize=16.sp,maxLines=5,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                        FocusButton(if(row.spoiler) tr("Mostrar spoiler","Show spoiler") else tr("Leer comentario","Read comment")) { selected=row }
                    }
                }
            }
        }
        Text(tr("Opiniones de usuarios de Trakt, en su idioma original.","Trakt user opinions, in their original language."),color=Muted,fontSize=13.sp)
        Spacer(Modifier.height(16.dp))
    }
    selected?.let { row ->
        val closeFocus=remember(row.id) { FocusRequester() }
        LaunchedEffect(row.id) { withFrameNanos { };runCatching { closeFocus.requestFocus() } }
        val text=remember(row.id) { row.text.replace(Regex("\\[/?spoiler(?:=[^\\]]*)?\\]",RegexOption.IGNORE_CASE),"") }
        Dialog(onDismissRequest={ selected=null }) {
            Column(Modifier.width(650.dp).heightIn(max=570.dp).background(Panel,RoundedCornerShape(20.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("${row.author} · Trakt",color=Color.White,fontSize=23.sp)
                if(row.spoiler) Text(tr("⚠ Spoiler mostrado","⚠ Spoiler revealed"),color=Muted)
                FocusButton(tr("Cerrar","Close"),modifier=Modifier.focusRequester(closeFocus)) { selected=null }
                LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    items(text.chunked(600)) { paragraph -> Text(paragraph,color=Color.White,fontSize=18.sp,modifier=Modifier.focusable()) }
                }
            }
        }
    }
}
