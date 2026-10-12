package com.miflix.native2.data

import com.miflix.native2.model.CloudSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One refresh in flight; all waiting requests reuse the rotated token. */
class SessionRenewal(private val current: () -> CloudSession?,private val save: (CloudSession) -> Unit,
                     private val expiresSoon: (CloudSession) -> Boolean,private val refresh: suspend (String) -> CloudSession) {
    private val mutex=Mutex()
    suspend fun valid(owner: String,failedToken: String?=null): CloudSession = mutex.withLock {
        val before=current()?.takeIf { it.userId==owner } ?: throw CancellationException("Account changed")
        if(!expiresSoon(before) && (failedToken==null || failedToken!=before.accessToken)) return@withLock before
        check(before.refreshToken.isNotBlank()) { "Vuelve a iniciar sesión / Please sign in again" }
        val next=refresh(before.refreshToken)
        val active=current()
        if(active?.userId!=owner || active.refreshToken!=before.refreshToken) throw CancellationException("Account changed during refresh")
        check(next.userId==owner) { "Unexpected session owner" }
        save(next);next
    }
}
