package com.miflix.native2.data

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import com.miflix.native2.R
import kotlinx.coroutines.*

internal object CalendarAlarmsRP120 {
    private const val CHANNEL="brunio_events_rp120"
    private fun pending(c:Context,owner:String,key:String):PendingIntent {
        val intent=Intent(c,CalendarReceiverRP120::class.java).setAction("com.miflix.CALENDAR_RP120")
            .setData(Uri.parse("brunio-calendar://alarm/${Uri.encode(owner)}/${Uri.encode(key)}")).putExtra("owner",owner).putExtra("key",key)
        return PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun precise(c:Context)=Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun schedule(c:Context,owner:String) {
        val prefs=c.getSharedPreferences("brunio_calendar_rp120",0);val alarm=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val old=prefs.getStringSet("alarms:$owner",emptySet()).orEmpty()
        for(key in old) alarm.cancel(pending(c,owner,key))
        if(CalendarStoreRP120.active(c)!=owner) return
        val now=System.currentTimeMillis()
        val targets=CalendarStoreRP120.events(c,owner).filter {it.timed && it.starts>now && it.starts<now+7*86400000L}.take(1)
        val keys=mutableSetOf<String>()
        fun put(key:String,at:Long,exact:Boolean) {
            val pi=pending(c,owner,key)
            runCatching {
                if(exact && precise(c)) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
                else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
                keys+=key
            }
        }
        for(event in targets) put(event.key,event.starts,true)
        if(CalendarStoreRP120.subscriptions(c,owner).isNotEmpty() || CalendarStoreRP120.events(c,owner).any {it.league=="BRUNIO" && it.starts>now+7*86400000L}) put("refresh",now+6*3600000L,false)
        prefs.edit().putStringSet("alarms:$owner",keys).apply()
    }
    fun notify(c:Context,owner:String,event:SportEventRP120) {
        val manager=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel(CHANNEL,"BruniO · Events",NotificationManager.IMPORTANCE_DEFAULT))
        val launch=c.packageManager.getLaunchIntentForPackage(c.packageName) ?: return
        val intent=PendingIntent.getActivity(c,0,launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder=androidx.core.app.NotificationCompat.Builder(c,CHANNEL)
        val notice=builder.setSmallIcon(R.drawable.brunio_icon).setContentTitle(event.title)
            .setContentText("${CalendarStoreRP120.name(c,owner)} · ${event.league} · ${CalendarStoreRP120.starting(c,owner)}")
            .setContentIntent(intent).setAutoCancel(true).build()
        runCatching {manager.notify(event.noticeKey.hashCode(),notice)}
    }
}

class CalendarReceiverRP120:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val app=context.applicationContext;val owner=CalendarStoreRP120.active(app)
        if(owner.isBlank()) return
        val requested=intent.getStringExtra("owner")
        if(requested!=null && requested!=owner) return
        val result=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try {
                CalendarStoreRP120.tick(app,owner)
                if(intent.getStringExtra("key")=="refresh" || intent.action in listOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED))
                    withTimeoutOrNull(8000) {CalendarStoreRP120.refresh(app,owner)}
                CalendarAlarmsRP120.schedule(app,owner)
            } finally {result.finish()}
        }
    }
}
