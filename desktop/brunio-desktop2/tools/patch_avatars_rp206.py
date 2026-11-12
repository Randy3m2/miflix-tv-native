"""Use BruniO TV's existing avatar IDs and local images, never Nuvio RPCs."""
from patch_runtime import once

def apply(root):
    app = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    avatars = app / "features/profiles/AvatarRepository.kt"
    once(avatars, "import com.nuvio.app.core.network.ServerConfigurationRepository",
         "import com.nuvio.app.features.brunio.BrunioCloud\n"
         "import com.nuvio.app.features.brunio.BrunioAvatarRegistry\n"
         "import com.nuvio.app.core.network.ServerConfigurationRepository")
    once(avatars,
         '''    suspend fun fetchAvatars() {
        hydrateFromCacheIfNeeded()''',
         '''    suspend fun fetchAvatars() {
        if (BrunioCloud.enabled) {
            standardCatalog = BrunioAvatarRegistry.catalog()
            standardLoaded = true
            publishCatalog()
            return
        }
        hydrateFromCacheIfNeeded()''')
    once(avatars,
         '''    suspend fun refreshAvatars(force: Boolean = false) {
        hydrateFromCacheIfNeeded()''',
         '''    suspend fun refreshAvatars(force: Boolean = false) {
        if (BrunioCloud.enabled) {
            standardCatalog = BrunioAvatarRegistry.catalog()
            standardLoaded = true
            publishCatalog()
            return
        }
        hydrateFromCacheIfNeeded()''')
    models = app / "features/profiles/ProfileModels.kt"
    once(models,
         '''fun profileAvatarImageUrl(profile: NuvioProfile, avatar: AvatarCatalogItem?): String? =
    normalizedAvatarUrl(profile.avatarUrl)
        ?: avatar?.let(::avatarImageUrl)''',
         '''fun profileAvatarImageUrl(profile: NuvioProfile, avatar: AvatarCatalogItem?): String? =
    com.nuvio.app.features.brunio.brunioAvatarLocalUrl(profile.avatarId.orEmpty())
        ?: normalizedAvatarUrl(profile.avatarUrl)
        ?: avatar?.let(::avatarImageUrl)''')
