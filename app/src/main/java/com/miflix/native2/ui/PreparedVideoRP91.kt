package com.miflix.native2.ui

import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import com.miflix.native2.data.WarmVideoPolicyRP91
import com.miflix.native2.player.SubtitleTiming

/** The same allocator/control is retained when the player becomes visible. */
internal class WarmLoadControlRP91(private val lowMemory:Boolean=false,private val normal:LoadControl=DefaultLoadControl.Builder()
    .setBufferDurationsMs(15000,50000,1000,1500).setTargetBufferBytes(48*1024*1024)
    .setPrioritizeTimeOverSizeThresholds(false).build()):LoadControl by normal {
    @Volatile private var warming=true
    fun activate() {warming=false}
    override fun shouldContinueLoading(parameters:LoadControl.Parameters):Boolean {
        if(warming && !WarmVideoPolicyRP91.continueWarm(parameters.bufferedDurationUs,normal.getAllocator(parameters.playerId).totalBytesAllocated,lowMemory)) return false
        return normal.shouldContinueLoading(parameters)
    }
    override fun shouldStartPlayback(parameters:LoadControl.Parameters):Boolean {
        if(warming) return parameters.bufferedDurationUs>=1_000_000L || normal.getAllocator(parameters.playerId).totalBytesAllocated>=WarmVideoPolicyRP91.bufferBytes(lowMemory)
        return normal.shouldStartPlayback(parameters)
    }
}

internal data class PreparedVideoRP91(
    val identity:Any,val imdb:String,val request:PlayerRequest,val originalUrl:String,
    val player:ExoPlayer,val timing:SubtitleTiming,val gate:WarmLoadControlRP91,val createdAt:Long,
    var listener:Player.Listener?=null,var expiryJob:kotlinx.coroutines.Job?=null
)
