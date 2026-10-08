package com.example.businesscard.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.glass.glowingBall
import com.example.businesscard.ui.glass.rememberGlassFeedback
import com.example.businesscard.ui.theme.SoftGlassMotion
import com.example.businesscard.ui.theme.SoftGlassShapes
import com.example.businesscard.ui.theme.SoftGlassSize
import com.example.businesscard.ui.theme.SoftGlassTheme
import com.example.businesscard.ui.theme.SoftGlassType
import kotlin.math.abs

/**
 * 見本の「Dropdown」。すりガラスの欄の右端に下向きの山形。押すと沈み、選択肢のメニューが開く。
 * 開くと山形が上を向く。選び直すと小さく振動する。
 */
@Composable
fun GlassDropdown(
    selected: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SoftGlassTheme.colors
    val feedback = rememberGlassFeedback()
    val interaction = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, SoftGlassMotion.slideSpring, label = "chevron")
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SoftGlassSize.control)
                .glassSurface(
                    shape = SoftGlassShapes.control,
                    glow = if (expanded) GlassGlow.Medium else GlassGlow.Soft,
                    interactionSource = interaction,
                )
                .clip(SoftGlassShapes.control)
                .clickable(interactionSource = interaction, indication = null, role = Role.DropdownList) {
                    feedback.click()
                    expanded = true
                }
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
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = c.ink,
                modifier = Modifier.graphicsLayer { rotationZ = chevron },
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(text = option, style = SoftGlassType.field, color = c.ink) },
                    onClick = {
                        feedback.tick()
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** 見本の左の「Toggle」。項目を切り替える横並びのボタン。選択中だけ明るいタイルになり、ばねで滑って移る。 */
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

/** 見本の「Tabs」。選択中のタブは上端が光るタイルになり、ばねで滑って移る。 */
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

/**
 * 選択中を示すタイルは1枚だけ。選び直すと、そのタイルがばねで滑って移る(パッと切り替えない)。
 * 押した所はガラスの中で光り、切り替わると目盛りをまたぐような小さな振動を返す。
 */
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
    val feedback = rememberGlassFeedback()
    val slide by animateFloatAsState(selectedIndex.toFloat(), SoftGlassMotion.slideSpring, label = "segment")
    val count = options.size
    BoxWithConstraints(
        modifier = modifier
            .height(SoftGlassSize.control)
            .glassSurface(shape = SoftGlassShapes.control, glow = GlassGlow.Soft),
    ) {
        val segmentWidth = maxWidth / count
        // 選択中のタイル(1枚が滑って移る)
        Box(
            Modifier
                .offset(x = segmentWidth * slide)
                .width(segmentWidth)
                .fillMaxHeight()
                .glassSurface(
                    shape = SoftGlassShapes.control,
                    fill = c.glassSelected,
                    glow = GlassGlow.Medium,
                    glowEdge = selectedGlowEdge,
                    layer = GlassLayer.Inset,
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup()
                .then(if (showDividers) Modifier.segmentDividers(count, slide, c.glassBorder) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                // 文字はタイルが近づくほど明るくなる
                val nearness = (1f - abs(slide - index)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .glassTouchLight(interaction, SoftGlassShapes.control)
                        .clip(SoftGlassShapes.control)
                        .selectable(
                            selected = selected,
                            interactionSource = interaction,
                            indication = null,
                            role = role,
                            onClick = {
                                if (!selected) {
                                    feedback.tick()
                                    onSelect(index)
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        style = SoftGlassType.field,
                        color = lerp(c.inkFaint, c.ink, nearness),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

/** 選択肢の区切り線。タイルが重なっている所の線は消す。 */
private fun Modifier.segmentDividers(count: Int, slide: Float, color: Color): Modifier = drawBehind {
    for (i in 1 until count) {
        // タイルは slide..slide+1 を占める。区切り i がその中にあれば見えない
        val distance = if (i > slide && i < slide + 1f) 0f else minOf(abs(i - slide), abs(i - slide - 1f))
        val alpha = 0.35f * (distance * 3f).coerceIn(0f, 1f)
        if (alpha <= 0f) continue
        val x = size.width * i / count
        drawLine(
            color = color.copy(alpha = color.alpha * alpha),
            start = Offset(x, size.height * 0.25f),
            end = Offset(x, size.height * 0.75f),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

/**
 * 見本の右の「Toggle」(スイッチ)。白い縁の細長い枠の中を、光る白い玉がばねで左右に動く。
 * オンにすると玉が右に寄り、枠の下端の光源が灯る。押している間は玉が少し横につぶれる。
 * オン・オフで感触の違う振動を返す。
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SoftGlassTheme.colors
    val feedback = rememberGlassFeedback()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val position by animateFloatAsState(if (checked) 1f else 0f, SoftGlassMotion.thumbSpring, label = "thumb")
    val squash by animateFloatAsState(if (pressed) 1f else 0f, SoftGlassMotion.thumbSpring, label = "squash")
    val inset = 5.dp
    val travel = SoftGlassSize.switchWidth - SoftGlassSize.switchThumb - inset * 2
    Box(
        modifier = modifier
            .size(width = SoftGlassSize.switchWidth, height = SoftGlassSize.switchHeight)
            .glassSurface(
                shape = SoftGlassShapes.pill,
                fill = lerp(c.glass.copy(alpha = 0.08f), c.glassSelected, position.coerceIn(0f, 1f)),
                glow = if (checked) GlassGlow.Medium else GlassGlow.None,
                border = c.focusBorder.copy(alpha = 0.85f),
                borderWidth = 2.dp,
                interactionSource = interaction,
                haptics = false,
            )
            // つまみの光が枠の外までにじむよう、切り抜かない(押した反応はつまみと枠の動きで返す)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = {
                    feedback.toggle(it)
                    onCheckedChange(it)
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = inset + travel * position)
                .size(SoftGlassSize.switchThumb)
                .graphicsLayer {
                    scaleX = 1f + 0.18f * squash
                    scaleY = 1f - 0.08f * squash
                }
                .glowingBall(
                    top = c.thumbTop,
                    bottom = c.thumbBottom,
                    lightColor = c.glow,
                    intensity = 0.7f + 0.6f * position.coerceIn(0f, 1f),
                ),
        )
    }
}
