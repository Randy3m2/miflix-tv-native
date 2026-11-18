package com.miflix.native2.ui

import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import com.miflix.native2.data.WarmVideoPolicyRP91
import com.miflix.native2.player.SubtitleTiming

internal fun tvVodLoadControl(lowMemory:Boolean=false):LoadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(10000,30000,500,1500)
    .setTargetBufferBytes(WarmVideoPolicyRP91.bufferBytes(lowMemory))
    .setPrioritizeTimeOverSizeThresholds(false).build()

internal data class PreparedVideoRP91(
    val identity:Any,val imdb:String,val request:PlayerRequest,val originalUrl:String,
    val player:ExoPlayer,val timing:SubtitleTiming,val createdAt:Long,
    var listener:Player.Listener?=null,var expiryJob:kotlinx.coroutines.Job?=null
)
