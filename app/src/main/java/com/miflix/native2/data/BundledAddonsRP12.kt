package com.miflix.native2.data

internal object BundledAddonsRP12 {
    val defaults=listOf(
        "https://peerflix.mov/manifest.json",
        "https://addonlatam.duckdns.org/A0wrYOb7z4LiG5vrhQwcb-g4/manifest.json",
        "https://torrentio.strem.fun/sort=qualitysize%7Clanguage=latino,spanish/manifest.json"
    )
    val retired=setOf(
        "https://mg.homelabx.qzz.io/manifest.json",
        "https://progreso-latino.xyz/api/addon/manifest.json",
        "https://plus.nuvehub.app/zWdrusnz49s0vtAquiLXDbPwzVCnwZPf-nIed12NG5gYuDhQ5GhmMeRBo861VDX3/manifest.json"
    )
    fun effective(values:List<String>,removed:Set<String> = emptySet(),restored:Set<String> = emptySet()):List<String> =
        (values.map {it.replace("|","%7C")}+defaults).distinct().filterNot {
            (it in retired && it !in restored) || (it in defaults && it in removed)
        }
}
