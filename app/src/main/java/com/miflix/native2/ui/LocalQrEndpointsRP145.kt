package com.miflix.native2.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.miflix.native2.data.LocalQrNetworkRP129
import java.net.Inet4Address

/** Prefer the actual active Ethernet/Wi-Fi route; enumerate alternatives without Wi-Fi SSID assumptions. */
internal fun localQrEndpointsRP145(context:Context):List<LocalQrNetworkRP129.Endpoint> {
    val active=runCatching {
        val manager=context.getSystemService(ConnectivityManager::class.java)
        val network=manager.activeNetwork ?: return@runCatching null
        val caps=manager.getNetworkCapabilities(network) ?: return@runCatching null
        if(caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
            !(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) return@runCatching null
        manager.getLinkProperties(network)?.linkAddresses?.firstOrNull {
            it.address is Inet4Address && LocalQrNetworkRP129.acceptsPeer(it.address.address) && it.prefixLength in 8..30
        }?.let {LocalQrNetworkRP129.Endpoint(it.address as Inet4Address,it.prefixLength)}
    }.getOrNull()
    return (listOfNotNull(active)+LocalQrNetworkRP129.find()).distinctBy {it.address.hostAddress}
}
