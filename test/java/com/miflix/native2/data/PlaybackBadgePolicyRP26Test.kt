package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
class PlaybackBadgePolicyRP26Test {
    @Test fun releaseFallback() {
        val b=PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(reported="2160p Dolby Vision Atmos 5.1"))
        assertEquals("4K",b.resolution);assertEquals("Dolby Vision",b.hdr);assertEquals("Atmos",b.audio);assertEquals("5.1",b.channels)
    }
    @Test fun selectedSdrStereoOverridesMisleadingRelease() {
        val b=PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(reported="4K DV Atmos 7.1",videoKnown=true,height=1080,audioKnown=true,audioMime="audio/mp4a-latm",channels=2,language="es"))
        assertEquals("1080p",b.resolution);assertEquals("",b.hdr);assertEquals("AAC",b.audio);assertEquals("Stereo",b.channels);assertEquals("es",b.language)
    }
    @Test fun croppedUhdStill4k() {assertEquals("4K",PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(videoKnown=true,height=1600,width=3840)).resolution)}
    @Test fun dolbyVisionAndJocFromPlayer() {
        val b=PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(videoKnown=true,videoCodecs="dvhe.08.06",audioKnown=true,audioMime="audio/eac3-joc",channels=6))
        assertEquals("Dolby Vision",b.hdr);assertEquals("Atmos",b.audio);assertEquals("5.1",b.channels)
    }
    @Test fun eac3DoesNotImplyAtmos() {assertEquals("Dolby Digital+",PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(audioKnown=true,audioMime="audio/eac3",reported="Atmos")).audio)}
    @Test fun trueHdDoesNotImplyAtmos() {assertEquals("TrueHD",PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(audioKnown=true,audioMime="audio/true-hd",reported="Atmos")).audio)}
    @Test fun unknownDoesNotInventTags() {assertEquals(PlaybackBadgePolicyRP26.Badges(),PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input()))}
    @Test fun hdrTransfers() {
        assertEquals("HDR10",PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(videoKnown=true,transfer=6)).hdr)
        assertEquals("HLG",PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(videoKnown=true,transfer=7)).hdr)
    }
    @Test fun noAccidentalDvWithinWords() {assertEquals("",PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(reported="Adventure DVD")).hdr)}
    @Test fun newSourceClearsOldMetadata() {val old=PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(reported="4k DV Atmos"));val fresh=PlaybackBadgePolicyRP26.read(PlaybackBadgePolicyRP26.Input(reported="720p AAC"));assertNotEquals(old,fresh);assertEquals("",fresh.hdr);assertEquals("AAC",fresh.audio)}
}
