package com.miflix.native2.data

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.miflix.native2.BuildConfig
import com.miflix.native2.MainActivity
import com.miflix.native2.R
import com.miflix.native2.model.ReleaseNotice
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.*
import java.util.concurrent.TimeUnit

internal object ReleaseAlertsRP164 {
 val lock=Mutex()
 private const val WORK="brunio_release_alerts_rp164"
 private const val CHANNEL="brunio_my_list_releases_rp164"
 private fun prefs(c:Context)=c.getSharedPreferences("brunio_release_alerts_rp164",0)
 fun enabled(c:Context)=c.packageName=="com.brunio.mobile"
 fun configure(c:Context,owner:String,name:String,ids:List<String>,token:String,language:String,isEnabled:Boolean) {
  if(!enabled(c) || c is GuestContextRP150) return
  val p=prefs(c)
  if(!isEnabled) {p.edit().remove("active").apply();WorkManager.getInstance(c).cancelUniqueWork(WORK);return}
  val old=p.getString("ids:$owner","");val rows=JSONArray(ids.filter(FavoriteSyncRP164::valid).distinct()).toString()
  val changed=old!=rows || p.getString("active","")!=owner
  p.edit().putString("active",owner).putString("ids:$owner",rows).putString("token:$owner",token).putString("language:$owner",language).putString("name:$owner",name).apply()
  val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()
  WorkManager.getInstance(c).enqueueUniquePeriodicWork(WORK,ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<ReleaseWorkerRP164>(1,TimeUnit.HOURS).setConstraints(constraints).build())
  if(changed && ids.isNotEmpty()) WorkManager.getInstance(c).enqueueUniqueWork("${WORK}_initial",ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<ReleaseWorkerRP164>().setConstraints(constraints).build())
 }
 fun allowed(c:Context)=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
 @Synchronized fun notify(c:Context,owner:String,release:ReleaseNotice) {
  if(!enabled(c) || !allowed(c) || prefs(c).getString("active","")!=owner) return
  val delivered=prefs(c).getStringSet("delivered:$owner",emptySet()).orEmpty()
  if(release.id in delivered) return
  val manager=c.getSystemService(NotificationManager::class.java)
  if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel(CHANNEL,"BruniO · My List releases",NotificationManager.IMPORTANCE_DEFAULT))
  if(!androidx.core.app.NotificationManagerCompat.from(c).areNotificationsEnabled()) return
  val intent=Intent(c,MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(android.net.Uri.parse("brunio://content/${release.item.type}/${release.item.id}"))
  val pi=PendingIntent.getActivity(c,(owner+release.id).hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notification=NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_release_rp164).setContentTitle(release.item.title).setContentText(release.message).setStyle(NotificationCompat.BigTextStyle().bigText(release.message)).setContentIntent(pi).setAutoCancel(true).build()
  try {manager.notify((owner+release.id).hashCode(),notification);prefs(c).edit().putStringSet("delivered:$owner",(delivered+release.id).toList().takeLast(1000).toSet()).apply()} catch(_:SecurityException) {}
 }
 suspend fun scan(c:Context)=lock.withLock {
  val p=prefs(c);val owner=p.getString("active","") ?: return@withLock
  val local=LocalStore(c);val current="${local.loadSession()?.userId ?: "local"}:${local.profileId()}"
  if(owner!=current) return@withLock
  val token=p.getString("token:$owner","").orEmpty().ifBlank {BuildConfig.MIFLIX_TMDB_TOKEN};if(token.isBlank()) return@withLock
  val a=JSONArray(p.getString("ids:$owner","[]"));val ids=(0 until a.length()).map {a.optString(it)}
  val cursor=p.getInt("cursor:$owner",0);val batch=ReleasePolicyRP164.batch(ids,cursor)
  val repo=TmdbRepository(token).apply {language=p.getString("language:$owner","en-US").orEmpty()}
  val noticesPrefs=c.getSharedPreferences(PLAYBACK_PREFS,0)
  val knownKey="known_${owner.replaceFirst(':','_')}";val noticesKey="notices_${owner.replaceFirst(':','_')}"
  val known=runCatching {JSONObject(noticesPrefs.getString(knownKey,"{}"))}.getOrDefault(JSONObject())
  val old=runCatching {JSONArray(noticesPrefs.getString(noticesKey,"[]"))}.getOrDefault(JSONArray());val rows=objectsRP164(old).toMutableList()
  for(id in batch) {
   if(p.getString("active","")!=owner) return@withLock
   try {
    val item=repo.byCloudId(id) ?: continue;val release=repo.latestRelease(item)
    if(ReleasePolicyRP164.changed(known.optString(id).takeIf {known.has(id)},release?.id) && release!=null) {
     if(rows.none {it.optString("id")==release.id}) rows.add(0,JSONObject().put("id",release.id).put("cloud",item.cloudId).put("title",item.title).put("overview",item.overview).put("poster",item.poster).put("message",release.message).put("date",release.date))
     notify(c,owner,release)
    }
    known.put(id,release?.id ?: "pending")
   } catch(e:CancellationException) {throw e} catch(_:Exception) {}
  }
  noticesPrefs.edit().putString(knownKey,known.toString()).putString(noticesKey,JSONArray(rows.take(100)).toString()).apply()
  p.edit().putInt("cursor:$owner",if(ids.isEmpty()) 0 else (cursor+batch.size)%ids.size).apply()
 }
 private fun objectsRP164(a:JSONArray)=(0 until a.length()).mapNotNull {a.optJSONObject(it)}
 const val PLAYBACK_PREFS="miflix_playback"
}
class ReleaseWorkerRP164(c:Context,p:WorkerParameters):CoroutineWorker(c,p) {
 override suspend fun doWork():Result {return try {withTimeout(8*60_000L) {ReleaseAlertsRP164.scan(applicationContext)};Result.success()} catch(e:CancellationException) {throw e} catch(_:Exception) {Result.retry()}}
}
