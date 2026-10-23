package com.miflix.native2.data

import android.content.Context
import android.util.Log
import java.io.File

/** One bounded local diagnostic, without exception messages, network calls or account data. */
internal object LocalCrashRP87 {
    private var installed=false
    @Synchronized fun install(context:Context) {
        if(installed) return
        installed=true
        val app=context.applicationContext
        val previous=Thread.getDefaultUncaughtExceptionHandler()
        if(previous==null) return
        Thread.setDefaultUncaughtExceptionHandler {thread,error ->
            try {record(app,error)} finally {previous.uncaughtException(thread,error)}
        }
    }
    fun record(context:Context,error:Throwable) {
        try {
            val text=buildString {
                append("BruniO RP88 / ").append(android.os.Build.VERSION.SDK_INT).append('\n')
                var cause:Throwable?=error
                repeat(4) {
                    val current=cause ?: return@repeat
                    append(current.javaClass.name).append('\n')
                    current.stackTrace.take(20).forEach {append(it.toString()).append('\n')}
                    cause=current.cause?.takeUnless {it===current}
                }
            }.take(8000)
            File(context.filesDir,"last_crash_rp88.txt").writeText(text)
            Log.e("BruniOPlayback",text)
        } catch(_:Throwable) { /* Preserve the platform's original crash handling. */ }
    }
}
