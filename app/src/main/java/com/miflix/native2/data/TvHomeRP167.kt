package com.miflix.native2.data

import android.content.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.tvprovider.media.tv.*
import com.miflix.native2.MainActivity
import com.miflix.native2.R
import com.miflix.native2.model.MediaSummary
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

internal object TvHomeRP167 {
 private val lock=Mutex()
 private fun prefs(c:Context)=c.getSharedPreferences("brunio_tv_home_rp167",0)
 fun supported(c:Context)=Build.VERSION.SDK_INT>=26 && !Build.MANUFACTURER.equals("Amazon",true) && c.packageName!="com.brunio.mobile" && c.packageManager.resolveContentProvider(TvContractCompat.AUTHORITY,0)!=null
 fun enabled(c:Context)=prefs(c).getBoolean("enabled",true)
 fun setEnabled(c:Context,value:Boolean){prefs(c).edit().putBoolean("enabled",value).apply()}
 fun channel(c:Context,role:String)=prefs(c).getLong("channel:$role",-1)
 private fun intent(c:Context,item:MediaSummary?=null,role:String="home")=Intent(c,MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(if(item==null) "brunio://tv-home?role=$role" else "brunio://content/${item.type}/${item.id}")).toUri(Intent.URI_INTENT_SCHEME)
 suspend fun clear(c:Context)=lock.withLock {withContext(Dispatchers.IO) {clearLocked(c)}}
 private fun clearLocked(c:Context) {
  if(!supported(c)) return
  val p=prefs(c);val helper=PreviewChannelHelper(c)
  for(role in listOf("continue","list","recommendations")) {val id=channel(c,role);if(id>=0)runCatching {helper.deletePreviewChannel(id)}}
  p.edit().clear().putBoolean("enabled",p.getBoolean("enabled",true)).apply()
 }
 suspend fun publish(c:Context,owner:String,name:String,rows:Map<String,List<MediaSummary>>) = lock.withLock {
  if(!supported(c)) return@withLock
  withContext(Dispatchers.IO) {
   val p=prefs(c)
   if(p.getString("owner","")!=owner) {
    val oldHelper=PreviewChannelHelper(c)
    for(role in listOf("continue","list","recommendations")) {
     val oldPrograms=runCatching {JSONObject(p.getString("programs:$role","{}"))}.getOrDefault(JSONObject())
     oldPrograms.keys().forEach {key->runCatching {oldHelper.deletePreviewProgram(oldPrograms.getLong(key))}}
     p.edit().remove("programs:$role").apply()
    }
   }
   if(!enabled(c)) return@withContext
   val helper=PreviewChannelHelper(c)
   for((role,items) in rows) {
    var id=channel(c,role)
    if(id>=0 && runCatching {helper.getPreviewChannel(id)}.getOrNull()==null)id=-1
    if(id<0) {
     val title=when(role){"continue"->"Continue Watching";"list"->"My List";else->"Recommended"}
     val logo=BitmapFactory.decodeResource(c.resources,R.drawable.brunio_icon)
     id=helper.publishChannel(PreviewChannel.Builder().setDisplayName("BruniO · $title").setDescription(name).setAppLinkIntentUri(Uri.parse(intent(c,role=role))).setLogo(logo).build())
     if(id<0)continue
     p.edit().putLong("channel:$role",id).apply()
    }
    val old=runCatching {JSONObject(p.getString("programs:$role","{}"))}.getOrDefault(JSONObject());val next=JSONObject()
    for((index,item) in items.distinctBy {it.cloudId}.take(20).withIndex()) {
     val builder=PreviewProgram.Builder().setChannelId(id).setType(if(item.type=="series") TvContractCompat.PreviewPrograms.TYPE_TV_SERIES else TvContractCompat.PreviewPrograms.TYPE_MOVIE).setTitle(item.title).setDescription(item.overview).setIntentUri(Uri.parse(intent(c,item))).setInternalProviderId(item.cloudId).setWeight(100-index)
     (item.backdrop ?: item.poster)?.let {builder.setPosterArtUri(Uri.parse(it)).setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9)}
     val program=builder.build();val previous=old.optLong(item.cloudId,-1)
     val row=if(previous>=0 && helper.getPreviewProgram(previous)!=null) {helper.updatePreviewProgram(previous,program);previous} else helper.publishPreviewProgram(program)
     if(row>=0)next.put(item.cloudId,row)
    }
    old.keys().forEach {key->if(!next.has(key))runCatching {helper.deletePreviewProgram(old.getLong(key))}}
    p.edit().putString("programs:$role",next.toString()).apply()
   }
   p.edit().putString("owner",owner).apply()
  }
 }
 fun requestVisible(c:Context,role:String):Boolean {
  if(!supported(c))return false
  val id=channel(c,role);if(id<0)return false
  return runCatching {c.startActivity(Intent(TvContractCompat.ACTION_REQUEST_CHANNEL_BROWSABLE).putExtra(TvContractCompat.EXTRA_CHANNEL_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));true}.getOrDefault(false)
 }
}
