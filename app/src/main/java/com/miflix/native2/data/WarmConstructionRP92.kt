package com.miflix.native2.data

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Serialize construction and retain ownership until a valid, configured resource is returned. */
internal class WarmConstructionRP92(private val worker:CoroutineDispatcher,private val owner:CoroutineDispatcher) {
    private val serial=Mutex()
    suspend fun <R:Any,C:Any> run(build:()->Pair<R,C>,valid:()->Boolean,configure:(R,C)->R,release:(R)->Unit):R=serial.withLock {
        val constructed=AtomicReference<Pair<R,C>?>(null)
        try {
            withContext(worker) {
                currentCoroutineContext().ensureActive()
                constructed.set(build())
            }
            val result=withContext(owner) {
                currentCoroutineContext().ensureActive()
                if(!valid()) throw CancellationException("Video preparation changed")
                val resource=checkNotNull(constructed.get())
                configure(resource.first,resource.second)
            }
            currentCoroutineContext().ensureActive()
            constructed.set(null)
            result
        } catch(e:Exception) {
            // Even a cancelled blocking constructor may have produced a resource: release it on its owner.
            withContext(NonCancellable+owner) {constructed.getAndSet(null)?.let {release(it.first)}}
            throw e
        }
    }
}
