package com.miflix.native2.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.Inet4Address

internal object RokuCastRP182 {
    data class Status(val sharing:Boolean=false,val busy:Boolean=false,val title:String="",val format:String="",val message:String="")
    val status=MutableStateFlow(Status())
    @Volatile var pending:RokuDlnaServerRP182.Media?=null
    @Volatile var server:RokuDlnaServerRP182?=null
    fun start(context:Context,request:com.miflix.native2.ui.PlayerRequest) {
        if(android.os.Build.VERSION.SDK_INT<26) throw IllegalStateException("Roku sharing requires Android 8 or newer")
        val stream=request.stream
        require(stream.url.startsWith("https://") || stream.url.startsWith("http://")) {"This source needs a direct HTTP video URL"}
        val path=java.net.URI(stream.url).path.orEmpty().lowercase()
        require(!path.endsWith(".mpd")) {"DASH is not supported by this Roku Media Player sharing route"}
        val mime=when {
            request.liveMime.contains("mpegurl",true) || path.endsWith(".m3u8")->"application/vnd.apple.mpegurl"
            request.liveMime.contains("mp2t",true) || path.endsWith(".ts")->"video/mp2t"
            else->"video/mp4"
        }
        val category=when {request.liveProvider=="sports" || request.liveProvider=="lidom"->"Live Sports";request.live->"Live TV";request.item.type=="series"->"Episodes";else->"Movies"}
        val title=request.item.title+if(request.season>0) " · S${request.season} E${request.episode}" else ""
        pending=RokuDlnaServerRP182.Media(title,stream.url,mime,category,stream.requestHeaders)
        status.value=Status(busy=true,title=title,format=mime,message="Starting local sharing…")
        try {ContextCompat.startForegroundService(context,Intent(context,RokuCastServiceRP182::class.java))}
        catch(e:Exception) {pending=null;status.value=Status(message="Could not start sharing on this device")}
    }
    fun stop(context:Context) {context.stopService(Intent(context,RokuCastServiceRP182::class.java))}
}

/** A user-started connected-device service keeps the LAN server alive with the screen locked. */
@androidx.annotation.RequiresApi(26)
class RokuCastServiceRP182:Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var work:Job?=null
    private var multicast:WifiManager.MulticastLock?=null
    private var wifiAwake:WifiManager.WifiLock?=null
    private var awake:android.os.PowerManager.WakeLock?=null
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onCreate() {
        super.onCreate()
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("brunio_roku","Roku local sharing",NotificationManager.IMPORTANCE_LOW))
        val stop=PendingIntent.getService(this,182,Intent(this,RokuCastServiceRP182::class.java).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(this,"brunio_roku").setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("BruniO · Roku Media Player").setContentText("Sharing continues while you use other apps. Keep Wi-Fi connected.")
            .setOngoing(true).addAction(Notification.Action.Builder(null,"Stop sharing",stop).build()).build()
        startForeground(182,notification)
        awake=getSystemService(android.os.PowerManager::class.java).newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK,"brunio:roku-sharing").apply {acquire()}
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="stop") {stopSelf();return START_NOT_STICKY}
        val media=RokuCastRP182.pending ?: run {
            // An empty repeat start must not tear down an existing receiver session.
            if(RokuCastRP182.server==null && work?.isActive!=true) stopSelf(startId)
            return START_NOT_STICKY
        }
        RokuCastRP182.pending=null
        val previous=work
        previous?.cancel()
        work=scope.launch {
            previous?.join()
            var candidate:RokuDlnaServerRP182?=null
            try {
                RokuCastRP182.server?.close();RokuCastRP182.server=null
                val cm=getSystemService(ConnectivityManager::class.java)
                val network=cm.activeNetwork ?: error("Connect the phone and Roku to the same Wi-Fi")
                val caps=cm.getNetworkCapabilities(network) ?: error("Wi-Fi is unavailable")
                require(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {"Use the same Wi-Fi as your Roku"}
                val endpoint=cm.getLinkProperties(network)?.linkAddresses?.firstOrNull {it.address is Inet4Address && LocalCastServer.isPrivate(it.address.address)}
                    ?: error("A private Wi-Fi address is required")
                if(multicast==null) {
                    val wifi=applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    multicast=wifi.createMulticastLock("brunio-roku").apply {setReferenceCounted(false);acquire()}
                }
                if(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && wifiAwake==null) {
                    val wifi=applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val lock=wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,"brunio-sharing-network")
                    lock.setReferenceCounted(false);lock.acquire();wifiAwake=lock
                }
                candidate=RokuDlnaServerRP182(endpoint.address as Inet4Address,endpoint.prefixLength,media)
                ensureActive()
                RokuCastRP182.server=candidate
                RokuCastRP182.status.value=RokuCastRP182.Status(sharing=true,title=media.title,format=media.mime,message="Open Roku Media Player → Video → BruniO Mobile → Now Playing")
                candidate=null
                while(isActive) {
                    delay(3000)
                    // The phone's default network may change (for example VPN or
                    // mobile data) while the receiver's Wi-Fi remains reachable.
                    val stillOnNetwork=cm.getNetworkCapabilities(network)!=null && cm.getLinkProperties(network)?.linkAddresses?.any {it.address==endpoint.address}==true
                    if(!stillOnNetwork) {
                        RokuCastRP182.server?.close();RokuCastRP182.server=null
                        RokuCastRP182.status.value=RokuCastRP182.Status(message="Wi-Fi changed. Connect both devices and start sharing again.")
                        stopSelf(startId);break
                    }
                }
            } catch(e:CancellationException) {throw e} catch(e:Exception) {
                RokuCastRP182.status.value=RokuCastRP182.Status(message=e.message ?: "Local sharing failed")
                stopSelf(startId)
            } finally {candidate?.close()}
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        scope.cancel();RokuCastRP182.server?.close();RokuCastRP182.server=null;RokuCastRP182.pending=null
        multicast?.let {if(it.isHeld) it.release()};multicast=null
        wifiAwake?.let {if(it.isHeld) it.release()};wifiAwake=null
        awake?.let {if(it.isHeld) it.release()};awake=null
        val current=RokuCastRP182.status.value
        RokuCastRP182.status.value=if(current.sharing || current.busy) RokuCastRP182.Status(message="Sharing stopped") else current
        super.onDestroy()
    }
}
