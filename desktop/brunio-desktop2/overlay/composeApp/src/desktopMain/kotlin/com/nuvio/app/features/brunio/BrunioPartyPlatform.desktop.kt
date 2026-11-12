package com.nuvio.app.features.brunio

import com.nuvio.app.core.network.ServerConfigurationRepository
import com.nuvio.app.core.network.SupabaseProvider
import io.github.jan.supabase.auth.auth
import java.time.Instant
import java.time.temporal.ChronoUnit

internal actual fun brunioPartyTransport(): BrunioRp1Transport {
    val configuration = ServerConfigurationRepository.active.value
    return BrunioHttpTransport(
        serverUrl = configuration.backendUrl,
        publicKey = configuration.publishableKey,
        accessToken = { SupabaseProvider.client.auth.currentAccessTokenOrNull() },
    )
}

internal actual fun brunioPartyClock(): Pair<String, String> {
    val now = Instant.now()
    return now.toString() to now.plus(8, ChronoUnit.HOURS).toString()
}
