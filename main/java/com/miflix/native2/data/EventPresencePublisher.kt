package com.miflix.native2.data

import android.content.Context
import com.miflix.native2.model.CloudSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.util.UUID

/** Main-thread outbox. One latest intent per account; no heartbeat or timer retries. */
internal class EventPresencePublisher(context: Context, private val social: SocialRepository,
    private val scope: CoroutineScope) {
    private val prefs=context.getSharedPreferences("brunio_presence_events",Context.MODE_PRIVATE)
    private val device=prefs.getString("device",null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("device",it).commit()
    }
    private val desired=mutableMapOf<String,String>()
    private var session: CloudSession?=null
    private var worker: Job?=null
    private var retrySignal=0L

    fun queue(s: CloudSession, title: String, media: String, season: Int, episode: Int,
        room: String?, expiresAt: Long) {
        session=s
        val active=title.isNotBlank()
        val signature=JSONObject().put("active",active).put("title",title).put("media",media)
            .put("s",season).put("e",episode).put("room",room ?: JSONObject.NULL).toString()
        if(desired[s.userId]!=signature) {
            desired[s.userId]=signature
            val sequence=prefs.getLong("sequence",0L)+1
            val body=JSONObject(signature).put("device",device).put("seq",sequence)
                .put("expiry",java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",java.util.Locale.US)
                    .apply { timeZone=java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(expiresAt)))
            // Commit intent before network I/O so process death cannot lose a pending stop.
            prefs.edit().putLong("sequence",sequence).putString("pending:${s.userId}",body.toString()).commit()
        }
        retry(s)
    }

    fun retry(s: CloudSession) {
        session=s
        retrySignal++
        if(worker?.isActive==true) return
        worker=scope.launch {
            while(true) {
                val current=session ?: break
                val key="pending:${current.userId}"
                val raw=prefs.getString(key,null) ?: break
                val signal=retrySignal
                try {
                    social.presenceEvent(current,JSONObject(raw))
                    // A newer event queued during this request must survive its acknowledgement.
                    if(prefs.getString(key,null)==raw) prefs.edit().remove(key).commit()
                } catch(e: CancellationException) { throw e } catch(_: Exception) {
                    if(prefs.getString(key,null)==raw && session?.userId==current.userId && retrySignal==signal) break
                }
            }
        }
    }
}
