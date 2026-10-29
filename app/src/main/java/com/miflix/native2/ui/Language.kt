package com.miflix.native2.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

/** Local device preference. Content names and user chat are never machine-translated. */
object UiLanguage {
    var code by mutableStateOf("en")
}
fun tr(es: String,en: String): String = when(UiLanguage.code) {"es" -> es; "fr","ko" -> AppLanguagesRP148.translate(UiLanguage.code,en);else -> en}
