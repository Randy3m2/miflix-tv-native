#!/usr/bin/env python3
"""BruniO-only repository wiring for pinned Nuvio visual upstream.

Only copies Nuvio UI. Remote account data, profile IDs and TMDB settings are
loaded from BruniO. Incompatible Nuvio sync RPCs are disabled, not redirected.
All changes are exact-anchored so an upstream change aborts the build.
"""
from pathlib import Path


def once(path: Path, before: str, after: str) -> None:
    source = path.read_text(encoding="utf-8")
    count = source.count(before)
    if count != 1:
        raise RuntimeError(f"Cannot safely patch {path}: {before[:100]!r} found {count} times")
    path.write_text(source.replace(before, after, 1), encoding="utf-8")


def install(destination: Path) -> None:
    root = destination / "composeApp/src/commonMain/kotlin/com/nuvio/app"

    profile = root / "features/profiles/ProfileRepository.kt"
    once(profile, "import com.nuvio.app.core.auth.AuthRepository",
        "import com.nuvio.app.features.brunio.BrunioCloud\nimport com.nuvio.app.core.auth.AuthRepository")
    once(profile,
        '        try {\n            val result = SupabaseProvider.client.postgrest.rpc("sync_pull_profiles")',
        '''        if (BrunioCloud.enabled) {
            val user = AuthRepository.state.value as? AuthState.Authenticated ?: return
            try {
                val profiles = BrunioCloud.pullProfiles(user.userId).nuvioProfiles.sortedBy { it.profileIndex }
                val current = profiles.firstOrNull { it.profileIndex == activeProfileIndex }
                    ?: profiles.firstOrNull()
                _state.value = _state.value.copy(
                    profiles = profiles, isLoaded = true, activeProfile = current,
                )
                if (current != null) activeProfileIndex = current.profileIndex
                persist()
            } catch (error: Throwable) {
                if (AuthRepository.signOutIfSessionInvalid(error, "BruniO profile pull")) return
                log.e(error) { "BruniO profile pull failed" }
                _state.value = _state.value.copy(isLoaded = true)
            }
            return
        }
        try {
            val result = SupabaseProvider.client.postgrest.rpc("sync_pull_profiles")''')
    # A BruniO profile mutation must NEVER call Nuvio's sync_push_profiles or
    # sync_delete_profile_data. Remote edits require a separate BruniO-safe writer.
    once(profile,
        '''    suspend fun pushProfiles(profiles: List<ProfilePushPayload>) {
        if (AuthRepository.state.value.isAnonymous) {''',
        '''    suspend fun pushProfiles(profiles: List<ProfilePushPayload>) {
        if (BrunioCloud.enabled) return // Remote profile editing disabled until BruniO-safe writer
        if (AuthRepository.state.value.isAnonymous) {''')
    once(profile,
        '''    suspend fun deleteProfile(profileIndex: Int) {
        if (AuthRepository.state.value.isAnonymous) {''',
        '''    suspend fun deleteProfile(profileIndex: Int) {
        if (BrunioCloud.enabled) return // Never delete BruniO profile via Nuvio RPC
        if (AuthRepository.state.value.isAnonymous) {''')
    once(profile,
        '''    suspend fun verifyPin(profileIndex: Int, pin: String): PinVerifyResult {
        if (AuthRepository.state.value !is AuthState.Authenticated) {''',
        '''    suspend fun verifyPin(profileIndex: Int, pin: String): PinVerifyResult {
        if (BrunioCloud.enabled) return PinVerifyResult(
            unlocked = false, message = "BruniO PIN verification needs an RP1-compatible adapter",
        )
        if (AuthRepository.state.value !is AuthState.Authenticated) {''')
    for signature in [
        "suspend fun setPin(profileIndex: Int, pin: String, currentPin: String? = null): PinVerifyResult",
        "suspend fun clearPin(profileIndex: Int, currentPin: String? = null): PinVerifyResult",
    ]:
        once(profile, f"    {signature} {{",
            f'''    {signature} {{
        if (BrunioCloud.enabled) return PinVerifyResult(
            unlocked = false, message = "Use BruniO TV to manage profile PINs",
        )''')
    once(profile,
        '''    suspend fun clearPinWithPassword(profileIndex: Int, accountPassword: String) {
        runCatching {''',
        '''    suspend fun clearPinWithPassword(profileIndex: Int, accountPassword: String) {
        if (BrunioCloud.enabled) return
        runCatching {''')
    once(profile,
        '''    suspend fun pullProfileLocks(): List<ProfileLockState> {
        return runCatching {''',
        '''    suspend fun pullProfileLocks(): List<ProfileLockState> {
        if (BrunioCloud.enabled) return emptyList()
        return runCatching {''')

    tmdb = root / "features/tmdb/TmdbSettingsRepository.kt"
    once(tmdb, "    fun effectiveApiKey(): String = snapshot().apiKey.ifBlank { TmdbConfig.API_KEY }",
        '''    fun effectiveApiKey(): String = snapshot().apiKey.ifBlank { TmdbConfig.API_KEY }

    /** In-memory BruniO TMDB setting: never copied to Nuvio profile-sync. */
    fun applyBrunioAccountSettings(token: String, accountLanguage: String) {
        ensureLoaded()
        val newToken = token.trim()
        val changed = apiKey != newToken
        apiKey = newToken
        enabled = newToken.isNotEmpty()
        language = normalizeLanguage(accountLanguage).ifBlank { "es-ES" }
        publish()
        if (changed) invalidateMetadata()
    }''')

    addon = root / "features/addons/AddonRepository.kt"
    once(addon, "import com.nuvio.app.core.network.SupabaseProvider",
        "import com.nuvio.app.features.brunio.BrunioCloud\nimport com.nuvio.app.core.network.SupabaseProvider")
    once(addon,
        '''    suspend fun pullFromServer(profileId: Int) {
        currentProfileId = resolveEffectiveProfileId(profileId)''',
        '''    suspend fun pullFromServer(profileId: Int) {
        if (BrunioCloud.enabled) {
            currentProfileId = resolveEffectiveProfileId(profileId)
            val urls = BrunioCloud.manifestUrls().map(::ensureManifestSuffix).distinct()
            val existing = _uiState.value.addons.associateBy(ManagedAddon::manifestUrl)
            _uiState.value = AddonsUiState(addons = urls.map { url ->
                existing[url].toPendingAddon(manifestUrl = url, enabled = true)
            })
            initialized = true
            persist()
            urls.forEach(::refreshAddon)
            return
        }
        currentProfileId = resolveEffectiveProfileId(profileId)''')
    once(addon,
        '''    private fun pushToServer() {
        if (isUsingPrimaryAddonsFromSecondaryProfile()) return''',
        '''    private fun pushToServer() {
        if (BrunioCloud.enabled) return // Never call Nuvio sync_push_addons on BruniO
        if (isUsingPrimaryAddonsFromSecondaryProfile()) return''')
    # Some upstream logging includes full configured add-on URLs containing
    # private API tokens; never emit them from BruniO builds.
    once(addon, 'log.i { "addAddon() — rawUrl=$rawUrl" }',
        'log.d { "BruniO add-on manual add requested" }')
    once(addon, 'log.i { "removeAddon() — $manifestUrl" }',
        'log.d { "BruniO add-on removal requested" }')

    sync = root / "core/sync/SyncManager.kt"
    once(sync, "import com.nuvio.app.core.auth.AuthRepository",
        "import com.nuvio.app.features.brunio.BrunioCloud\nimport com.nuvio.app.core.auth.AuthRepository")
    once(sync,
        '''    fun pullAllForProfile(profileId: Int) {
        startFullProfilePull(profileId = profileId, reason = "requested")''',
        '''    fun pullAllForProfile(profileId: Int) {
        if (BrunioCloud.enabled) return // No Nuvio sync_* queries on BruniO backend
        startFullProfilePull(profileId = profileId, reason = "requested")''')
    once(sync,
        '''    fun requestForegroundPull(profileId: Int, force: Boolean = false) {
        val authState = AuthRepository.state.value''',
        '''    fun requestForegroundPull(profileId: Int, force: Boolean = false) {
        if (BrunioCloud.enabled) return // BruniO state reads on profile/account open only
        val authState = AuthRepository.state.value''')
    once(sync,
        '''    fun startPeriodicNuvioSyncPull(profileId: Int) {
        val authState = AuthRepository.state.value''',
        '''    fun startPeriodicNuvioSyncPull(profileId: Int) {
        if (BrunioCloud.enabled) return // No recurring Supabase Home polling
        val authState = AuthRepository.state.value''')

    gate = root / "AppGate.kt"
    once(gate, "import com.nuvio.app.core.auth.AuthRepository",
        "import com.nuvio.app.features.brunio.BrunioCloud\nimport com.nuvio.app.core.auth.AuthRepository")
    once(gate, "        DeviceSessionRegistration.registerIfAuthenticated(force = true)",
        "        // BruniO: Nuvio-specific device registration RPC disabled")
    once(gate,
        '''                    SyncManager.pullAllForProfile(profile.profileIndex)''',
        '''                    if (BrunioCloud.enabled) {
                        BrunioCloud.activateProfile(profile.profileIndex)
                        AddonRepository.pullFromServer(profile.profileIndex)
                    } else {
                        SyncManager.pullAllForProfile(profile.profileIndex)
                    }''')
    # Native updater and incompatible Nuvio deletion functions are unsafe.
    auth = root / "core/auth/AuthRepository.kt"
    once(auth, "import co.touchlab.kermit.Logger",
        "import com.nuvio.app.features.brunio.BrunioCloud\nimport co.touchlab.kermit.Logger")
    once(auth,
        '''    suspend fun deleteAccount(): Result<Unit> = runCatching {
        _error.value = null''',
        '''    suspend fun deleteAccount(): Result<Unit> = runCatching {
        check(!BrunioCloud.enabled) { "Delete your account through BruniO account settings" }
        _error.value = null''')
    once(auth,
        '''    suspend fun signOut(): Result<Unit> {
        _error.value = null''',
        '''    suspend fun signOut(): Result<Unit> {
        if (BrunioCloud.enabled) BrunioCloud.clear()
        _error.value = null''')

    party = root / "features/party/PartyRepository.kt"
    once(party, "import co.touchlab.kermit.Logger",
        "import com.nuvio.app.features.brunio.BrunioCloud\nimport co.touchlab.kermit.Logger")
    once(party,
        '''    val isSupported: Boolean
        get() = available.value''',
        '''    val isSupported: Boolean
        get() = !BrunioCloud.enabled && available.value
        // RP1 adapter is being connected separately; never create incompatible
        // Nuvio watch_party_* records on the user's BruniO backend.''')
