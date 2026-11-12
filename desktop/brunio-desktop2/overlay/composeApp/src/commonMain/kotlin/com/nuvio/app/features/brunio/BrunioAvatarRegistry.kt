package com.nuvio.app.features.brunio

import com.nuvio.app.features.profiles.AvatarCatalogItem

internal expect fun brunioBuiltInAvatarList(): List<Pair<String, String>>
internal expect fun brunioAvatarLocalUrl(id: String): String?

/** Same avatar IDs as BruniO TV: ai:* and pack:* entries, not Nuvio RPC IDs. */
internal object BrunioAvatarRegistry {
    fun catalog(): List<AvatarCatalogItem> = brunioBuiltInAvatarList().mapIndexed { index, (id, name) ->
        AvatarCatalogItem(
            id = id,
            displayName = name,
            category = if (id.startsWith("ai:")) "BruniO Original" else
                id.substringAfter("pack:").substringBefore(':').uppercase(),
            sortOrder = index,
            storagePath = "",
            localImageUrl = brunioAvatarLocalUrl(id),
        )
    }
}
