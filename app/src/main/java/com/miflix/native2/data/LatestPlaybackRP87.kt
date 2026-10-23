package com.miflix.native2.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope

/** Only the newest source lookup may finish; cancels its own child scope, never the app scope. */
internal class LatestPlaybackRP87 {
    private var active:Job?=null
    suspend fun <T> run(block:suspend CoroutineScope.()->T):T=coroutineScope {
        val job=coroutineContext[Job]!!
        val previous=active
        active=job
        if(previous!==job) previous?.cancel()
        try {block()} finally {if(active===job) active=null}
    }
    fun cancel() {val previous=active;active=null;previous?.cancel()}
}
