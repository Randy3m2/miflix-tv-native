package com.miflix.native2.ui

import android.os.Looper
import androidx.compose.runtime.*
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Local scheduled playback boundary, not a network poll or a Watch Party command. */
@Composable
internal fun MomentPlaybackBoundary(player: ExoPlayer,req: PlayerRequest,onEnd: () -> Unit,onUnavailable: () -> Unit) {
    val endCallback=rememberUpdatedState(onEnd)
    val unavailable=rememberUpdatedState(onUnavailable)
    DisposableEffect(player,req.momentStartMs,req.momentEndMs) {
        val enabled=req.momentEndMs>req.momentStartMs && req.momentEndMs>0
        var disposed=false
        var finishing=false
        val gate=com.miflix.native2.player.MomentBoundaryGate(req.momentEndMs.coerceAtLeast(1))
        fun finish() {
            if(disposed || finishing || !enabled || !gate.claimEnd()) return
            finishing=true
            player.pause()
            player.seekTo(req.momentEndMs.coerceAtMost(player.duration.takeIf {it>0} ?: req.momentEndMs))
            endCallback.value()
            finishing=false
        }
        fun checkDuration() {
            if(enabled && player.duration>0 && req.momentEndMs>player.duration && !disposed) {
                player.pause();unavailable.value()
            }
        }
        val listener=object : Player.Listener {
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo,newPosition: Player.PositionInfo,reason: Int) {
                if(enabled) gate.movedTo(newPosition.positionMs)
                if(enabled && !finishing && newPosition.positionMs>=req.momentEndMs) finish()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if(state==Player.STATE_READY) checkDuration()
                if(state==Player.STATE_ENDED && enabled) finish()
            }
        }
        val message=if(enabled) player.createMessage { _, _ -> finish() }
            .setLooper(Looper.getMainLooper()).setPosition(0,req.momentEndMs).setDeleteAfterDelivery(false).send() else null
        if(enabled) {player.addListener(listener);checkDuration();if(player.currentPosition>=req.momentEndMs) finish()}
        onDispose {disposed=true;message?.cancel();if(enabled) player.removeListener(listener)}
    }
}
