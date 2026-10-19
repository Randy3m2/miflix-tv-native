package com.miflix.native2.data

/** Rendered format wins over release names. No probing or network requests. */
internal object PlaybackBadgePolicyRP26 {
    data class Input(val reported:String="",val videoKnown:Boolean=false,val height:Int=-1,val width:Int=-1,
        val videoMime:String="",val videoCodecs:String="",val transfer:Int=-1,
        val audioKnown:Boolean=false,val audioMime:String="",val audioCodecs:String="",
        val channels:Int=-1,val language:String="")
    data class Badges(val resolution:String="",val hdr:String="",val audio:String="",
        val channels:String="",val language:String="")
    fun read(i:Input):Badges {
        val source=i.reported.lowercase(java.util.Locale.ROOT)
        fun has(pattern:String)=Regex(pattern,RegexOption.IGNORE_CASE).containsMatchIn(source)
        val resolution=if(i.videoKnown) when {i.height>=4000 || i.width>=7500->"8K";i.height>=2000 || i.width>=3800->"4K";i.height>=1000 || i.width>=1900->"1080p";i.height>=700 || i.width>=1250->"720p";i.height>0->"SD";else->""}
            else when {has("\\b(8k|4320p?)\\b")->"8K";has("\\b(4k|2160p?|uhd)\\b")->"4K";has("\\b1080p?\\b")->"1080p";has("\\b720p?\\b")->"720p";else->""}
        val v=(i.videoMime+" "+i.videoCodecs).lowercase(java.util.Locale.ROOT)
        val hdr=if(i.videoKnown) when {"dolby-vision" in v || Regex("\\b(dvhe|dvh1|dva1|dvav)").containsMatchIn(v)->"Dolby Vision";i.transfer==6->"HDR10";i.transfer==7->"HLG";else->""}
            else when {has("\\bdolby[ ._-]*vision\\b|\\b(dovi|dv)\\b")->"Dolby Vision";has("\\bhdr10\\+")->"HDR10+";has("\\bhdr10\\b")->"HDR10";has("\\bhlg\\b")->"HLG";has("\\bhdr\\b")->"HDR";else->""}
        val a=(i.audioMime+" "+i.audioCodecs).lowercase(java.util.Locale.ROOT)
        val audio=if(i.audioKnown) when {"eac3-joc" in a || "ec+3" in a->"Atmos";"true-hd" in a || "truehd" in a->"TrueHD";"eac3" in a || "ec-3" in a->"Dolby Digital+";"ac3" in a || "ac-3" in a->"Dolby Digital";"dts-hd" in a->"DTS-HD";"dts" in a->"DTS";"flac" in a->"FLAC";"aac" in a || "mp4a" in a->"AAC";else->""}
            else when {has("\\batmos\\b")->"Atmos";has("\\btrue[ ._-]*hd\\b")->"TrueHD";has("\\b(e[ ._-]*ac[ ._-]*3|ddp)\\b|\\bdd\\+")->"Dolby Digital+";has("\\bac[ ._-]*3\\b")->"Dolby Digital";has("\\bdts[ ._-]*hd\\b")->"DTS-HD";has("\\bdts\\b")->"DTS";has("\\baac\\b")->"AAC";else->""}
        val channels=if(i.audioKnown) when(i.channels) {1->"Mono";2->"Stereo";6->"5.1";8->"7.1";else->""}
            else Regex("(?<![0-9])([257]\\.1)(?![0-9])").find(source)?.groupValues?.get(1).orEmpty()
        return Badges(resolution,hdr,audio,channels,i.language.takeUnless {it in listOf("und","")}.orEmpty())
    }
}
