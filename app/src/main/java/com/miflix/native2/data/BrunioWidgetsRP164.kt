package com.miflix.native2.data

import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.graphics.*
import android.os.Bundle
import android.widget.RemoteViews
import androidx.work.*
import com.miflix.native2.MainActivity
import com.miflix.native2.R
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

internal object WidgetDatesRP164 {
 fun days(month:YearMonth):List<LocalDate?> {val offset=month.atDay(1).dayOfWeek.value-1;return (0 until 42).map {i->(i-offset+1).takeIf {it in 1..month.lengthOfMonth()}?.let {month.atDay(it)}}}
 fun upcoming(events:List<SportEventRP120>,now:Long):List<SportEventRP120> {
  val today=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString()
  return events.filter {!it.cancelled && if(it.timed) it.starts>=now-30*60000L else it.date>=today}.distinctBy {it.key}.sortedWith(compareBy<SportEventRP120> {it.day()}.thenBy {it.starts})
 }
}
internal object BrunioWidgetsRP164 {
 private fun mobile(c:Context)=c.packageName=="com.brunio.mobile"
 private fun owner(c:Context):String {
  val active=CalendarStoreRP120.active(c);val user=LocalStore(c).loadSession()?.userId ?: "local"
  return active.takeIf {it.startsWith("$user:")} ?: ""
 }
 fun updateAll(c:Context) {
  if(!mobile(c) || c is GuestContextRP150) return
  val manager=AppWidgetManager.getInstance(c)
  for(type in listOf(BrunioCalendarWidgetRP164::class.java,BrunioUpcomingWidgetRP164::class.java)) {
   manager.getAppWidgetIds(ComponentName(c,type)).forEach {render(c,manager,it,type==BrunioCalendarWidgetRP164::class.java)}
  }
 }
 private fun click(c:Context,id:Int)=PendingIntent.getActivity(c,id,Intent(c,MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(android.net.Uri.parse("brunio://calendar")),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 fun render(c:Context,manager:AppWidgetManager,id:Int,calendar:Boolean) {
  val owner=owner(c);val events=if(owner.isBlank()) emptyList() else CalendarStoreRP120.events(c,owner)
  val p=c.getSharedPreferences("brunio_release_alerts_rp164",0);val locale=Locale.forLanguageTag(p.getString("language:$owner","en-US").orEmpty());val es=locale.language=="es"
  val today=LocalDate.now();val next=WidgetDatesRP164.upcoming(events,System.currentTimeMillis())
  val views=RemoteViews(c.packageName,R.layout.brunio_widget_rp164)
  views.setTextViewText(R.id.widget_title,if(calendar) if(es) "Tu calendario" else "Your calendar" else if(es) "Próximos eventos" else "Coming up")
  views.setTextViewText(R.id.widget_profile,CalendarStoreRP120.name(c,owner).ifBlank {"BruniO"})
  views.setOnClickPendingIntent(R.id.widget_root,click(c,id));views.setOnClickPendingIntent(R.id.widget_title,click(c,id))
  val refresh=Intent(c,if(calendar) BrunioCalendarWidgetRP164::class.java else BrunioUpcomingWidgetRP164::class.java).setAction("com.brunio.WIDGET_REFRESH_RP164")
  views.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(c,id,refresh,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
  views.setViewVisibility(R.id.widget_calendar,if(calendar) android.view.View.VISIBLE else android.view.View.GONE)
  if(calendar) views.setImageViewBitmap(R.id.widget_calendar,monthBitmap(today,events,locale))
  views.removeAllViews(R.id.widget_events)
  val count=if(calendar) 0 else ((manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,250)-96)/56).coerceIn(1,4)
  for(e in next.take(count)) {
   val row=RemoteViews(c.packageName,R.layout.brunio_widget_event_rp164);val day=runCatching {LocalDate.parse(e.day())}.getOrDefault(today)
   row.setTextViewText(R.id.widget_day,day.format(DateTimeFormatter.ofPattern("MMM",locale)).uppercase()+"\n"+day.dayOfMonth)
   row.setTextViewText(R.id.widget_event_title,e.title)
   val time=if(e.timed) Instant.ofEpochMilli(e.starts).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a",locale)) else if(es) "Por confirmar" else "Time TBA"
   row.setTextViewText(R.id.widget_event_time,"${e.league}  ·  $time");row.setOnClickPendingIntent(R.id.widget_event_row,click(c,id));views.addView(R.id.widget_events,row)
  }
  views.setTextViewText(R.id.widget_footer,if(next.isEmpty()) if(es) "Añade eventos o sigue tus equipos en BruniO" else "Add events or follow teams in BruniO" else if(calendar) (if(es) "Próximo: " else "Next: ")+next.first().title else if(es) "${next.size} eventos · Abrir calendario ›" else "${next.size} events · Open calendar ›")
  views.setOnClickPendingIntent(R.id.widget_footer,click(c,id));manager.updateAppWidget(id,views)
 }
 private fun monthBitmap(today:LocalDate,events:List<SportEventRP120>,locale:Locale):Bitmap {
  val image=Bitmap.createBitmap(672,360,Bitmap.Config.ARGB_8888);val canvas=Canvas(image);val paint=Paint(Paint.ANTI_ALIAS_FLAG);val month=YearMonth.from(today)
  fun text(value:String,x:Float,y:Float,size:Float,color:Int,bold:Boolean=false) {paint.color=color;paint.textSize=size;paint.typeface=Typeface.create("sans-serif",if(bold) Typeface.BOLD else Typeface.NORMAL);paint.textAlign=Paint.Align.CENTER;canvas.drawText(value,x,y,paint)}
  text(month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy",locale)).replaceFirstChar {it.titlecase(locale)},336f,30f,25f,Color.WHITE,true)
  val names=if(locale.language=="es") listOf("L","M","X","J","V","S","D") else listOf("M","T","W","T","F","S","S")
  names.forEachIndexed {i,name->text(name,48f+i*96,68f,18f,Color.rgb(155,161,185))}
  val marked=events.map {it.day()}.toSet()
  WidgetDatesRP164.days(month).forEachIndexed {index,day->if(day!=null) {
   val x=48f+index%7*96;val y=105f+index/7*46
   if(day==today) {paint.color=Color.rgb(155,125,244);canvas.drawCircle(x,y-8,21f,paint)}
   text(day.dayOfMonth.toString(),x,y,23f,if(day==today) Color.BLACK else Color.WHITE,day==today)
   if(day.toString() in marked) {paint.color=if(day==today) Color.WHITE else Color.rgb(185,158,255);canvas.drawCircle(x,y+12,3.5f,paint)}
  }}
  return image
 }
 fun schedule(c:Context) {if(mobile(c)) WorkManager.getInstance(c).enqueueUniquePeriodicWork("brunio_widget_refresh_rp164",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<WidgetWorkerRP164>(6,TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())}
 suspend fun refresh(c:Context) {val owner=owner(c);if(owner.isNotBlank()) withTimeoutOrNull(30000) {runCatching {CalendarStoreRP120.refresh(c,owner)}};updateAll(c)}
}
abstract class BrunioWidgetProviderRP164:AppWidgetProvider() {
 override fun onEnabled(c:Context) {BrunioWidgetsRP164.schedule(c);BrunioWidgetsRP164.updateAll(c)}
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray) {BrunioWidgetsRP164.updateAll(c)}
 override fun onAppWidgetOptionsChanged(c:Context,m:AppWidgetManager,id:Int,b:Bundle) {BrunioWidgetsRP164.updateAll(c)}
 override fun onReceive(c:Context,i:Intent) {
  super.onReceive(c,i)
  if(i.action=="com.brunio.WIDGET_REFRESH_RP164") {val pending=goAsync();CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {try {BrunioWidgetsRP164.refresh(c.applicationContext)} finally {pending.finish()}}}
 }
}
class BrunioCalendarWidgetRP164:BrunioWidgetProviderRP164()
class BrunioUpcomingWidgetRP164:BrunioWidgetProviderRP164()
class WidgetWorkerRP164(c:Context,p:WorkerParameters):CoroutineWorker(c,p) {override suspend fun doWork():Result {BrunioWidgetsRP164.refresh(applicationContext);return Result.success()}}
