package com.miflix.native2.data

/** Keeps configured add-on paths and raw query parameters intact for catalog/stream requests. */
internal object LiveManifestUrlRP49 {
    fun resource(manifest:String,path:String):String {
        val clean=manifest.trim().substringBefore('#')
        val base=clean.substringBefore('?').removeSuffix("/manifest.json").trimEnd('/')
        val query=if('?' in clean) clean.substringAfter('?') else ""
        return base+"/"+path.trimStart('/')+if(query.isNotEmpty()) "?"+query else ""
    }
}
