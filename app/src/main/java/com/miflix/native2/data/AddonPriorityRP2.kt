package com.miflix.native2.data

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

internal data class InstalledSourceAddonRP2(val id:String,val name:String,val host:String)

/** Preferences contain hashes, never manifest URLs or access tokens. */
internal object AddonPriorityRP2 {
    fun id(manifest:String)=MessageDigest.getInstance("SHA-256").digest(manifest.toByteArray(Charsets.UTF_8)).joinToString("") {"%02x".format(it)}
    fun language(value:String)=if(StreamLanguagePriority.prefersSpanish(value)) "es" else "en"
    private fun normalized(value:String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"),"")
    fun defaultRank(addon:InstalledSourceAddonRP2,language:String):Int {
        val name=normalized(addon.name+" "+addon.host)
        val comet="comet" in name;val torrentio="torrentio" in name
        if(AddonPriorityRP2.language(language)=="en") return if(comet) 0 else if(torrentio) 1 else 10
        return when {
            "deepflix" in name || addon.host.equals("mg.homelabx.qzz.io",ignoreCase=true) -> -1
            "latadd" in name ->0
            "addonlatam" in name ->1
            "progresolatino" in name ->3
            "umbra" in name ->4
            "nuvedebrid" in name ->5
            "nuve" in name ->6
            torrentio ->7
            comet ->8
            else ->10
        }
    }
    fun ordered(installed:List<InstalledSourceAddonRP2>,saved:List<String>,language:String):List<InstalledSourceAddonRP2> {
        val defaults=installed.distinctBy {it.id}.sortedBy {defaultRank(it,language)}
        val known=defaults.associateBy {it.id}
        return saved.distinct().mapNotNull {known[it]}+defaults.filter {it.id !in saved}
    }
}
