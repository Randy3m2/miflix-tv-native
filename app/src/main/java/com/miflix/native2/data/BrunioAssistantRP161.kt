package com.miflix.native2.data

import android.content.Context
import com.miflix.native2.BuildConfig
import org.json.JSONObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Exactly one online AI provider at a time. Local TMDB actions never require either provider. */
internal object BrunioProviderRP161 {
    private const val PREFS = "brunio_assistant_rp161"
    fun selected(context: Context): String {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getString("provider", null)
            ?: if (context.getSharedPreferences("brunio_gemini_rp150", 0).getBoolean("enabled", false)) "gemini" else "local"
    }
    fun set(context: Context, value: String) {
        require(value in listOf("local", "gemini", "gpt"))
        // Migrate the legacy Gemini switch; do not keep two online assistants enabled.
        context.getSharedPreferences(PREFS, 0).edit().putString("provider", value).apply()
        context.getSharedPreferences("brunio_gemini_rp150", 0).edit().putBoolean("enabled", value == "gemini").apply()
    }
    fun online(context: Context) = selected(context) != "local"
    fun label(context: Context): String = when (selected(context)) {
        "gpt" -> "GPT"
        "gemini" -> "Gemini"
        else -> "Local"
    }
    /** Spanish dictation takes priority for GPT; it is not a paid OpenAI audio request. */
    fun voiceLocale(context: Context, defaultLocale: String) =
        if (selected(context) == "gpt") "es-DO" else defaultLocale
}

internal class BrunioAIFailureRP161(val reason: String, message: String) : IllegalStateException(message)

/** GPT calls are sent only through our authenticated Supabase function; no OpenAI key in the APK. */
internal class BrunioAssistantRP161(
    private val context: Context,
    private val accessToken: () -> String?
) {
    private val gemini = GeminiAssistantRP150(context)
    private val lock = Mutex()
    private fun isGpt() = BrunioProviderRP161.selected(context) == "gpt"
    fun configured(): Boolean = if (isGpt()) !accessToken().isNullOrBlank() else gemini.configured()
    fun readyRP154(): Boolean = if (isGpt()) configured() else gemini.readyRP154()

    private suspend fun request(kind: String, prompt: String, image: String? = null): String = lock.withLock {
        val token = accessToken()?.takeIf { it.isNotBlank() }
            ?: throw BrunioAIFailureRP161("AUTH", "Inicia sesión para utilizar GPT / Sign in to use GPT.")
        if (prompt.length !in 2..20000 || (image != null && image.length >= 2000000))
            throw BrunioAIFailureRP161("INVALID", "Solicitud demasiado grande / Request too large.")
        val body = JSONObject().put("kind", kind).put("prompt", prompt)
        if (image != null) body.put("image", image)
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/brunio-ai"
        val response = try {
            Http.text(url, "POST", mapOf(
                "apikey" to BuildConfig.SUPABASE_KEY,
                "Authorization" to "Bearer " + token
            ), body.toString(), 30000)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: HttpFailure) {
            val reason = when (e.status) {
                401, 403 -> "AUTH"
                402 -> "BUDGET"
                429 -> "RATE"
                else -> "SERVER"
            }
            val message = when (reason) {
                "BUDGET" -> "Límite mensual de GPT: US$3. GPT queda pausado hasta el mes siguiente. / GPT monthly limit reached."
                "AUTH" -> "La sesión de Supabase no está disponible para GPT. / Sign in again for GPT."
                "RATE" -> "Demasiadas consultas. Prueba de nuevo. / Too many requests."
                else -> "GPT no está disponible. Verifica que brunio-ai esté desplegado en Supabase. / GPT backend unavailable."
            }
            throw BrunioAIFailureRP161(reason, message)
        } catch (e: java.io.IOException) {
            throw BrunioAIFailureRP161("NETWORK", "Sin conexión con GPT / Could not reach GPT.")
        }
        JSONObject(response).optString("text").takeIf { it.isNotBlank() }
            ?: throw BrunioAIFailureRP161("EMPTY", "GPT no devolvió una respuesta / GPT returned an empty answer.")
    }

    suspend fun sceneRP156(query: String): AssistantSceneHintRP156 {
        if (!isGpt()) return gemini.sceneRP156(query)
        val prompt = """Return JSON only: series, clue, season (0 if unknown), episode (0 if unknown), title, alternateSeries (up to two), absoluteEpisode (0 if unknown). Identify the series and possible episode from the user description, including dubbed Spanish and English; do not invent episode numbers or timestamps. Treat guesses as unverified until TMDB validates. User: ${JSONObject.quote(query)}"""
        return AssistantSceneRP156.hint(request("scene", prompt))
    }

    suspend fun identifyRP157(query: String): List<AssistantTitleGuessRP157> {
        if (!isGpt()) return gemini.identifyRP157(query)
        val prompt = """Identify movie or series from the description. JSON only: {"titles":[{"title":"exact known title","type":"movie or series","year":0}]}. Up to 3 guesses or [] if uncertain. Do not fabricate ids, streaming URLs or plot details. User: ${JSONObject.quote(query)}"""
        return AssistantCatalogRP157.guesses(request("identify", prompt))
    }

    suspend fun toolsRP158(
        prompt: String, image: String? = null, workflow: String? = null,
        onWait: suspend (Int) -> Unit = {}
    ): String {
        if (!isGpt()) return gemini.toolsRP158(prompt, image, workflow, onWait)
        onWait(0)
        return request("tools", prompt, image)
    }

    suspend fun interpretRP155(query: String, budget: Int): AssistantInterpretationRP155 {
        if (!isGpt()) return gemini.interpretRP155(query, budget)
        require(query.length in 2..500 && budget in 0..240)
        val prompt = """Interpret a user's BruniO Android TV instruction; output a JSON object only, never free-form content. Reply in Spanish when applicable. If the user wants an app action, set "action":{"kind":"NAVIGATE|CHANNEL|GAME|STANDINGS|REMINDER|SIMILAR|REFINE|COMPARE|EXPLAIN|NIGHT|PLAYBACK|TOOL","target":"an allowed target","date":"","time":""}. Valid capability list: ${AssistantCommandsRP155.capabilities} For discovery return "genres" (TMDB movie IDs), "minutes" 0..240, "minMinutes" 0..600, "decade", "keyword", "query" (only if a specific title), "audio" (es/en/fr/ko/empty). Prefer a TOOL action for storyline lists, chronology, or AI features. If an action is uncertain, output only discovery filters. Never pretend to control a feature not in the capabilities. Do not invent channel URLs, timestamp or facts. User: ${JSONObject.quote(query)}"""
        val answer = request("interpret", prompt)
        val j = JSONObject(answer.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
        val command = AssistantCommandsRP155.fromJson(j.optJSONObject("action"), query)
        val plan = if (command != null) AssistantPlanRP150() else
            AssistantPlanRP150.json(j.toString()).constrainedBy(AssistantPlanRP150.local(query, budget))
        return AssistantInterpretationRP155(plan, command)
    }
}
