package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import org.junit.Assert.*
import org.junit.Test

class FrenchPlaybackRP148Test {
    @Test fun frenchTrackAndSubtitleAliasesMatch() {
        for(code in listOf("fr","fra","fre","fr-FR","fr-CA","French","Français")) assertTrue(code,StreamLanguagePriority.languageMatches(code,"fr"))
        assertFalse(StreamLanguagePriority.languageMatches("en","fr"))
    }
    @Test fun frenchSubtitlesAreNeverAssumedToBeFrenchAudio() {
        for(label in listOf("1080p VOSTFR","French subtitles","English audio | French subtitles")) assertFalse(label,StreamLanguagePriority.hasAudio("",label,"fr"))
        assertTrue(StreamLanguagePriority.hasAudio("fr-CA","English subtitles","fr"))
        assertFalse(StreamLanguagePriority.hasAudio("eng","French 1080p","fr"))
    }
    @Test fun frenchReleaseTagsAreRecognized() {
        for(tag in listOf("VFF","VFQ","TRUEFRENCH","Français","🇫🇷")) assertTrue(tag,StreamLanguagePriority.hasAudio("",tag+" 1080p","fr"))
    }
    private fun stream(id:String,lang:String,title:String)=StreamChoice("test","https://example.test/$id",title,audioLanguages=listOf(lang))
    @Test fun preferredFrenchAudioPrecedesResolutionWithoutChangingOtherLanguages() {
        val english=stream("en","en","2160p English");val french=stream("fr","fr","720p French")
        assertEquals(listOf(french,english),StreamOrdering.sorted(listOf(english,french),"fr"))
        assertEquals(listOf(english,french),StreamOrdering.sorted(listOf(french,english),"en"))
    }
    @Test fun frenchSecondaryAudioRemainsAfterPrimary() {
        val korean=stream("ko","ko","720p Korean");val french=stream("fr","fr","1080p French");val english=stream("en","en","2160p English")
        assertEquals(listOf(korean,french,english),StreamOrdering.sorted(listOf(english,french,korean),"ko",secondaryAudio="fr"))
    }
}
