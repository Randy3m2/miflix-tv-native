# RP200 native trailer engine attribution

The following files in `app/src/main/java/com/miflix/native2/data/rp200/` adapt files from [NuvioTV](https://github.com/NuvioMedia/NuvioTV), branch `dev`, distributed under the GNU General Public License version 3 (GPL-3.0):
- InAppYouTubeExtractor.kt
- YoutubeChunkedDataSourceFactory.kt
- YouTubeRateLimitGate.kt
- VideoDecoderSupport.kt
- TrailerPlaybackSource.kt

Changes include package relocation, removing dependency injection and Nuvio-specific networking customizations, and integration with BruniO. Attribution does not imply affiliation or endorsement. Incorporating and distributing this GPLv3-derived code entails license obligations for the combined work; review the complete application licensing prior to publishing APKs. See `RP200_GPLv3_LICENSE.txt`.
