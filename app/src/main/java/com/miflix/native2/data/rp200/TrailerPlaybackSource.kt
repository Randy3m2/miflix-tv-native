/* Adapted from NuvioTV (GPL-3.0), https://github.com/NuvioMedia/NuvioTV .
 * Modified for BruniO RP200. See RP200_NUVIO_ATTRIBUTION.md for license details. */
package com.miflix.native2.data.rp200

data class TrailerPlaybackSource(
    val videoUrl: String,
    val audioUrl: String? = null
)
