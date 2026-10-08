package com.example.businesscard.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.theme.BusinessCardTheme

/**
 * デザインシステムのカタログ。見本の画像と同じ並び(2列 x 4段)で全部品を置く。
 * Android Studio のプレビューと、スクリーンショットテストの両方で使う。
 */
@Composable
internal fun SoftGlassCatalog(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    val dropdownOptions = listOf("Dropdown", "Option A", "Option B")
    var dropdownIndex by remember { mutableIntStateOf(0) }
    var segment by remember { mutableIntStateOf(0) }
    var switchOn by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(1) }

    SoftGlassBackground(modifier) {
        Column(
            modifier = Modifier.padding(start = 20.dp, top = 48.dp, end = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            CatalogRow(
                left = {
                    CatalogItem("Launch") {
                        GlassButton(text = "Launch", onClick = {}, modifier = Modifier.fillMaxWidth())
                    }
                },
                right = {
                    CatalogItem("Secondary") {
                        GlassButton(
                            text = "Secondary",
                            onClick = {},
                            style = GlassButtonStyle.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
            CatalogRow(
                left = {
                    GlassTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = "Icon button",
                        placeholder = "Text field",
                        leadingIcon = GlassIcons.ForwardArrow,
                    )
                },
                right = {
                    CatalogItem("Dropdown") {
                        GlassDropdown(
                            selected = dropdownOptions[dropdownIndex],
                            options = dropdownOptions,
                            onSelect = { dropdownIndex = it },
                        )
                    }
                },
            )
            CatalogRow(
                left = {
                    CatalogItem("Toggle") {
                        GlassSegmentedControl(
                            options = listOf("List", "Grid", "Tabs"),
                            selectedIndex = segment,
                            onSelect = { segment = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                right = {
                    CatalogItem("Toggle") {
                        GlassSwitch(checked = switchOn, onCheckedChange = { switchOn = it })
                    }
                },
            )
            CatalogRow(
                left = {
                    CatalogItem("Tabs") {
                        GlassTabs(
                            tabs = listOf("All", "Tabs", "New"),
                            selectedIndex = tab,
                            onSelect = { tab = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                right = {
                    CatalogItem("Alert plan") {
                        GlassButton(text = "Premium plan", onClick = {}, modifier = Modifier.fillMaxWidth())
                    }
                },
            )
        }
    }
}

@Composable
private fun CatalogRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Box(Modifier.weight(1f)) { left() }
        Box(Modifier.weight(1f)) { right() }
    }
}

@Composable
private fun CatalogItem(label: String, content: @Composable () -> Unit) {
    Column {
        GlassLabel(text = label)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Preview(name = "Soft Glass catalog", widthDp = 412, heightDp = 640)
@Composable
private fun SoftGlassCatalogPreview() {
    BusinessCardTheme { SoftGlassCatalog() }
}
