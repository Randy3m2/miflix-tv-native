package com.miflix.native2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.miflix.native2.data.ContentAdvisoriesRP31
import com.miflix.native2.data.ContentGuideClientRP33
import com.miflix.native2.data.HttpFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ContentGuideDiagnosticsRP33() {
    val scope=rememberCoroutineScope()
    val context=LocalContext.current.applicationContext
    var showing by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var status by remember {mutableStateOf("")}
    SettingsTileRC103(tr("Comprobar avisos de contenido","Check content warnings"),
        tr("Prueba la fuente desde esta TV y muestra el resultado real.","Test the source from this TV and show the actual result."),"lists") {showing=true}
    if(showing) PlayerMenuDialog(onDismissRequest={showing=false}) {
        Column(Modifier.widthIn(max=560.dp).background(Color(0xFF101010)).padding(26.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(tr("Estado de la guía de contenido","Content guide status"),color=Color.White)
            Text(tr("Consulta directa, sin usar la caché. Un error no significa que el título no tenga advertencias.","Direct request, bypassing cache. An error does not mean the title has no advisories."),color=Muted)
            fun test(id:String,title:String) {
                if(busy) return
                busy=true;status=tr("Consultando ","Checking ")+title+"…"
                scope.launch {
                    try {
                        val result=ContentAdvisoriesRP31.parse(ContentGuideClientRP33.fetch(id))
                        status=title+": "+if(result.isEmpty()) tr("La fuente respondió, sin avisos con votos suficientes.","Source responded; no advisories with sufficient votes.") else tr("La fuente respondió. Categorías: ","Source responded. Categories: ")+result.joinToString {it.category+" · "+it.severity}
                    } catch(_:TimeoutCancellationException) {status=tr("La fuente no respondió a tiempo.","Source timed out.")} catch(e:CancellationException) {throw e} catch(e:HttpFailure) {status="HTTP ${e.status} · "+tr("La fuente no está disponible desde esta TV.","The source is unavailable from this TV.")} catch(_:Exception) {status=tr("No se pudo leer la respuesta de la fuente.","Could not read the source response.")}
                    finally {busy=false}
                }
            }
            FocusButton(tr("Probar Lanterns","Test Lanterns")) {test("tt26545992","Lanterns")}
            FocusButton(tr("Probar The Shawshank Redemption","Test The Shawshank Redemption")) {test("tt0111161","The Shawshank Redemption")}
            if(status.isNotBlank()) Text(status,color=Color.White)
            FocusButton(tr("Borrar caché de avisos","Clear guide cache")) {
                if(!busy) {
                    context.getSharedPreferences("brunio_content_guide_rp31",0).edit().clear().apply()
                    context.getSharedPreferences("brunio_content_guide_rp33",0).edit().clear().apply()
                    status=tr("Caché borrada. La próxima reproducción consultará la fuente.","Cache cleared. The next playback will request fresh data.")
                }
            }
            FocusButton(tr("Cerrar","Close")) {showing=false}
        }
    }
}
