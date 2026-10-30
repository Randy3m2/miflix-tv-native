package com.miflix.native2.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** Explicit AI press starts capture; ordinary focus never launches the IME or speech activity. */
@Composable
internal fun AssistantEntryRP156(state:AppState,close:()->Unit,submit:(String)->Unit) {
    val context=LocalContext.current
    val owner=state.localExtrasScope()
    val fire=remember {android.os.Build.MANUFACTURER.equals("Amazon",true) || android.os.Build.MODEL.startsWith("AFT",true) || context.packageManager.hasSystemFeature("amazon.hardware.fire_tv")}
    var keyboard by remember {mutableStateOf(fire)}
    var text by remember {mutableStateOf("")}
    var submittedRP160 by remember {mutableStateOf(false)}
    var listening by remember {mutableStateOf(false)}
    var status by remember {mutableStateOf("")}
    val focus=remember {FocusRequester()}
    val ime=LocalSoftwareKeyboardController.current
    fun finish(value:String) {if(!submittedRP160 && state.localExtrasScope()==owner && state.assistantEnabledRP155 && value.isNotBlank()) {submittedRP160=true;ime?.hide();submit(value.trim().take(500))}}
    val voice=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
        listening=false
        if(state.localExtrasScope()==owner) {
            if(result.resultCode==Activity.RESULT_OK) {
                val value=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
                if(value.isNotBlank()) finish(value) else {keyboard=true;status=tr("No se escuchó nada. Escribe tu solicitud.","Nothing heard. Type your request.")}
            } else close()
        }
    }
    fun dictate() {
        try {listening=true;voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE,com.miflix.native2.data.BrunioProviderRP161.voiceLocale(context,state.voiceLocaleRP81))
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)
            .putExtra(RecognizerIntent.EXTRA_PROMPT,tr("¿Qué hacemos en BruniO?","What shall we do in BruniO?")))}
        catch(_:Exception) {listening=false;keyboard=true;status=tr("Dictado no disponible. Usa el teclado.","Dictation unavailable. Use the keyboard.")}
    }
    LaunchedEffect(Unit) {if(!fire) dictate()}
    LaunchedEffect(keyboard) {if(keyboard) {withFrameNanos {};focus.requestFocus();ime?.show()}}
    DisposableEffect(Unit) {onDispose {ime?.hide()}}
    BackHandler(onBack=close)
    // Overlay only: the current destination and its scroll/focus state remain mounted.
    androidx.compose.ui.window.Dialog(onDismissRequest=close,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopEnd) {
        Column(Modifier.padding(top=76.dp,end=24.dp).width(440.dp).background(Color(0xff101010),RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text("✦ BruniO Assistant",color=Color.White,fontSize=22.sp,modifier=Modifier.weight(1f));FocusButton("×",onClick=close)}
            if(keyboard) BasicTextField(text,{text=it.take(500)},modifier=Modifier.fillMaxWidth().focusRequester(focus).background(Color(0xff242424),RoundedCornerShape(12.dp)).padding(16.dp),textStyle=TextStyle(color=Color.White,fontSize=18.sp),cursorBrush=SolidColor(Color.White),singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={finish(text)},onDone={finish(text)}))
            Text(status.ifBlank {if(listening) tr("Escuchando…","Listening…") else tr("Pide una búsqueda o una función de la app.","Ask for a search or an app action.")},color=Muted,fontSize=14.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                if(keyboard) FocusButton(tr("Buscar / Hacer","Search / Run")) {finish(text)}
                FocusButton(tr("Micrófono","Microphone")) {dictate()}
                if(!keyboard) FocusButton(tr("Teclado","Keyboard")) {keyboard=true}
            }
        }
    }
    }
}
