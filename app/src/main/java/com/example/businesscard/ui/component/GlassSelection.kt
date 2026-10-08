package com.example.businesscard.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.theme.SoftGlassShapes
import com.example.businesscard.ui.theme.SoftGlassSize
import com.example.businesscard.ui.theme.SoftGlassTheme
import com.example.businesscard.ui.theme.SoftGlassType

/** 見本の「Dropdown」。すりガラスの欄の右端に下向きの山形。押すと選択肢のメニューが開く。 */
@Composable
fun GlassDropdown(
    selected: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SoftGlassTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SoftGlassSize.control)
                .glassSurface(shape = SoftGlassShapes.control, glow = GlassGlow.Soft)
                .clip(SoftGlassShapes.control)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected,
                style = SoftGlassType.field,
                color = c.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = c.ink)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(text = option, style = SoftGlassType.field, color = c.ink) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** 見本の左の「Toggle」。項目を切り替える横並びのボタン。選択中だけ明るいタイルになる。 */
@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSegmentRow(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        role = Role.RadioButton,
        selectedGlowEdge = GlowEdge.Bottom,
        showDividers = true,
        modifier = modifier,
    )
}

/** 見本の「Tabs」。選択中のタブは上端が光るタイルになる。 */
@Composable
fun GlassTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSegmentRow(
        options = tabs,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        role = Role.Tab,
        selectedGlowEdge = GlowEdge.Top,
        showDividers = false,
        modifier = modifier,
    )
}

@Composable
private fun GlassSegmentRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    role: Role,
    selectedGlowEdge: GlowEdge,
    showDividers: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = SoftGlassTheme.colors
    Row(
        modifier = modifier
            .height(SoftGlassSize.control)
            .glassSurface(shape = SoftGlassShapes.control, glow = GlassGlow.Soft)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            if (showDividers && index > 0 && !selected && index - 1 != selectedIndex) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.5f)
                        .background(c.glassBorder.copy(alpha = 0.35f)),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selected) {
                            Modifier.glassSurface(
                                shape = SoftGlassShapes.control,
                                fill = c.glassSelected,
                                glow = GlassGlow.Medium,
                                glowEdge = selectedGlowEdge,
                                castShadow = false,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clip(SoftGlassShapes.control)
                    .selectable(selected = selected, role = role, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = SoftGlassType.field,
                    color = if (selected) c.ink else c.inkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/**
 * 見本の右の「Toggle」(スイッチ)。白い縁の細長い枠の中を、光る白い玉が左右に動く。
 * オンのときは玉が右に寄り、枠の中が明るくなる。
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SoftGlassTheme.colors
    val inset = 5.dp
    val thumbX by animateDpAsState(
        targetValue = if (checked) SoftGlassSize.switchWidth - SoftGlassSize.switchThumb - inset else inset,
        label = "thumb",
    )
    Box(
        modifier = modifier
            .size(width = SoftGlassSize.switchWidth, height = SoftGlassSize.switchHeight)
            .glassSurface(
                shape = SoftGlassShapes.pill,
                fill = if (checked) c.glassSelected else c.glass.copy(alpha = 0.08f),
                glow = GlassGlow.None,
                border = c.focusBorder.copy(alpha = 0.85f),
                borderWidth = 2.dp,
            )
            .clip(SoftGlassShapes.pill)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .size(SoftGlassSize.switchThumb)
                .drawBehind {
                    val r = size.minDimension / 2f
                    // 玉のまわりの光
                    drawGlow(center, r * 1.9f, r * 1.9f, c.glow.copy(alpha = 0.6f))
                    // 玉(上が明るい白)
                    drawCircle(
                        brush = Brush.verticalGradient(listOf(c.thumbTop, c.thumbBottom)),
                        radius = r,
                    )
                    drawCircle(
                        color = c.glow.copy(alpha = 0.9f),
                        radius = r - 0.5.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                },
        )
    }
}
