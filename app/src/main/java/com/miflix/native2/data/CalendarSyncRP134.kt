package com.miflix.native2.data

import org.json.JSONObject

/** Per-subscription tombstones preserve removals across devices and offline edits. */
internal object CalendarSyncRP134 {
    fun merge(local:JSONObject,remote:JSONObject):JSONObject {
        val rows=JSONObject(local.toString())
        remote.keys().forEach {key->
            val incoming=remote.optJSONObject(key) ?: return@forEach
            val old=rows.optJSONObject(key)
            if(old==null || incoming.optLong("at")>old.optLong("at") ||
                (incoming.optLong("at")==old.optLong("at") && !incoming.optBoolean("enabled"))) rows.put(key,JSONObject(incoming.toString()))
        }
        return rows
    }
    fun enabled(rows:JSONObject):Set<String> = rows.keys().asSequence().filter {rows.optJSONObject(it)?.optBoolean("enabled")==true}.toSet()
}
