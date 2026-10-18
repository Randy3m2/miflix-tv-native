package com.miflix.native2.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

internal data class SettingsHelpRC103(val title:String,val description:String,val icon:String,val value:String)
internal val LocalSettingsHelpRC103=staticCompositionLocalOf<((SettingsHelpRC103)->Unit)?> {null}

private val SettingsAccentRC103=Color(0xFF7CE0D3)

/** Code-drawn icons: sharp on TV, no downloads or additional dependency. */
@Composable
internal fun SettingsIconRC103(kind:String) {
    Box(Modifier.size(44.dp).background(SettingsAccentRC103.copy(alpha=0.12f),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
        Canvas(Modifier.size(26.dp)) {
            val c=SettingsAccentRC103;val w=size.width;val h=size.height;val sw=2.dp.toPx()
            fun line(x:Float,y:Float,x2:Float,y2:Float)=drawLine(c,Offset(w*x,h*y),Offset(w*x2,h*y2),sw)
            when(kind) {
                "sleep" -> {drawCircle(c,w*0.34f,center,style=Stroke(sw));line(.5f,.5f,.5f,.25f);line(.5f,.5f,.7f,.6f)}
                "still" -> {drawCircle(c,w*.38f,center,style=Stroke(sw));line(.4f,.32f,.4f,.68f);line(.6f,.32f,.6f,.68f)}
                "hide" -> {drawOval(c,Offset(w*.08f,h*.27f),Size(w*.84f,h*.46f),style=Stroke(sw));drawCircle(c,w*.1f,center);line(.12f,.88f,.88f,.12f)}
                "filters" -> {line(.1f,.25f,.9f,.25f);line(.25f,.5f,.75f,.5f);line(.4f,.75f,.6f,.75f)}
                "tracks" -> {line(.18f,.4f,.35f,.4f);line(.18f,.6f,.35f,.6f);line(.18f,.4f,.18f,.6f);line(.35f,.4f,.55f,.2f);line(.35f,.6f,.55f,.8f);line(.55f,.2f,.55f,.8f);drawArc(c,-55f,110f,false,Offset(w*.4f,h*.2f),Size(w*.5f,h*.6f),style=Stroke(sw))}
                "technical","stats" -> {line(.2f,.8f,.2f,.48f);line(.5f,.8f,.5f,.2f);line(.8f,.8f,.8f,.36f)}
                "backup" -> {line(.15f,.72f,.15f,.87f);line(.15f,.87f,.85f,.87f);line(.85f,.87f,.85f,.72f);line(.5f,.12f,.5f,.66f);line(.28f,.44f,.5f,.66f);line(.72f,.44f,.5f,.66f)}
                "previous" -> {line(.2f,.5f,.8f,.5f);line(.2f,.5f,.46f,.24f);line(.2f,.5f,.46f,.76f);line(.8f,.5f,.8f,.8f)}
                "links" -> {drawRoundRect(c,Offset(w*.08f,h*.3f),Size(w*.5f,h*.4f),androidx.compose.ui.geometry.CornerRadius(w*.15f),style=Stroke(sw));drawRoundRect(c,Offset(w*.42f,h*.3f),Size(w*.5f,h*.4f),androidx.compose.ui.geometry.CornerRadius(w*.15f),style=Stroke(sw))}
                "home" -> {line(.1f,.45f,.5f,.1f);line(.5f,.1f,.9f,.45f);line(.25f,.4f,.25f,.85f);line(.25f,.85f,.75f,.85f);line(.75f,.85f,.75f,.4f)}
                "taste" -> {line(.5f,.08f,.5f,.92f);line(.08f,.5f,.92f,.5f);line(.22f,.22f,.78f,.78f);line(.22f,.78f,.78f,.22f);drawCircle(c,w*.14f,center)}
                else -> {for(i in 0..2) {val y=.25f+i*.25f;drawCircle(c,w*.035f,Offset(w*.15f,h*y));line(.32f,y,.85f,y)}}
            }
        }
    }
}

@Composable
internal fun SettingsHeadingRC103(title:String,subtitle:String) {
    Column(Modifier.fillMaxWidth().padding(top=10.dp,bottom=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(title,color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
        Text(subtitle,color=Color(0xFFA9B7C8),fontSize=13.sp,lineHeight=18.sp)
    }
}

/** Context for compact choices inside the settings dialog. */
@Composable
internal fun Modifier.settingsHelpRC103(title:String,description:String,icon:String,value:String):Modifier {
    var focused by remember {mutableStateOf(false)}
    val hoverSource=remember {MutableInteractionSource()}
    val hovered by hoverSource.collectIsHoveredAsState()
    val help by rememberUpdatedState(LocalSettingsHelpRC103.current)
    LaunchedEffect(focused,hovered,title,description,value) {if(focused || hovered) help?.invoke(SettingsHelpRC103(title,description,icon,value))}
    return onFocusChanged {focused=it.isFocused}.hoverable(hoverSource)
}

/** One focus target for the entire card. Toggle semantics expose its state to accessibility. */
@Composable
internal fun SettingsTileRC103(title:String,description:String,icon:String,modifier:Modifier=Modifier,checked:Boolean?=null,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val hoverSource=remember {MutableInteractionSource()}
    val hovered by hoverSource.collectIsHoveredAsState()
    val active=focused || hovered
    val help by rememberUpdatedState(LocalSettingsHelpRC103.current)
    val value=if(checked==null) tr("Abrir opciones","Open options") else if(checked) tr("Activado","On") else tr("Desactivado","Off")
    LaunchedEffect(active,title,description,checked,icon) {if(active) help?.invoke(SettingsHelpRC103(title,description,icon,value))}
    val edge by animateColorAsState(if(active) Color.White else if(checked==true) SettingsAccentRC103.copy(alpha=.45f) else Color(0xFF2B3549),label="settingsEdge")
    val track by animateColorAsState(if(checked==true) SettingsAccentRC103 else Color(0xFF455065),label="settingsTrack")
    val position by animateFloatAsState(if(checked==true) 1f else 0f,label="settingsThumb")
    val shape=RoundedCornerShape(18.dp)
    val interaction=if(checked==null) Modifier.clickable(role=Role.Button,onClick=onClick) else Modifier
        .semantics {stateDescription=if(checked) tr("Activado","On") else tr("Desactivado","Off")}
        .toggleable(value=checked,role=Role.Switch,onValueChange={onClick()})
    Row(modifier.fillMaxWidth().clip(shape).background(Brush.horizontalGradient(listOf(if(active) Color(0xFF27394E) else Color(0xFF182133),Color(0xFF101725))))
        .border(if(active) 2.dp else 1.dp,edge,shape).onFocusChanged {focused=it.isFocused}.hoverable(hoverSource).then(interaction).padding(16.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        SettingsIconRC103(icon)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(title,color=Color.White,fontSize=17.sp,lineHeight=22.sp,fontWeight=FontWeight.SemiBold)
            Text(description,color=Color(0xFFB6C3D4),fontSize=13.sp,lineHeight=18.sp)
        }
        if(checked!=null) Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Canvas(Modifier.size(44.dp,26.dp)) {
                drawRoundRect(track,cornerRadius=androidx.compose.ui.geometry.CornerRadius(size.height/2))
                val radius=size.height/2-3.dp.toPx()
                drawCircle(if(checked) Color(0xFF092522) else Color.White,radius,Offset(size.height/2+(size.width-size.height)*position,size.height/2))
            }
            Text(if(checked) tr("ACTIVO","ON") else tr("INACTIVO","OFF"),color=if(checked) SettingsAccentRC103 else Color(0xFFA9B7C8),fontSize=10.sp,fontWeight=FontWeight.Bold)
        } else Text("›",color=if(active) Color.White else SettingsAccentRC103,fontSize=30.sp)
    }
}

@Composable
private fun SettingsHelpPanelRC103(help:SettingsHelpRC103,modifier:Modifier=Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF101B2B)).border(1.dp,Color(0xFF30445C),RoundedCornerShape(18.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        SettingsIconRC103(help.icon)
        Text(tr("ASÍ FUNCIONA","HOW IT WORKS"),color=SettingsAccentRC103,fontSize=11.sp,fontWeight=FontWeight.Bold)
        Text(help.title,color=Color.White,fontSize=21.sp,lineHeight=26.sp,fontWeight=FontWeight.Bold)
        Text(help.description,color=Color(0xFFB6C3D4),fontSize=14.sp,lineHeight=21.sp)
        if(help.value.isNotBlank()) Text(help.value,color=SettingsAccentRC103,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
    }
}

@Composable
internal fun SettingsDialogRC103(title:String,subtitle:String,close:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) {
            val first=remember {FocusRequester()}
            var help by remember {mutableStateOf(SettingsHelpRC103(title,subtitle,"taste",""))}
            // Reserve a readable side panel on TV; compact windows show help above the list.
            val wide=maxWidth>=720.dp
            LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()}}
            Column(Modifier.widthIn(max=1120.dp).fillMaxWidth().heightIn(max=maxHeight)
                .clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(Color(0xFF172538),Color(0xFF0C111C))))
                .border(1.dp,Color(0xFF35465C),RoundedCornerShape(26.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text("B R U N I O  /  "+tr("TU EXPERIENCIA","YOUR EXPERIENCE"),color=SettingsAccentRC103,fontSize=10.sp,fontWeight=FontWeight.Bold)
                        Text(title,color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Bold)
                        Text(subtitle,color=Color(0xFFB6C3D4),fontSize=13.sp,lineHeight=18.sp)
                    }
                    FocusButton(tr("Listo","Done"),modifier=Modifier.focusRequester(first)) {close()}
                }
                CompositionLocalProvider(LocalSettingsHelpRC103 provides {help=it}) {
                    if(wide) Row(Modifier.weight(1f,fill=false).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        Column(Modifier.weight(.65f).verticalScroll(rememberScrollState()).focusGroup(),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
                        Column(Modifier.weight(.35f).verticalScroll(rememberScrollState())) {SettingsHelpPanelRC103(help)}
                    } else Column(Modifier.weight(1f,fill=false).fillMaxWidth().verticalScroll(rememberScrollState()).focusGroup(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        SettingsHelpPanelRC103(help)
                        content()
                    }
                }
            }
        }
    }
}
