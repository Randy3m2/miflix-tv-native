package com.miflix.native2.data

internal object TvInputPolicyRP143 {
    fun systemKeyboard(manufacturer:String,model:String)=manufacturer.equals("Amazon",true) || model.startsWith("AFT",true)
    const val ERROR_VISIBLE_MS=8000L
}
