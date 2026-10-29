package com.miflix.native2.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

@Composable
internal fun TvSystemInputRP143(initial:String,submit:(String)->Unit,phone:()->Unit,close:()->Unit) {
    var editor by remember {mutableStateOf<EditText?>(null)}
    val latestSubmit by rememberUpdatedState(submit)
    fun finish() {editor?.let {latestSubmit(it.text.toString().take(256))}}
    DisposableEffect(Unit) {onDispose {editor?.let {(it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(it.windowToken,0)};editor=null}}
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.widthIn(max=620.dp).fillMaxWidth(.88f).heightIn(max=460.dp).verticalScroll(rememberScrollState()).background(Panel,RoundedCornerShape(22.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(tr("Buscar con el teclado del sistema","Search with the system keyboard"),color=Color.White,fontSize=22.sp)
            Text(tr("Con el campo seleccionado, usa el dictado del teclado si está disponible. Si el botón del mando abre Alexa, puedes escribir o usar el QR del celular.","With the field selected, use keyboard dictation if available. If the remote button opens Alexa, type or use your phone's QR keyboard."),color=Muted,fontSize=14.sp)
            AndroidView(factory={context->EditText(context).apply {
                setSingleLine(true);setTextColor(android.graphics.Color.WHITE);setHintTextColor(android.graphics.Color.LTGRAY)
                hint=tr("Título, actor o director","Title, actor or director")
                inputType=android.text.InputType.TYPE_CLASS_TEXT
                imeOptions=EditorInfo.IME_ACTION_SEARCH
                setText(initial);setSelection(text.length)
                setOnEditorActionListener {_,action,_->if(action==EditorInfo.IME_ACTION_SEARCH || action==EditorInfo.IME_ACTION_DONE) {finish();true} else false}
                editor=this
                postDelayed({if(isAttachedToWindow) {requestFocus();(context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(this,InputMethodManager.SHOW_IMPLICIT)}},200)
            }},modifier=Modifier.fillMaxWidth().heightIn(min=58.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusButton(tr("Buscar","Search"),primary=true) {finish()}
                FocusButton(tr("Usar celular · QR","Use phone · QR"),onClick=phone)
                FocusButton(tr("Cerrar","Dismiss"),onClick=close)
            }
        }
    }
}
