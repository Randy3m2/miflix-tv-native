package com.miflix.native2.data

import java.net.URI

internal object UpdatePolicy {
    const val MAX_BYTES=400L*1024*1024
    fun newer(candidate: String,current: String): Boolean {
        fun parts(value: String)=Regex("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-rc(\\d+))?$").matchEntire(value.trim())
        val a=parts(candidate) ?: return false
        val b=parts(current) ?: return false
        val aa=(1..3).map { a.groupValues[it].toLongOrNull() ?: 0L }
        val bb=(1..3).map { b.groupValues[it].toLongOrNull() ?: 0L }
        for(i in aa.indices) if(aa[i]!=bb[i]) return aa[i]>bb[i]
        val ar=a.groupValues[4].toLongOrNull() ?: Long.MAX_VALUE
        val br=b.groupValues[4].toLongOrNull() ?: Long.MAX_VALUE
        return ar>br
    }
    fun releaseUrl(value: String): Boolean = runCatching {
        val u=URI(value)
        u.scheme=="https" && u.host.equals("github.com",true) && u.userInfo==null && u.port==-1 &&
            u.path.lowercase(java.util.Locale.ROOT).startsWith("/randy3m2/miflix-tv-native/releases/download/") &&
            !u.path.contains("..")
    }.getOrDefault(false)
    fun downloadHost(value: String): Boolean = runCatching {
        val u=URI(value)
        u.scheme=="https" && u.userInfo==null && u.port==-1 && u.host.lowercase(java.util.Locale.ROOT) in
            setOf("github.com","release-assets.githubusercontent.com","objects.githubusercontent.com")
    }.getOrDefault(false)
}
