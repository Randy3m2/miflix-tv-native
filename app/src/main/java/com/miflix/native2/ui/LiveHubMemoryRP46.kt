package com.miflix.native2.ui

/** Bounded session memory: survives leaving the hub for playback, isolated by account/profile. */
internal object LiveHubMemoryRP46 {
    private data class Entry(var section:String="hub",val categories:MutableMap<String,String> = mutableMapOf())
    private val entries=linkedMapOf<String,Entry>()
    private fun entry(owner:String):Entry {
        val value=entries.remove(owner) ?: Entry();entries[owner]=value
        while(entries.size>6) entries.remove(entries.keys.first())
        return value
    }
    fun section(owner:String)=entry(owner).section
    fun section(owner:String,value:String) {entry(owner).section=value}
    fun category(owner:String,section:String)=entry(owner).categories[section]
    fun category(owner:String,section:String,value:String) {entry(owner).categories[section]=value}
}
