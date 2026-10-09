package com.example.businesscard.ui.porcelain

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.businesscard.R
import com.example.businesscard.ui.glass.rememberGlassFeedback

/** 板の色に合う文字色。 */
@Composable
private fun inkOn(tone: PorcelainTone): Color {
    val c = PorcelainTheme.colors
    return when (tone) {
        PorcelainTone.Light -> c.ink
        PorcelainTone.Dark, PorcelainTone.Muted -> c.onDark
    }
}

/**
 * 丸いボタン(pill)。見本の「Hover / Loading / Success」(濃い板)と「Default / Loaded」(明るい板)。
 * 押すと板に沈み込み(外の影が消え、内側に影ができる)、底に当たって止まる。離すとばねで浮き上がる。
 * 押す・離すで短く振動し、確定するとクリック音(端末の設定に従う)。
 *
 * @param contentColor 文字・アイコンの色。省略すると板の色に合う色
 * @param glow 縁が暖かく光る強さ(見本の Hover with glow)
 * @param active true なら押し込まれたまま(見本の Active。オンになっているトグルなど)
 */
@Composable
fun PorcelainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: PorcelainTone = PorcelainTone.Dark,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentColor: Color = inkOn(tone),
    glow: Float = 0f,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberPorcelainPress(interaction, feedback)
    val shownTone = if (enabled) tone else PorcelainTone.Muted
    val ink = if (enabled) contentColor else PorcelainTheme.colors.onDarkMuted

    Row(
        modifier = modifier
            .heightIn(min = PorcelainSize.control)
            .porcelainSurface(shape = PorcelainShapes.pill, tone = shownTone, pressed = press, sunken = active, glow = glow)
            .clip(PorcelainShapes.pill)
            // 押した反応は板そのもの(沈み込み)で返すので、標準の波紋は出さない
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
            )
            .sinkWith(press)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
        }
        Text(text = text, style = PorcelainType.button, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailingIcon != null) {
            Spacer(Modifier.weight(1f, fill = false))
            Icon(imageVector = trailingIcon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
        }
    }
}

/** 丸いアイコンボタン。戻る・編集・削除・見た目の切り替えなど。 */
@Composable
fun PorcelainIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tone: PorcelainTone = PorcelainTone.Light,
    contentColor: Color = inkOn(tone),
) {
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = rememberPorcelainPress(interaction, feedback)
    Box(
        modifier = modifier
            .size(PorcelainSize.iconButton)
            .porcelainSurface(shape = CircleShape, tone = tone, pressed = press)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
            )
            .sinkWith(press),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = contentColor, modifier = Modifier.size(24.dp))
    }
}

/** 部品の上に置く小さなラベル。 */
@Composable
fun PorcelainLabel(text: String, modifier: Modifier = Modifier, color: Color = PorcelainTheme.colors.inkMuted) {
    Text(text = text, style = PorcelainType.label, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/**
 * 入力欄。板に彫り込まれた溝(見本の inset shadow)で、左に丸いアイコン台が浮いている。
 * 入力中は縁が暖かく光る(見本の Hover with glow)。エラー時は注記が危険色になり、縁も危険色で光る。
 */
@Composable
fun PorcelainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val c = PorcelainTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(modifier = modifier) {
        PorcelainLabel(text = label, color = if (isError) c.danger else c.inkMuted, modifier = Modifier.padding(start = 6.dp))
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (isError && errorText != null) error(errorText)
                },
            singleLine = true,
            textStyle = PorcelainType.field.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = keyboardOptions,
            interactionSource = interaction,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PorcelainSize.control)
                        .porcelainSurface(
                            shape = PorcelainShapes.pill,
                            tone = PorcelainTone.Light,
                            sunken = true,
                            glow = if (focused || isError) 1f else 0f,
                            glowColor = if (isError) c.danger.copy(alpha = 0.55f) else c.focusGlow,
                            pressScale = false,
                        )
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Box(
                            modifier = Modifier
                                .size(PorcelainSize.fieldIcon)
                                .porcelainSurface(shape = CircleShape, tone = PorcelainTone.Light, elevation = SmallRaise),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = leadingIcon, contentDescription = null, tint = c.inkMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = PorcelainType.field, color = c.inkFaint, maxLines = 1)
                        }
                        innerTextField()
                    }
                }
            },
        )
        if (isError && errorText != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = errorText, style = PorcelainType.caption, color = c.danger, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

/** 入力欄の中の小さな丸い台。小さいので影も短い。 */
private val SmallRaise = PorcelainElevation(
    lightDx = (-1).dp, lightDy = (-1.5).dp, lightBlur = 3.dp, lightAlpha = 0.6f,
    dropDx = 1.dp, dropDy = 2.5.dp, dropBlur = 5.dp, dropAlpha = 0.3f,
    contactDy = 1.dp, contactBlur = 1.5.dp, contactAlpha = 0.18f,
)

/** 画面上部。戻るは左の丸いボタン、タイトルはその右、右端に [actions]。 */
@Composable
fun PorcelainTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PorcelainSpacing.lg, top = 18.dp, end = PorcelainSpacing.lg, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (onBack != null) {
            PorcelainIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = title,
            style = PorcelainType.title,
            color = PorcelainTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actions != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/** 文字だけの案内(一覧が空のときなど)。明るい板に載せる。 */
@Composable
fun PorcelainMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PorcelainType.body,
        color = PorcelainTheme.colors.inkMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
            .porcelainSurface(shape = PorcelainShapes.card, tone = PorcelainTone.Light, elevation = PorcelainElevation.Card)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    )
}

/**
 * 選択肢(見本の「✓ Verified inset shadow」「○ Hover with glow」)。
 * 選んでいるものは濃い板にチェック、選んでいないものは明るい板に空の丸。
 */
@Composable
fun PorcelainChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PorcelainButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        tone = if (selected) PorcelainTone.Dark else PorcelainTone.Light,
        leadingIcon = if (selected) Icons.Filled.Check else PorcelainIcons.Ring,
    )
}

/** ダイアログの板。中身は [content] に縦に並べる。 */
@Composable
fun PorcelainDialog(
    title: String,
    onDismiss: () -> Unit,
    message: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = PorcelainTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        // 影が窓の外で切れないよう、周りに余白を取る
        Box(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 24.dp, bottom = 28.dp)) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .porcelainSurface(shape = PorcelainShapes.dialog, tone = PorcelainTone.Light, elevation = PorcelainElevation.Dialog)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = title, style = PorcelainType.dialogTitle, color = c.ink)
                if (message != null) {
                    Text(text = message, style = PorcelainType.body, color = c.inkMuted)
                }
                Spacer(Modifier.height(4.dp))
                content()
            }
        }
    }
}

/**
 * 取り消せない操作の確認。[キャンセル] [確定] をどちらも明るい板で並べ、確定は危険色の文字にする
 * (濃い板は「進める」操作に使う色なので、取り消せない操作には使わない)。
 */
@Composable
fun PorcelainConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PorcelainDialog(title = title, message = message, onDismiss = onDismiss) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PorcelainButton(
                text = dismissLabel,
                onClick = onDismiss,
                tone = PorcelainTone.Light,
                modifier = Modifier.weight(1f),
            )
            PorcelainButton(
                text = confirmLabel,
                onClick = onConfirm,
                tone = PorcelainTone.Light,
                contentColor = PorcelainTheme.colors.danger,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
