package com.miflix.native2.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences

/** Guest writes never reach a disk-backed preferences file or the host account. */
internal class GuestContextRP150(base:Context,language:String):ContextWrapper(base) {
    private val stores=mutableMapOf<String,SharedPreferences>()
    override fun getApplicationContext():Context=this
    @Synchronized override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=stores.getOrPut(name) {MemoryPreferencesRP150()}
    init {
        getSharedPreferences("miflix_playback",0).edit().putString("interface",language).putBoolean("remember_last_profile",true).apply()
        getSharedPreferences("miflix_native2",0).edit().putString("offline_profiles","""[{"id":"guest-rp150","name":"Guest","primary":true}]""").apply()
    }
}
internal class MemoryPreferencesRP150:SharedPreferences {
    private val values=linkedMapOf<String,Any?>()
    @Synchronized override fun getAll():MutableMap<String,*> = values.mapValues {(_,v)->if(v is Set<*>) v.toSet() else v}.toMutableMap()
    @Synchronized override fun getString(key:String?,defValue:String?)=values[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST") @Synchronized override fun getStringSet(key:String?,defValues:MutableSet<String>?):MutableSet<String>?=(values[key] as? Set<String>)?.toMutableSet() ?: defValues?.toMutableSet()
    @Synchronized override fun getInt(key:String?,defValue:Int)=values[key] as? Int ?: defValue
    @Synchronized override fun getLong(key:String?,defValue:Long)=values[key] as? Long ?: defValue
    @Synchronized override fun getFloat(key:String?,defValue:Float)=values[key] as? Float ?: defValue
    @Synchronized override fun getBoolean(key:String?,defValue:Boolean)=values[key] as? Boolean ?: defValue
    @Synchronized override fun contains(key:String?)=values.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener:SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener:SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun edit():SharedPreferences.Editor=object:SharedPreferences.Editor {
        val pending=linkedMapOf<String,Any?>();var clear=false
        override fun putString(k:String?,v:String?)=apply {if(k!=null) pending[k]=v}
        override fun putStringSet(k:String?,v:MutableSet<String>?)=apply {if(k!=null) pending[k]=v?.toSet()}
        override fun putInt(k:String?,v:Int)=apply {if(k!=null) pending[k]=v}
        override fun putLong(k:String?,v:Long)=apply {if(k!=null) pending[k]=v}
        override fun putFloat(k:String?,v:Float)=apply {if(k!=null) pending[k]=v}
        override fun putBoolean(k:String?,v:Boolean)=apply {if(k!=null) pending[k]=v}
        override fun remove(k:String?)=apply {if(k!=null) pending[k]=null}
        override fun clear()=apply {clear=true}
        override fun commit():Boolean {synchronized(this@MemoryPreferencesRP150) {if(clear) values.clear();pending.forEach {(k,v)->if(v==null) values.remove(k) else values[k]=v}};return true}
        override fun apply() {commit()}
    }
}
