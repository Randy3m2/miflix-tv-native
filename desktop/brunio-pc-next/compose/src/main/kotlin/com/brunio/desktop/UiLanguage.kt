package com.brunio.desktop
import androidx.compose.runtime.*
import org.json.JSONObject

val LocalUiLanguage=staticCompositionLocalOf{"es"}
object UiStrings {
    private val english:Map<String,String> by lazy {
        val stream=checkNotNull(UiStrings::class.java.getResourceAsStream("/i18n-en.json"))
        val json=stream.bufferedReader(Charsets.UTF_8).use{JSONObject(it.readText())}
        json.keys().asSequence().associateWith{json.getString(it)}
    }
    fun translate(text:String,language:String):String=if(language=="en")english[text]?:text else text
}
@Composable fun ui(text:String)=UiStrings.translate(text,LocalUiLanguage.current)
