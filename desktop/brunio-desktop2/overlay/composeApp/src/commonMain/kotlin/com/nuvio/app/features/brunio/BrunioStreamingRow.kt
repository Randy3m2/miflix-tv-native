package com.nuvio.app.features.brunio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.core.ui.NuvioDesktopAnimation

/**
 * Exactly Nuvio's original landscape collection artwork; no recreated wordmarks.
 * https://github.com/rrevanth/nuvio-assets at pinned commit.
 */
private object NuvioStreamingArt {
    private const val base =
        "https://raw.githubusercontent.com/rrevanth/nuvio-assets/" +
        "6013348026be6a943aaec367a4681e731dbe50ab/streaming"

    private data class Files(val cover: String, val focus: String?)
    private val files = mapOf(
        "netflix" to Files("netflix/netflix-landscape.gif", "netflix/netflix.gif"),
        "disney" to Files("disney/disney-landscape.png", "disney/disney.gif"),
        "prime" to Files("prime-video/prime-video-landscape.png", "prime-video/prime-video.gif"),
        "apple" to Files("apple-tv/apple-tv-landscape.jpg", "apple-tv/apple-tv.gif"),
        "hbo" to Files("hbo-max/hbo-max-landscape.gif", "hbo-max/hbo-max-landscape.gif"),
        "crunchyroll" to Files("crunchyroll/crunchyroll-landscape.gif", "crunchyroll/crunchyroll.gif"),
        "hulu" to Files("hulu/hulu.gif", "hulu/hulu.gif"),
        "paramount" to Files("paramount/paramount.gif", "paramount/paramount.gif"),
        "amcplus" to Files("", null),
    )

    fun cover(id: String) = files[id]?.cover?.takeIf(String::isNotEmpty)?.let { "$base/$it" }
        ?: brunioStreamingLogoUri(id)
    fun focus(id: String) = files[id]?.focus?.let { "$base/$it" }
}

/** Shows Nuvio landscape artwork and labels, separate from Home row configuration. */
@Composable
internal fun BrunioStreamingRow(
    onRefresh: () -> Unit,
    onOpenProvider: (BrunioHomeRow) -> Unit = {},
    sectionPadding: Dp = 28.dp,
) {
    val enabled by BrunioHomePreferences.selected.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    val streaming = remember {
        listOf("netflix", "disney", "prime", "apple", "hbo", "crunchyroll",
            "hulu", "paramount", "amcplus").mapNotNull { id ->
                BrunioOptionalHomeRows.firstOrNull { it.id == id }
            }
    }

    // Original Nuvio layout: heading, wide 16:9 landscape artwork, name below.
    // No border/checkmark/configuration state in any provider tile.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Streaming",
            modifier = Modifier.padding(horizontal = sectionPadding)
                .combinedClickable(
                    onClick = { showEditor = true },
                    onLongClick = { showEditor = true },
                ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = sectionPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(streaming, key = { it.id }) { provider ->
                val hoverSource = remember { MutableInteractionSource() }
                val hovered by hoverSource.collectIsHoveredAsState()
                val cover = if (hovered) {
                    NuvioStreamingArt.focus(provider.id) ?: NuvioStreamingArt.cover(provider.id)
                } else NuvioStreamingArt.cover(provider.id)
                Column(
                    modifier = Modifier.width(340.dp)
                        .clickable { onOpenProvider(provider) },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(192.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF191919))
                            .hoverable(hoverSource),
                        contentAlignment = Alignment.Center,
                    ) {
                        NuvioAsyncImage(
                            model = cover,
                            contentDescription = provider.label,
                            modifier = Modifier.fillMaxWidth().height(192.dp),
                            contentScale = ContentScale.Crop,
                            desktopAnimation = NuvioDesktopAnimation.Always,
                        )
                    }
                    Text(
                        provider.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFECECEC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    // Keep the row management capability; it is not part of the visual cards.
    if (showEditor) {
        BrunioReadableSurface {
            AlertDialog(
                onDismissRequest = { showEditor = false },
                title = { Text("Personalizar filas del Home") },
                text = {
                    LazyColumn(modifier = Modifier.height(390.dp)) {
                        items(BrunioOptionalHomeRows, key = { it.id }) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    BrunioHomePreferences.toggle(row.id)
                                    onRefresh()
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = row.id in enabled,
                                    onCheckedChange = {
                                        BrunioHomePreferences.toggle(row.id)
                                        onRefresh()
                                    },
                                )
                                Text(
                                    row.label,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = BrunioDarkColors.onSurface,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    OutlinedButton(onClick = { showEditor = false }) { Text("Listo") }
                },
                containerColor = BrunioDarkColors.surface,
                titleContentColor = BrunioDarkColors.onSurface,
                textContentColor = BrunioDarkColors.onSurface,
            )
        }
    }
}
