package com.miflix.native2.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/** Network restoration retries only a durable pending event; it never sends heartbeats. */
@Composable
internal fun PresenceNetworkRecovery(state: AppState) {
    val context=LocalContext.current
    DisposableEffect(state) {
        val manager=context.getSystemService(ConnectivityManager::class.java)
        val handler=Handler(Looper.getMainLooper())
        var disposed=false
        val retry=Runnable { if(!disposed) state.retrySocialPresenceEvent() }
        val callback=object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { handler.post(retry) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) handler.post(retry)
            }
        }
        val registered=runCatching { manager.registerNetworkCallback(NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),callback) }.isSuccess
        onDispose {
            disposed=true
            handler.removeCallbacks(retry)
            if(registered) runCatching { manager.unregisterNetworkCallback(callback) }
        }
    }
}
