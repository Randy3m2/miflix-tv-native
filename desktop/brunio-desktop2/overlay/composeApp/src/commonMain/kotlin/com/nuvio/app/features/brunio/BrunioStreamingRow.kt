package com.nuvio.app.features.brunio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.collection.Collection
import com.nuvio.app.features.collection.CollectionFolder
import com.nuvio.app.features.home.components.HomeCollectionRowSection

/**
 * The ORIGINAL Nuvio Collection row renderer, not a mock or an imitation:
 * HomeCollectionRowSection -> CollectionFolderCard -> CollectionCardRemoteImage.
 *
 * Original landscape covers/focus GIFs from rrevanth/nuvio-assets pinned to
 * commit 6013348026be6a943aaec367a4681e731dbe50ab.
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
    )
    // Nuvio's original HBO/Hulu GIFs can begin on nearly empty frames.
    // Use separately verified, static Nuvio-compatible covers at rest and
    // keep the original Nuvio animations for focused/hovered cards.
    private const val stableBase =
        "https://raw.githubusercontent.com/luckynumb3rs/stremio-perfect-setup/main/collections/streaming"
    private val staticCovers = mapOf(
        "hbo" to "$stableBase/cover/hbo-max.png",
        "hulu" to "$stableBase/cover/hulu.png",
        "amcplus" to "https://raw.githubusercontent.com/Randy3m2/miflix-tv-native/main/app/src/main/res/drawable-nodpi/streaming_amcplus_rp48.png",
    )
    private val titleLogos = mapOf(
        "hbo" to "$stableBase/title/hbo-max.png",
        "hulu" to "$stableBase/title/hulu.png",
        // AMC+ has no cover in rrevanth/nuvio-assets. Its logo remains
        // legible even if its bundled background is too dark.
        "amcplus" to "https://raw.githubusercontent.com/Randy3m2/miflix-tv-native/main/app/src/main/res/drawable-nodpi/streaming_amcplus_rp48.png",
    )
    fun cover(id: String): String? = staticCovers[id]
        ?: files[id]?.cover?.let { "$base/$it" }
        ?: brunioStreamingLogoUri(id)
    fun focus(id: String): String? = files[id]?.focus?.let { "$base/$it" }
    fun titleLogo(id: String): String? = titleLogos[id]
}

/**
 * Uses Nuvio's actual shelf/card layout and GIF loader. No checkbox, border,
 * selected style, or fake wordmark is added to the visual card.
 */
@Composable
internal fun BrunioStreamingRow(
    onRefresh: () -> Unit,
    onOpenProvider: (BrunioHomeRow) -> Unit = {},
    onOpenMedia: ((MetaPreview) -> Unit)? = null,
    sectionPadding: Dp = 28.dp,
) {
    val enabled by BrunioHomePreferences.selected.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    var chosenProvider by remember { mutableStateOf<BrunioHomeRow?>(null) }
    var catalogLoading by remember { mutableStateOf(false) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var catalogEntries by remember { mutableStateOf(emptyList<MetaPreview>()) }

    LaunchedEffect(chosenProvider) {
        val provider = chosenProvider ?: return@LaunchedEffect
        catalogLoading = true
        catalogError = null
        catalogEntries = emptyList()
        try {
            val providerQuery = provider.query
            val movies = BrunioTmdb.catalog("discover/movie", "movie", providerQuery)
            val series = BrunioTmdb.catalog("discover/tv", "series", providerQuery)
            catalogEntries = (movies + series).distinctBy { it.id }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            catalogError = "No se pudo cargar el catálogo. Comprueba tu conexión y TMDB."
        } finally {
            catalogLoading = false
        }
    }
    val streaming = remember {
        listOf("netflix", "disney", "prime", "apple", "hbo", "crunchyroll",
            "hulu", "paramount", "amcplus").mapNotNull { id ->
                BrunioOptionalHomeRows.firstOrNull { it.id == id }
            }
    }
    val collection = remember(streaming) {
        Collection(
            id = "brunio-nuvio-streaming",
            title = "Streaming",
            folders = streaming.map { provider ->
                CollectionFolder(
                    id = provider.id,
                    title = provider.label,
                    coverImageUrl = NuvioStreamingArt.cover(provider.id),
                    titleLogoUrl = NuvioStreamingArt.titleLogo(provider.id),
                    focusGifUrl = NuvioStreamingArt.focus(provider.id),
                    focusGifEnabled = true,
                    mobileFocusGifEnabled = true,
                    tileShape = "landscape",
                    hideTitle = false,
                )
            },
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        HomeCollectionRowSection(
            collection = collection,
            modifier = Modifier.fillMaxWidth(),
            sectionPadding = sectionPadding,
            animateGifs = true,
            onFolderClick = { _, folderId ->
                streaming.firstOrNull { it.id == folderId }?.let { provider ->
                    chosenProvider = provider
                }
            },
        )
        // Editing is separate from the visual cards: click the heading to
        // manage rows. This overlay does not alter Nuvio's shelf appearance.
        Box(
            modifier = Modifier.padding(start = sectionPadding)
                .width(195.dp).height(34.dp)
                .clickable { showEditor = true },
        )
    }

    if (chosenProvider != null) {
        BrunioReadableSurface {
            AlertDialog(
                onDismissRequest = { chosenProvider = null },
                title = { Text(chosenProvider?.label.orEmpty()) },
                text = {
                    Column {
                        Text("Películas y series disponibles en esta plataforma.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                        if (catalogLoading) Text("Cargando catálogo…")
                        if (catalogError != null) Text(catalogError.orEmpty(),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        if (!catalogLoading && catalogError == null && catalogEntries.isEmpty()) {
                            Text("No se encontraron títulos para esta plataforma.")
                        }
                        LazyColumn(modifier = Modifier.height(440.dp)) {
                            items(catalogEntries, key = { it.id }) { media ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            chosenProvider = null
                                            onOpenMedia?.invoke(media)
                                        }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncImage(
                                        model = media.poster,
                                        contentDescription = null,
                                        modifier = Modifier.size(width = 65.dp, height = 94.dp),
                                    )
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Text(media.name, maxLines = 2,
                                            overflow = TextOverflow.Ellipsis)
                                        Text(if (media.type == "series") "Serie" else "Película",
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    OutlinedButton(onClick = { chosenProvider = null }) { Text("Cerrar") }
                },
                containerColor = BrunioDarkColors.surface,
                titleContentColor = BrunioDarkColors.onSurface,
                textContentColor = BrunioDarkColors.onSurface,
            )
        }
    }

    if (showEditor) {
        BrunioReadableSurface {
            AlertDialog(
                onDismissRequest = { showEditor = false },
                title = { Text("Personalizar filas del Home") },
                text = {
                    LazyColumn(modifier = Modifier.height(390.dp)) {
                        items(BrunioOptionalHomeRows, key = { it.id }) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
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
