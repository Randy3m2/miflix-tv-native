package com.brunio.desktop

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

fun main(args:Array<String>) {
    if("--smoke" in args) {
        try {
            kotlinx.coroutines.runBlocking { BridgeClient().use { it.call("session.restore") } }
            val media=System.getenv("BRUNIO_SMOKE_MEDIA")?:error("Missing native smoke fixture")
            VlcEngine().use { engine ->
                engine.play(media)
                val until=System.nanoTime()+12_000_000_000L
                while(engine.frame.get()==null && !engine.failed.get() && System.nanoTime()<until)Thread.sleep(20)
                check(engine.frame.get()!=null && !engine.failed.get())
            }
            kotlin.system.exitProcess(0)
        } catch(_:Throwable) { System.err.println("Bundled application smoke failed");kotlin.system.exitProcess(1) }
    }
    application {
    val state=rememberWindowState(width=1280.dp,height=800.dp)
    var pip by remember{mutableStateOf(false)}
    var previousSize by remember{mutableStateOf(DpSize(1280.dp,800.dp))}
    var previousPlacement by remember{mutableStateOf(WindowPlacement.Floating)}
    fun restore() { if(pip){pip=false;state.size=previousSize;state.placement=previousPlacement} }
    Window(state=state,alwaysOnTop=pip,onCloseRequest={if(pip)restore() else exitApplication()},title="BruniO PC · Desarrollo") {
        MaterialTheme(colorScheme=darkColorScheme(background=Color(0xFF080809),surface=Color(0xFF151518),primary=Color.White,onPrimary=Color.Black)) {
            ConnectedDesktop(
                onFullscreen={state.placement=if(state.placement==WindowPlacement.Fullscreen)WindowPlacement.Floating else WindowPlacement.Fullscreen},
                onPip={if(pip)restore() else{previousSize=state.size;previousPlacement=state.placement;pip=true;state.placement=WindowPlacement.Floating;state.size=DpSize(560.dp,340.dp)}},
                onPlayerClosed=::restore)
        }
    }
}

}
