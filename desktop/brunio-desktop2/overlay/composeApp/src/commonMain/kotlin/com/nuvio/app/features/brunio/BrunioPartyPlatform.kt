package com.nuvio.app.features.brunio

/** Desktop implementations use BruniO's authenticated Supabase session. */
internal expect fun brunioPartyTransport(): BrunioRp1Transport
internal expect fun brunioPartyClock(): Pair<String, String>
