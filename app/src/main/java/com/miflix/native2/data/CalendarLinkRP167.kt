package com.miflix.native2.data

internal object CalendarLinkRP167 {
 fun matches(value:String?):Boolean=runCatching {java.net.URI(value ?: "").let {it.scheme=="brunio" && it.host=="calendar" && it.userInfo==null && (it.path.isNullOrEmpty() || it.path=="/")}}.getOrDefault(false)
 fun date(value:String?):String?=runCatching {java.net.URI(value ?: "").rawQuery.orEmpty().split('&').firstOrNull {it.startsWith("date=")}?.substringAfter('=')?.let {java.time.LocalDate.parse(it).toString()}}.getOrNull()
}
