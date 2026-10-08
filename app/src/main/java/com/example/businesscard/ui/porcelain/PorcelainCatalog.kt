package com.example.businesscard.ui.porcelain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Porcelain のカタログ。見本の画像と同じ並び(3列 x 5段、2段目と3段目の間を光る波が横切る)で部品を置く。
 * Android Studio のプレビューと、スクリーンショットテストの両方で使う。
 *
 * 見本より部品が大きい(タップしやすい高さにした)ので、横長の部品の文字は短くしている。
 */
@Composable
fun PorcelainCatalog(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    PorcelainBackground(modifier, wave = WavePlacement.BelowTop(300.dp)) {
        Column(
            modifier = Modifier.padding(start = 14.dp, top = 22.dp, end = 14.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            CatalogRow {
                Small { PorcelainButton("Default", {}, tone = PorcelainTone.Light) }
                Small { PorcelainButton("Hover", {}) }
                Wide { PorcelainButton("Glow", {}, leadingIcon = PorcelainIcons.Ring, glow = 1f) }
            }
            CatalogRow {
                Small { PorcelainButton("Hover", {}) }
            }
            // 光る波の縁(板の上から 300dp)をまたぐ
            Spacer(Modifier.height(134.dp))
            CatalogRow {
                Small { PorcelainButton("Active", {}, tone = PorcelainTone.Light, active = true) }
                Small { PorcelainButton("Active", {}, active = true) }
                Wide { PorcelainButton("Inset", {}, leadingIcon = Icons.Filled.Check) }
            }
            CatalogRow {
                Small { PorcelainButton("Loaded", {}, tone = PorcelainTone.Light) }
                Small { PorcelainButton("Loading", {}) }
                Wide { PorcelainButton("Done", {}, leadingIcon = Icons.Outlined.CheckCircle) }
            }
            CatalogRow {
                Small { PorcelainButton("Success", {}) }
                Small { PorcelainButton("Success", {}) }
                Wide { PorcelainButton("Muted", {}, leadingIcon = Icons.Outlined.CheckCircle, enabled = false) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PorcelainTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Text field",
                    placeholder = "Name",
                    leadingIcon = Icons.Outlined.Person,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PorcelainIconButton(onClick = {}, icon = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                PorcelainIconButton(
                    onClick = {},
                    icon = Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    tone = PorcelainTone.Dark,
                )
                PorcelainChoice(text = "Choice", selected = false, onClick = {}, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CatalogRow(content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun RowScope.Small(content: @Composable () -> Unit) {
    Box(Modifier.width(108.dp), propagateMinConstraints = true) { content() }
}

@Composable
private fun RowScope.Wide(content: @Composable () -> Unit) {
    Box(Modifier.weight(1f), propagateMinConstraints = true) { content() }
}

@Preview(name = "Porcelain catalog", widthDp = 412, heightDp = 892)
@Composable
private fun PorcelainCatalogPreview() {
    PorcelainTheme { PorcelainCatalog() }
}
