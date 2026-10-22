package com.miflix.native2.data

import android.content.Context

/** Local settings only. A configured custom manifest is preserved across updates. */
object SportsSourceRP71 {
    const val DEFAULT=SportsPolicyRP71.DEFAULT
    fun valid(raw:String)=SportsPolicyRP71.validManifest(raw)
    fun read(context:Context):String {
        val prefs=context.getSharedPreferences("miflix_live",Context.MODE_PRIVATE)
        return SportsPolicyRP71.configured(prefs.getString("sports_manifest",null))
    }
    fun save(context:Context,manifest:String) {
        require(valid(manifest)) {"Use a valid HTTPS manifest"}
        context.getSharedPreferences("miflix_live",Context.MODE_PRIVATE).edit().putString("sports_manifest",manifest.trim()).apply()
    }
}
