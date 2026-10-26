package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.miflix.native2.data.ScoreTeamRP11
import androidx.compose.ui.platform.LocalContext

/** Full name remains available to accessibility; only the logo is visible. */
@Composable
internal fun StatsTeamLogoRP132(team:ScoreTeamRP11,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val request=remember(team.logo,context) {ImageRequest.Builder(context).data(team.logo).size(80,80).build()}
    var failed by remember(team.logo) {mutableStateOf(team.logo.isBlank())}
    Box(modifier.semantics {contentDescription=team.name},contentAlignment=Alignment.Center) {
        if(failed) Text("◈",color=Color.White,fontSize=16.sp)
        else AsyncImage(request,null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit,onError={failed=true})
    }
}

@Composable
internal fun StatsTeamButtonRP132(team:ScoreTeamRP11,isSelected:Boolean,modifier:Modifier=Modifier,click:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    Box(modifier.height(if(LocalMobileBrunioRP133.current) 46.dp else 30.dp).clip(RoundedCornerShape(7.dp)).onFocusChanged {focused=it.isFocused}
        .semantics {contentDescription=team.name;selected=isSelected}
        .background(GlassActionBrushRP108(focused,isSelected))
        .border(1.dp,if(focused) Color.White else if(isSelected) Color(0xFF9386AF) else Color(0x445E6878),RoundedCornerShape(7.dp))
        .clickable(role=Role.Button,onClick=click).padding(3.dp),contentAlignment=Alignment.Center) {
        StatsTeamLogoRP132(team,Modifier.size(24.dp))
    }
}
