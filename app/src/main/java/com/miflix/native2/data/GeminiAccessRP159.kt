package com.miflix.native2.data

internal enum class GeminiErrorRP159 { COOLDOWN, DAILY_LIMIT, NOT_CONFIGURED, AUTH, PROVIDER_LIMIT, NETWORK, PROVIDER, EMPTY_RESPONSE, INVALID_INPUT, IMAGE_TOO_LARGE }
internal class GeminiFailureRP159(val reason:GeminiErrorRP159,val seconds:Int=0):IllegalStateException(reason.name)
internal object GeminiAccessRP159 {
    const val DAILY_LIMIT=20
    const val INTERVAL_MS=20000L
    fun waitMillis(last:Long,now:Long):Long=if(last==0L || now<last) 0L else (INTERVAL_MS-(now-last)).coerceAtLeast(0L)
    fun check(count:Int,last:Long,now:Long,followup:Boolean=false):GeminiFailureRP159?=when {
        count>=DAILY_LIMIT->GeminiFailureRP159(GeminiErrorRP159.DAILY_LIMIT)
        !followup && waitMillis(last,now)>0->GeminiFailureRP159(GeminiErrorRP159.COOLDOWN,((waitMillis(last,now)+999)/1000).toInt())
        else->null
    }
    fun provider(error:Exception):GeminiFailureRP159=when(error) {
        is GeminiFailureRP159->error
        is HttpFailure->GeminiFailureRP159(when(error.status) {401,403->GeminiErrorRP159.AUTH;429->GeminiErrorRP159.PROVIDER_LIMIT;else->GeminiErrorRP159.PROVIDER})
        is java.io.IOException->GeminiFailureRP159(GeminiErrorRP159.NETWORK)
        else->GeminiFailureRP159(GeminiErrorRP159.PROVIDER)
    }
    fun message(error:GeminiFailureRP159,es:Boolean):String=when(error.reason) {
        GeminiErrorRP159.COOLDOWN->if(es) "Espera ${error.seconds} s entre consultas a Gemini." else "Wait ${error.seconds}s between Gemini requests."
        GeminiErrorRP159.DAILY_LIMIT->if(es) "Alcanzaste el límite local de BruniO: 20 consultas de AI al día. Se restablece mañana. Las funciones locales siguen disponibles." else "BruniO's local limit of 20 AI requests per day has been reached. It resets tomorrow. Local features remain available."
        GeminiErrorRP159.NOT_CONFIGURED->if(es) "Activa y configura Gemini en Settings → Assistant." else "Enable and configure Gemini in Settings → Assistant."
        GeminiErrorRP159.AUTH->if(es) "Google rechazó la clave o el acceso al modelo. Revisa tu clave de Gemini en Settings → Assistant." else "Google rejected the key or model access. Check your Gemini key in Settings → Assistant."
        GeminiErrorRP159.PROVIDER_LIMIT->if(es) "Google devolvió un límite de cuota de Gemini (429). Espera y revisa la cuota de tu proyecto; es independiente del límite local de BruniO." else "Google returned a Gemini quota limit (429). Wait and check your project's quota; this is separate from BruniO's local limit."
        GeminiErrorRP159.NETWORK->if(es) "No se pudo conectar con Gemini. Revisa la conexión y pulsa Reintentar." else "Could not connect to Gemini. Check your connection and select Retry."
        GeminiErrorRP159.EMPTY_RESPONSE->if(es) "Gemini no devolvió un análisis utilizable. Puedes reintentar." else "Gemini returned no usable analysis. You can retry."
        GeminiErrorRP159.IMAGE_TOO_LARGE->if(es) "La captura es demasiado grande. Prueba explicando la opción por texto." else "The screenshot is too large. Try describing the option in text."
        GeminiErrorRP159.INVALID_INPUT->if(es) "La solicitud no es válida para esta función. Prueba una descripción más corta." else "This request is invalid for the tool. Try a shorter description."
        GeminiErrorRP159.PROVIDER->if(es) "Gemini no pudo completar la respuesta. Puedes reintentar; no se cambió tu clave." else "Gemini could not complete the response. You can retry; your key was not changed."
    }
}
