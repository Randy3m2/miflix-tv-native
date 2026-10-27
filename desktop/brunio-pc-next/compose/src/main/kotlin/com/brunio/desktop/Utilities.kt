package com.brunio.desktop

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray

class Utilities(private val scope: CoroutineScope, private val bridge: BridgeClient) {
    var dialog by mutableStateOf<String?>(null)
    var downloads by mutableStateOf<JSONObject?>(null)
    var addons by mutableStateOf<JSONObject?>(null)
    var profiles by mutableStateOf<JSONObject?>(null)
    var social by mutableStateOf<JSONObject?>(null)
    private var tickActive=false
    private var partyEpoch=0L
    var subscriptions by mutableStateOf<JSONObject?>(null)
    var accountSetup by mutableStateOf<JSONObject?>(null)
    var scores by mutableStateOf<List<JSONObject>>(emptyList())
    var pinned by mutableStateOf<JSONObject?>(null)
    var party by mutableStateOf<JSONObject?>(null)
    var updates by mutableStateOf<JSONObject?>(null)
    var error by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    private var generation=0L
    var playbackActive=false
    private var partyJob:Job?=null
    fun reset() { partyEpoch++;partyJob?.cancel();partyJob=null;generation++; dialog=null;downloads=null;addons=null;profiles=null;subscriptions=null;scores=emptyList();pinned=null;party=null;social=null;tickActive=false;updates=null;error=null;busy=false }
    fun run(method: String, params: JSONObject=JSONObject(), done:(Any)->Unit={}) {
        val epoch=generation;val roomEpoch=partyEpoch
        scope.launch {
            busy=true;error=null
            try { val value=bridge.call(method,params);if(epoch==generation && (!method.startsWith("party.") || roomEpoch==partyEpoch))done(value) }
            catch(e:Exception) { if(epoch==generation)error=e.message }
            finally { if(epoch==generation)busy=false }
        }
    }
    fun open(kind: String) {
        dialog=kind;error=null
        when(kind) {
            "addons" -> refreshAddons()
            "account" -> run("settings.get"){accountSetup=it as JSONObject}
            "profiles" -> run("profiles.list") { profiles=it as JSONObject }
            "subscriptions" -> run("calendar.subscriptions") { subscriptions=it as JSONObject }
            "scores" -> loadScores("NBA")
            "social" -> run("friends.identity"){social=it as JSONObject}
            "party" -> pollParty()
            "updates" -> run("updates.check") { updates=it as JSONObject }
        }
    }
    fun saveNickname(nickname:String) {
        val data=social?:return
        run("friends.nickname",JSONObject().put("scope",data.getString("scope")).put("nickname",nickname)){social=it as JSONObject;dialog=null}
    }
    fun saveProfile(name:String,id:String?,avatar:String?,done:(JSONObject)->Unit) {
        val data=profiles?:return
        val args=JSONObject().put("scope",data.getString("scope")).put("name",name)
        if(id!=null)args.put("id",id)
        if(avatar!=null)args.put("avatar",avatar)
        run("profiles.save",args) { profiles=it as JSONObject;done(it) }
    }
    fun deleteProfile(id:String,done:(JSONObject)->Unit) {
        val data=profiles?:return
        run("profiles.delete",JSONObject().put("scope",data.getString("scope")).put("id",id)) { profiles=it as JSONObject;done(it) }
    }
    fun configureAccount(token:String) {
        val data=accountSetup?:return
        run("account.configure",JSONObject().put("scope",data.getString("scope")).put("token",token)){dialog=null}
    }
    fun openDownloadsFolder()=run("downloads.folder") { value->
        try { if(java.awt.Desktop.isDesktopSupported())java.awt.Desktop.getDesktop().open(java.io.File((value as JSONObject).getString("path")))else error="El explorador de archivos no está disponible" }
        catch(_:Exception){error="No se pudo abrir la carpeta de descargas"}
    }
    fun subscribe(key:String,enabled:Boolean) {
        val data=subscriptions?:return
        run("calendar.subscribe",JSONObject().put("scope",data.getString("scope")).put("key",key).put("enabled",enabled)) { subscriptions=it as JSONObject }
    }
    fun loadScores(league:String)=run("scores.list",JSONObject().put("league",league)) { scores=(it as JSONArray).objects() }
    fun refreshPinned() {
        val event=pinned?:return
        run("scores.list",JSONObject().put("league",event.getString("league"))) { value ->
            (value as JSONArray).objects().firstOrNull{it.optString("id")==event.optString("id")}?.let { if(pinned?.optString("id")==event.optString("id"))pinned=it }
        }
    }
    fun refreshDownloads() = run("downloads.list") { downloads=it as JSONObject }
    fun refreshAddons() = run("addons.list") { addons=it as JSONObject }
    fun pollParty() = run("party.poll") { party=it as JSONObject }
    fun join(code:String) = run("party.join",JSONObject().put("code",code)) { party=it as JSONObject;partyLoop() }
    fun create() = run("party.create") { party=it as JSONObject;partyLoop() }
    fun leave() { partyEpoch++;partyJob?.cancel();partyJob=null;party=party?.let{JSONObject().put("scope",it.optString("scope")).put("room",JSONObject.NULL).put("pending",JSONObject.NULL)};run("party.leave") { party=it as JSONObject } }
    private fun partyLoop() {
        partyJob?.cancel();val epoch=generation;val roomEpoch=partyEpoch
        partyJob=scope.launch {
            while(epoch==generation && roomEpoch==partyEpoch) {
                delay(2500)
                if(dialog=="party")continue
                if(playbackActive && party?.optBoolean("host")!=true)continue
                try {
                    val value=bridge.call(if(party?.optBoolean("host")==true)"party.poll" else "party.state") as JSONObject
                    if(epoch!=generation || roomEpoch!=partyEpoch)break
                    party=value
                    if(value.isNull("room") && value.isNull("pending"))break
                } catch(e:Exception) { if(epoch==generation)error=e.message }
            }
        }
    }
    fun send(body:String,kind:String="chat") {
        val p=party?:return
        run("party.send",JSONObject().put("scope",p.getString("scope")).put("room",p.getString("room")).put("body",body).put("kind",kind)) { pollParty() }
    }
    fun decide(request:JSONObject,approve:Boolean) {
        val p=party?:return
        run("party.decide",JSONObject().put("scope",p.getString("scope")).put("code",request.getString("room_code")).put("guest",request.getString("user_id")).put("approve",approve)) { pollParty() }
    }
    fun tick(session:PlaybackSession,position:Long,playing:Boolean,onRemote:(JSONObject)->Unit) {
        val p=party?:return
        if(p.isNull("room") || p.optString("room").isBlank())return
        if(tickActive)return
        tickActive=true
        val item=session.item
        val state=JSONObject().put("cloudId",item.optString("cloudId")).put("season",session.season).put("episode",session.episode)
            .put("positionMs",position).put("playing",playing).put("liveChannelId",item.optString("liveChannelId"))
            .put("liveProvider",item.optString("liveProvider")).put("liveTitle",if(item.optString("type")=="live")item.optString("title") else "").put("liveType",item.optString("liveType","tv"))
        val epoch=generation;val roomEpoch=partyEpoch
        scope.launch {
            try {
                val value=bridge.call("party.tick",JSONObject().put("scope",p.getString("scope")).put("room",p.getString("room")).put("playback",state)) as JSONObject
                if(epoch==generation && roomEpoch==partyEpoch && party?.optString("room")==p.optString("room")){
                    for(key in listOf("requests","events"))if(!value.has(key)&&party?.has(key)==true)value.put(key,party!!.get(key))
                    party=value;onRemote(value)
                }
            } catch(e:Exception){if(epoch==generation)error=e.message}
            finally {if(epoch==generation)tickActive=false}
        }
    }
    fun requestPlaying(playing:Boolean) {
        val p=party?:return
        run("party.tick",JSONObject().put("scope",p.getString("scope")).put("room",p.getString("room")).put("request",playing)) { party=it as JSONObject }
    }
    fun add(manifest:String) {
        val data=addons?:return
        run("addons.add",JSONObject().put("scope",data.getString("scope")).put("manifest",manifest)) { addons=it as JSONObject }
    }
    fun removeAddon(id:String) {
        val data=addons?:return
        run("addons.remove",JSONObject().put("scope",data.getString("scope")).put("id",id)) { addons=it as JSONObject }
    }
    fun order(ids:List<String>) {
        val data=addons?:return
        run("addons.order",JSONObject().put("scope",data.getString("scope")).put("language",data.getString("language")).put("ids",JSONArray(ids))) { addons=it as JSONObject }
    }
    fun transfer(item:JSONObject,season:Int=0,episode:Int=0,fullSeason:Boolean=false) {
        run("settings.get") { value ->
            val owner=(value as JSONObject).getString("scope")
            val args=JSONObject().put("scope",owner).put("cloud_id",item.getString("cloudId")).put("season",season)
            if(!fullSeason)args.put("episode",episode)
            run(if(fullSeason)"downloads.season" else "downloads.start",args) { plan ->
                if(!fullSeason) refreshDownloads()
                else {
                    val epoch=generation
                    scope.launch {
                        try {
                            val numbers=(plan as JSONObject).getJSONArray("episodes")
                            for(index in 0 until numbers.length()) {
                                if(epoch!=generation || roomEpoch!=partyEpoch)break
                                try { bridge.call("downloads.start",JSONObject(args.toString()).put("episode",numbers.getInt(index))) }
                                catch(e:Exception) { if(epoch==generation)error="Algunos episodios no ofrecen un archivo descargable." }
                                delay(50)
                            }
                            if(epoch==generation)refreshDownloads()
                        } catch(e:Exception) { if(epoch==generation)error=e.message }
                    }
                }
            }
        }
    }
    fun downloadAction(method:String,id:String,done:(JSONObject)->Unit={}) {
        val data=downloads?:return
        run(method,JSONObject().put("scope",data.getString("scope")).put("id",id)) { done(it as JSONObject);refreshDownloads() }
    }
    fun prepareUpdate() = run("updates.prepare") { updates=it as JSONObject }
    fun pollUpdate() = run("updates.poll") { updates=(updates?:JSONObject()).put("status",(it as JSONObject).optString("status")) }
    fun installUpdate() = run("updates.install") { updates=JSONObject().put("status","launched") }
}
