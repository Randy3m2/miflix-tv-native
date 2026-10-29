package com.miflix.native2.ui

import com.miflix.native2.data.*

/** Only completed, explicitly submitted input reaches this dispatcher. Never called while typing. */
internal object AssistantDirectRP156 {
    fun run(state:AppState,request:String):Boolean {
        val command=AssistantCommandsRP155.parse(request) ?: return false
        when(command.action) {
            AssistantActionRP155.STANDINGS -> {state.assistantLeagueRP156=command.target;state.normalLiveEntryRP85=false;state.screen=Screen.LIVE_TV;return true}
            AssistantActionRP155.PLAYBACK -> return state.assistantPlaybackRP155?.invoke(command.target)==true
            AssistantActionRP155.NAVIGATE -> {
                val destination=mapOf("home" to Screen.HOME,"search" to Screen.SEARCH,"live" to Screen.LIVE_TV,"favorites" to Screen.MY_LIST,"notifications" to Screen.NOTIFICATIONS,"settings" to Screen.SETTINGS,"profiles" to Screen.PROFILES,"friends" to Screen.FRIENDS,"franchises" to Screen.COLLECTIONS,"downloads" to Screen.DOWNLOADS)[command.target]
                if(destination!=null) {state.screen=destination;return true}
                if(command.target in listOf("addons","language","playback","appearance")) {state.assistantSettingsCategoryRP155=if(command.target=="addons") "sources" else command.target;state.screen=Screen.SETTINGS;return true}
            }
            else -> Unit
        }
        return false
    }
}
