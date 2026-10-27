package com.brunio.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import java.awt.Robot
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/** Explicit fixture gallery for CI screenshots. It never loads a user account or posts data. */
fun visualSmoke(directory:Path)=application {
    var scene by remember{mutableStateOf(0)}
    val routes=DesktopTab.entries
    val bounds=GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
    val width=(bounds.width-40).coerceIn(800,1440);val height=(bounds.height-80).coerceIn(600,900)
    val state=rememberWindowState(width=width.dp,height=height.dp,position=WindowPosition(20.dp,20.dp))
    Window(onCloseRequest=::exitApplication,state=state,title="BruniO · UI fixture review") {
        LaunchedEffect(Unit){applyWindowTheme(window)}
        BrunioTheme {
            val art="asset:brunio_splash.png"
            val cards=(1..12).map{MediaTile("test:$it","Título de prueba $it",art,"2026",if(it<4).4f else null)}
            val tab=routes.getOrNull(scene)
            Surface(Modifier.fillMaxSize(),color=Color(0xFF080809),contentColor=Color.White) {
            Box(Modifier.fillMaxSize()) {
                if(tab!=null) {
                    Box(Modifier.fillMaxSize().padding(start=76.dp)) {
                        DesktopScreen(tab,DesktopScreenState(
                            hero=MediaTile("test:hero","Una nueva forma de ver",art,"Datos de prueba para revisar proporciones, contraste y controles."),
                            rows=listOf(ContentRow("Seguir viendo",cards,true),ContentRow("Streaming",cards,true),ContentRow("Para ti",cards)),
                            tiles=cards,entries=listOf(FeedEntry("one","Evento de prueba","2026-10-20T20:00:00Z"),FeedEntry("two","@usuario","En línea","Ver"))),{}, {})
                    }
                    DesktopNavigation(tab,{},{})
                } else if(scene==routes.size) ProfilePicker(listOf(
                    MediaTile("a","Principal","asset:brunio_avatar_astronaut.png"),MediaTile("b","Randy","asset:brunio_avatar_fox.png"),MediaTile("c","Kids","asset:brunio_avatar_robot.png")),{}, {})
                else TvPlayerControls(PlaybackUi("Revisión visual del reproductor",source="Fuente de prueba · 1080p",paused=false,position=65000,duration=3600000,seekable=true),{},{},{})
            }
            }
            LaunchedEffect(Unit) {
                Files.createDirectories(directory)
                for(index in 0 until routes.size+2) {
                    scene=index;delay(1500)
                    withContext(Dispatchers.IO) {
                        val image=Robot().createScreenCapture(window.bounds)
                        val name=routes.getOrNull(index)?.name ?: if(index==routes.size)"Profiles" else "Player"
                        ImageIO.write(image,"png",directory.resolve("$name.png").toFile())
                    }
                }
                exitApplication()
            }
        }
    }
}
