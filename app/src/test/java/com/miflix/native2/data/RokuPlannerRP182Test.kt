package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class RokuPlannerRP182Test {
    private fun target(hls:Boolean=false,live:Boolean=false)=CastTarget("r","Roku",null,
        CastCapabilities(CastDestinationKind.ROKU_MEDIA_PLAYER,supportsHls=hls,supportsLive=live))
    @Test fun rokuAlwaysRequiresTheMediaPlayerBrowseRoute() {
        val media=CastMediaDescriptor("m","Movie",CastMediaKind.MOVIE,"https://video.test/m.mp4","video/mp4",CastProtocol.MP4)
        assertTrue(UniversalCastPlannerRP173.plan(target(),media) is CastPlan.RokuMediaPlayer)
    }
    @Test fun liveHlsIsNotClaimedCompatibleWithoutBothCapabilities() {
        val media=CastMediaDescriptor("l","Channel",CastMediaKind.LIVE_CHANNEL,"https://video.test/l.m3u8","application/vnd.apple.mpegurl",CastProtocol.HLS,isLive=true)
        assertTrue(UniversalCastPlannerRP173.plan(target(),media) is CastPlan.Unsupported)
        assertTrue(UniversalCastPlannerRP173.plan(target(hls=true),media) is CastPlan.Unsupported)
        assertTrue(UniversalCastPlannerRP173.plan(target(hls=true,live=true),media) is CastPlan.RokuMediaPlayer)
    }
}
