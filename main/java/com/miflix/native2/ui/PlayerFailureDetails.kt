package com.miflix.native2.ui

internal fun playerFailureDetails(error: androidx.media3.common.PlaybackException): String {
    val causes=generateSequence(error.cause) {it.cause}.take(5).toList()
    val http=causes.filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>().firstOrNull()
    val detail=causes.lastOrNull()?.let {
        val message=it.message.orEmpty().replace(Regex("https?://[^\\s]+"),"[source]")
            .replace(Regex("(?i)(token|key|authorization|password)\\s*[=:]\\s*[^\\s,;]+"),"[private]")
        it.javaClass.simpleName+if(message.isBlank()) "" else ": "+message.take(220)
    }.orEmpty()
    val site=causes.lastOrNull()?.stackTrace?.firstOrNull {it.className.startsWith("androidx.media3")}
        ?.let {"${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}"}
    return listOf(error.errorCodeName,http?.let {"HTTP ${it.responseCode}"},detail.takeIf {it.isNotBlank()},site).filterNotNull().joinToString(" · ")
}
