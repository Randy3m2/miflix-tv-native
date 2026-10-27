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
    var party by mutableStateOf<JSONObject?>(null)
    var updates by mutableStateOf<JSONObject?>(null)
    var error by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    private var generation=0L
    fun reset() { generation++; dialog=null;downloads=null;addons=null;profiles=null;party=null;updates=null;error=null;busy=false }
    fun run(method: String, params: JSONObject=JSONObject(), done:(Any)->Unit={}) {
        val epoch=generation
        scope.launch {
            busy=true;error=null
            try { val value=bridge.call(method,params);if(epoch==generation)done(value) }
            catch(e:Exception) { if(epoch==generation)error=e.message }
            finally { if(epoch==generation)busy=false }
        }
    }
    fun open(kind: String) {
        dialog=kind;error=null
        when(kind) {
            "addons" -> refreshAddons()
            "profiles" -> run("profiles.list") { profiles=it as JSONObject }
            "party" -> pollParty()
            "updates" -> run("updates.check") { updates=it as JSONObject }
        }
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
    fun refreshDownloads() = run("downloads.list") { downloads=it as JSONObject }
    fun refreshAddons() = run("addons.list") { addons=it as JSONObject }
    fun pollParty() = run("party.poll") { party=it as JSONObject }
    fun join(code:String) = run("party.join",JSONObject().put("code",code)) { party=it as JSONObject }
    fun create() = run("party.create") { party=it as JSONObject }
    fun leave() = run("party.leave") { party=it as JSONObject }
    fun send(body:String,kind:String="chat") {
        val p=party?:return
        run("party.send",JSONObject().put("scope",p.getString("scope")).put("room",p.getString("room")).put("body",body).put("kind",kind)) { pollParty() }
    }
    fun decide(request:JSONObject,approve:Boolean) {
        val p=party?:return
        run("party.decide",JSONObject().put("scope",p.getString("scope")).put("code",request.getString("room_code")).put("guest",request.getString("user_id")).put("approve",approve)) { pollParty() }
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
                                if(epoch!=generation)break
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
