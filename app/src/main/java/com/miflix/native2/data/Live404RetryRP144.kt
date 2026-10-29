package com.miflix.native2.data

import kotlinx.coroutines.*

internal class Live404RetryBudgetRP144 {
    var used=0
        private set
    fun nextDelay():Long?=DELAYS.getOrNull(used)?.also {used++}
    fun stable() {used=0}
    companion object {
        private val DELAYS=listOf(1000L,2000L,4000L,8000L)
        const val STABLE_MS=30000L
        fun eligible(live:Boolean,recording:Boolean,provider:String,type:String)=live && !recording && provider!="sports" && type!="youtube"
    }
}

/** One owner/player, main-thread calls. Cancels delayed work before its player is released. */
internal class Live404RetrySessionRP144(
    private val scope:CoroutineScope,private val eligible:Boolean,
    private val owner:()->Boolean,private val allowed:()->Boolean,private val playing:()->Boolean,
    private val reload:()->Unit,private val status:(Int?)->Unit,private val failed:(Throwable)->Unit,
    private val wait:suspend (Long)->Unit={delay(it)}
) {
    private val budget=Live404RetryBudgetRP144()
    private var pending:Job?=null
    private var stable:Job?=null
    private var closed=false
    fun offer(error:Throwable,http:Int?):Boolean {
        if(http!=404 || !eligible) {pending?.cancel();return false}
        if(closed || !owner() || !allowed()) return false
        if(pending?.isActive==true) return true
        val delay=budget.nextDelay() ?: return false
        stable?.cancel()
        pending=scope.launch {
            val self=currentCoroutineContext()[Job]
            try {
                status(budget.used);wait(delay)
                if(!closed && owner()) {if(allowed()) reload() else failed(error)}
            } catch(e:CancellationException) {throw e}
            catch(_:Exception) {if(!closed && owner()) failed(error)}
            finally {if(pending===self) {pending=null;status(null)}}
        }
        return true
    }
    fun onPlaying(value:Boolean) {
        stable?.cancel();stable=null
        if(!value || closed || budget.used==0 || !owner() || !allowed()) return
        stable=scope.launch {
            wait(Live404RetryBudgetRP144.STABLE_MS)
            if(!closed && owner() && allowed() && playing()) budget.stable()
        }
    }
    fun close() {
        if(closed) return
        closed=true;pending?.cancel();stable?.cancel();pending=null;stable=null;status(null)
    }
}
