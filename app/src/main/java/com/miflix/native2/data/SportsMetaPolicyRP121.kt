package com.miflix.native2.data
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/** Preserve real add-on dates instead of dropping them during catalog decoding. */
internal object SportsMetaPolicyRP121 {
    fun start(meta:JSONObject):String {
        for(key in listOf("released","startsAt","startTime","start_time","eventDate","date","startTimestamp","timestamp","releaseInfo")) {
            val raw=meta.opt(key)?.takeUnless {it==JSONObject.NULL}?.toString()?.trim().orEmpty()
            if(raw.isBlank() || raw.length>200) continue
            raw.toLongOrNull()?.let {number ->
                val millis=if(number in 946684800L..4102444800L) number*1000L else number
                if(millis in 946684800000L..4102444800000L) return Instant.ofEpochMilli(millis).toString()
            }
            runCatching {OffsetDateTime.parse(raw).toInstant().toString()}.getOrNull()?.let {return it}
            runCatching {LocalDate.parse(raw).toString()}.getOrNull()?.let {return it}
        }
        return ""
    }
}
