package com.miflix.native2.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

/** Focus/hover selects the field. Only an explicit click or OK creates a text editor. */
@Composable
internal fun AssistantSearchFieldRP153(value:String,change:(String)->Unit,enabled:Boolean,modifier:Modifier=Modifier,submit:()->Unit) {
    var editing by remember {mutableStateOf(false)}
    var focused by remember {mutableStateOf(false)}
    var editorHadFocus by remember {mutableStateOf(false)}
    val fieldFocus=remember {FocusRequester()}
    val editorFocus=remember {FocusRequester()}
    val keyboard=LocalSoftwareKeyboardController.current
    val scope=rememberCoroutineScope()
    val hint=tr("Cuéntame qué te apetece ver…","Tell me what you're in the mood for…")
    fun close(returnFocus:Boolean) {
        keyboard?.hide();editing=false;editorHadFocus=false
        if(returnFocus) scope.launch {withFrameNanos {};runCatching {fieldFocus.requestFocus()}}
    }
    androidx.activity.compose.BackHandler(enabled=editing) {close(true)}
    if(editing) {
        DisposableEffect(Unit) {onDispose {keyboard?.hide()}}
        BasicTextField(value,change,singleLine=true,textStyle=TextStyle(color=Color.White,fontSize=19.sp),cursorBrush=SolidColor(Color.White),
            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={close(true);submit()},onDone={close(true)}),
            modifier=modifier.focusRequester(editorFocus).onFocusChanged {if(it.isFocused) editorHadFocus=true else if(editorHadFocus) close(false)}
                .border(1.dp,Color.White,RoundedCornerShape(12.dp)).padding(14.dp),
            decorationBox={inner ->if(value.isBlank()) Text(hint,color=Muted,fontSize=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis);inner()})
        LaunchedEffect(Unit) {withFrameNanos {};editorFocus.requestFocus();keyboard?.show()}
    } else {
        val activate={if(enabled) editing=true;Unit}
        Box(modifier.focusRequester(fieldFocus).onFocusChanged {focused=it.isFocused}.focusable().tvClick(activate).clickable(enabled=enabled,onClick=activate)
            .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else Color.Transparent,RoundedCornerShape(12.dp)).padding(14.dp)) {
            Text(value.ifBlank {hint},color=if(value.isBlank()) Muted else Color.White,fontSize=18.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}
