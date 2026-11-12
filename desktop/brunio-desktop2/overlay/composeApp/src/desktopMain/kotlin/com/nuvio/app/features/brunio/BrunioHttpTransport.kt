package com.nuvio.app.features.brunio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Authenticated BruniO RP1 REST transport, independent of Nuvio's Party API.
 * Token is supplied fresh per call; never persisted or included in log messages.
 */
internal class BrunioHttpTransport(
    serverUrl: String,
    private val publicKey: String,
    private val accessToken: () -> String?,
    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(15))
        .build(),
) : BrunioRp1Transport {
    private val origin = URI(serverUrl.trimEnd('/')).also { url ->
        require(url.scheme == "https" && !url.host.isNullOrBlank()) { "BruniO requires HTTPS" }
        require(url.userInfo == null && url.query == null && url.fragment == null)
        require(url.path.isNullOrEmpty() || url.path == "/")
    }.toASCIIString().trimEnd('/')

    init {
        require(publicKey.isNotBlank() && publicKey.none { it == '\r' || it == '\n' })
    }

    override suspend fun execute(call: BrunioRp1Call): JsonElement = withContext(Dispatchers.IO) {
        require(call.profileId.isNotBlank() && call.profileId.length <= 160)
        require(call.profileId.none { it.isISOControl() })
        val method = call.method.uppercase()
        require(method in setOf("GET", "POST", "PATCH", "DELETE"))
        val path = call.path
        require(path.isNotBlank() && !path.startsWith('/') && !path.contains(".."))
        require(path.none { it.isISOControl() || it == '#' || it == '\\' })
        require(path.startsWith("rpc/") || path.startsWith("rp1_miflix_") || path.startsWith("miflix_user_state"))
        require(path.count { it == '?' } <= 1)
        val token = accessToken()?.trim().orEmpty()
        check(token.isNotEmpty() && token.none { it.isWhitespace() }) { "Sign in to BruniO to continue" }
        val uri = URI("$origin/rest/v1/$path")
        check(uri.scheme == "https" && uri.host == URI(origin).host && uri.rawFragment == null)
        val builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(25))
            .header("Accept", "application/json")
            .header("apikey", publicKey)
            .header("Authorization", "Bearer $token")
            .header("x-brunio-profile", call.profileId)
            .header("Cache-Control", "no-store")
        val requestBody = call.jsonBody?.toString()
        if (requestBody != null) builder.header("Content-Type", "application/json")
        if (method == "POST" || method == "PATCH") builder.header("Prefer", "return=representation")
        val publisher = if (requestBody == null) HttpRequest.BodyPublishers.noBody()
            else HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)
        val response = client.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofInputStream())
        val bytes = response.body().use { it.readNBytes(4 * 1024 * 1024 + 1) }
        if (response.statusCode() !in 200..299) {
            error("BruniO REST request failed; see HTTP status in a trusted local debugger")
        }
        check(bytes.size <= 4 * 1024 * 1024) { "BruniO response exceeded size limit" }
        val raw = String(bytes, StandardCharsets.UTF_8)
        if (raw.isBlank()) JsonNull else Json.parseToJsonElement(raw)
    }
}
