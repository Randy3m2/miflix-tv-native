package com.nuvio.app.features.brunio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.core.ui.NuvioDesktopAnimation

/** Exact streaming badges/GIFs from BruniO TV; tap to add/remove the TMDB row. */
@Composable
internal fun BrunioStreamingRow(onRefresh: () -> Unit) {
    val enabled by BrunioHomePreferences.selected.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    val streaming = remember { BrunioOptionalHomeRows.filter { it.id in
        setOf("netflix", "disney", "prime", "apple", "hbo", "hulu", "paramount", "amcplus") } }
    BrunioReadableSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Streaming", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White)
                OutlinedButton(onClick = { showEditor = true }) { Text("Agregar / organizar filas") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(streaming, key = { it.id }) { provider ->
                    val active = provider.id in enabled
                    Column(
                        modifier = Modifier.width(158.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(if (active) 2.dp else 1.dp,
                                if (active) BrunioDarkColors.primary else BrunioDarkColors.outlineVariant,
                                RoundedCornerShape(14.dp))
                            .background(BrunioDarkColors.surface)
                            .clickable {
                                BrunioHomePreferences.toggle(provider.id)
                                onRefresh()
                            }.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(78.dp),
                            contentAlignment = Alignment.Center) {
                            val picture = brunioStreamingLogoUri(provider.id)
                            if (picture != null) {
                                NuvioAsyncImage(
                                    model = picture,
                                    contentDescription = provider.label,
                                    modifier = Modifier.fillMaxWidth().height(72.dp),
                                    contentScale = ContentScale.Fit,
                                    desktopAnimation = NuvioDesktopAnimation.Always,
                                )
                            } else Text(provider.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            if (active) "✓ " + provider.label else "+ " + provider.label,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = BrunioDarkColors.onSurface,
                        )
                    }
                }
            }
        }
        if (showEditor) {
            AlertDialog(
                onDismissRequest = { showEditor = false },
                title = { Text("Personalizar filas del Home") },
                text = {
                    LazyColumn(modifier = Modifier.height(380.dp)) {
                        items(BrunioOptionalHomeRows, key = { it.id }) { row ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                BrunioHomePreferences.toggle(row.id)
                                onRefresh()
                            }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = row.id in enabled,
                                    onCheckedChange = {
                                        BrunioHomePreferences.toggle(row.id)
                                        onRefresh()
                                    },
                                )
                                Text(row.label, modifier = Modifier.padding(start = 8.dp),
                                    color = BrunioDarkColors.onSurface)
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
