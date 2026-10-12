package com.miflix.native2.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

/** Local device preference. Content names and user chat are never machine-translated. */
object UiLanguage {
    var code by mutableStateOf("en")
}
fun tr(es: String,en: String): String = if(UiLanguage.code=="es") es else en
