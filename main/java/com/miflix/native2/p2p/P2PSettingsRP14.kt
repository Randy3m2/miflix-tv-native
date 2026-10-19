package com.miflix.native2.p2p

import android.content.Context
import android.os.Build

object P2PSettingsRP14 {
    fun supported()=Build.VERSION.SDK_INT>=28 && Build.SUPPORTED_ABIS.any {it in setOf("arm64-v8a","armeabi-v7a","x86","x86_64")}
    private fun prefs(context:Context)=context.getSharedPreferences("brunio_p2p",Context.MODE_PRIVATE)
    fun enabled(context:Context,scope:String)=supported() && prefs(context).getBoolean("enabled:$scope",false)
    fun saveEnabled(context:Context,scope:String,value:Boolean) {prefs(context).edit().putBoolean("enabled:$scope",value && supported()).apply()}
    fun cacheGb(context:Context)=prefs(context).getInt("cache_gb",4).takeIf {it in listOf(2,4,8,16,32)} ?: 4
    fun saveCacheGb(context:Context,value:Int) {require(value in listOf(2,4,8,16,32));prefs(context).edit().putInt("cache_gb",value).apply()}
    /** Remove leftovers from a killed process; called once at app startup before any player exists. */
    fun cleanStale(context:Context) {java.io.File(context.cacheDir,"p2p-rp14").deleteRecursively()}
}
