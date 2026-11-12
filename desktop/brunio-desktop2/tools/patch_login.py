"""Authentication hardening for the BruniO Desktop2 fork.

Guest preview accounts are not real BruniO accounts. Do not let a cached
anonymous Nuvio profile trap the user in an empty profile picker.
"""
from pathlib import Path
from patch_runtime import once


def apply(root: Path) -> None:
    src = root / "composeApp/src/commonMain/kotlin/com/nuvio/app"
    auth = src / "core/auth/AuthRepository.kt"
    once(auth, "import com.nuvio.app.features.brunio.BrunioCloud\nimport co.touchlab.kermit.Logger",
         "import com.nuvio.app.features.brunio.BrunioCloud\nimport co.touchlab.kermit.Logger")
    once(auth, '''        val savedAnonId = AuthStorage.loadAnonymousUserId()
        if (savedAnonId != null) {''',
         '''        // Old UI previews could store an anonymous Nuvio guest, which does
        // not exist on the BruniO backend. Drop only that guest identifier.
        if (BrunioCloud.enabled) AuthStorage.clearAnonymousUserId()
        val savedAnonId = AuthStorage.loadAnonymousUserId()
        if (savedAnonId != null) {''')
    once(auth, '''    fun signInAnonymously() {
        _error.value = null''',
         '''    fun signInAnonymously() {
        if (BrunioCloud.enabled) {
            _error.value = "Inicia sesión con tu cuenta de BruniO para continuar."
            return
        }
        _error.value = null''')

    screen = src / "features/auth/AuthScreen.kt"
    once(screen, "import com.nuvio.app.core.auth.AuthRepository",
         "import com.nuvio.app.features.brunio.BrunioCloud\nimport com.nuvio.app.core.auth.AuthRepository")
    once(screen, '''        AuthSecondaryButton(
            text = stringResource(Res.string.compose_auth_continue_without_account),
            enabled = !isLoading,
            height = metrics.secondaryHeight,
            scale = scale,
            onClick = onContinueWithoutAccount,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(Res.string.compose_auth_store_locally),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AuthTextMuted,
                fontSize = (13f * scale).sp,
                lineHeight = (18f * scale).sp,
                fontWeight = FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
        )''',
         '''        if (!BrunioCloud.enabled) {
            AuthSecondaryButton(
                text = stringResource(Res.string.compose_auth_continue_without_account),
                enabled = !isLoading,
                height = metrics.secondaryHeight,
                scale = scale,
                onClick = onContinueWithoutAccount,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(Res.string.compose_auth_store_locally),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AuthTextMuted,
                    fontSize = (13f * scale).sp,
                    lineHeight = (18f * scale).sp,
                    fontWeight = FontWeight.Normal,
                ),
                textAlign = TextAlign.Center,
            )
        }''')
    # Only the email/password Supabase flow is supported. Nuvio's device link
    # endpoint uses different server RPCs.
    server = src / "core/network/ServerConfiguration.kt"
    once(server, "        tvLogin = true,\n        watchParty = true,",
         "        tvLogin = false,\n        watchParty = true,")

    gate = src / "AppGate.kt"
    once(gate, "import androidx.compose.foundation.layout.Box\n", 
         "import androidx.compose.foundation.layout.Box\n"
         "import androidx.compose.foundation.layout.Arrangement\n"
         "import androidx.compose.foundation.layout.Column\n"
         "import androidx.compose.foundation.layout.fillMaxWidth\n"
         "import androidx.compose.material3.Button\n"
         "import androidx.compose.material3.OutlinedButton\n"
         "import androidx.compose.material3.Text\n"
         "import androidx.compose.ui.unit.dp\n")
    once(gate, '''        val hasCachedProfileAccess =
            cachedProfiles.isNotEmpty() &&
                authState !is AuthState.Authenticated''',
         '''        val hasCachedProfileAccess =
            !BrunioCloud.enabled &&
                cachedProfiles.isNotEmpty() &&
                authState !is AuthState.Authenticated''')
    once(gate, '''                if (gateScreen == AppGateScreen.Loading.name || gateScreen == AppGateScreen.Auth.name) {
                    enterProfileGate(ProfileRepository.state.value.profiles, syncOnEnter = true)
                }''',
         '''                if (gateScreen == AppGateScreen.Loading.name || gateScreen == AppGateScreen.Auth.name) {
                    if (BrunioCloud.enabled && !ProfileRepository.state.value.isLoaded) {
                        gateScreen = AppGateScreen.Loading.name
                    } else {
                        enterProfileGate(ProfileRepository.state.value.profiles, syncOnEnter = true)
                    }
                }''')
    once(gate, '''                ProfileSelectionScreen(
                    onProfileSelected''',
         '''                if (BrunioCloud.enabled && profileState.profiles.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("No se encontraron perfiles de BruniO.")
                        Text("Reintenta cargar tu cuenta o vuelve a iniciar sesión.")
                        Button(onClick = {
                            gateScope.launch { ProfileRepository.pullProfiles() }
                        }) { Text("Reintentar") }
                        OutlinedButton(onClick = {
                            gateScope.launch {
                                AuthRepository.signOut()
                                gateScreen = AppGateScreen.Auth.name
                            }
                        }) { Text("Cambiar cuenta / Iniciar sesión") }
                    }
                } else ProfileSelectionScreen(
                    onProfileSelected''')
