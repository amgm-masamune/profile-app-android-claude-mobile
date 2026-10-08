package com.example.businesscard.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.businesscard.ui.theme.SoftGlassShapes
import com.example.businesscard.ui.theme.SoftGlassSize
import com.example.businesscard.ui.theme.SoftGlassTheme
import com.example.businesscard.ui.theme.SoftGlassType

/**
 * ボタンの種類。見本の「Launch」「Premium plan」が Primary、「Secondary」が Secondary。
 * Primary は太字で強く光る。1画面に Primary は1つまで。
 */
enum class GlassButtonStyle { Primary, Secondary, Danger }

/** 部品の上に置く小さなラベル(見本の「Launch」「Icon button」などの見出し)。 */
@Composable
fun GlassLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = SoftGlassType.label,
        color = SoftGlassTheme.colors.ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** すりガラスのボタン。押している間は膜が濃くなり、光が強くなる。 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Primary,
    icon: ImageVector? = null,
) {
    val c = SoftGlassTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val textStyle = if (style == GlassButtonStyle.Secondary) SoftGlassType.button else SoftGlassType.buttonStrong
    val contentColor = if (style == GlassButtonStyle.Danger) c.danger else c.ink
    val glow = when {
        pressed -> GlassGlow.Strong
        style == GlassButtonStyle.Primary -> GlassGlow.Strong
        style == GlassButtonStyle.Secondary -> GlassGlow.Medium
        else -> GlassGlow.Soft
    }

    Row(
        modifier = modifier
            .heightIn(min = SoftGlassSize.control)
            .glassSurface(
                shape = SoftGlassShapes.control,
                fill = if (pressed) c.glassPressed else c.glass,
                glow = glow,
            )
            .clip(SoftGlassShapes.control)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        }
        Text(
            text = text,
            style = textStyle,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 四角いアイコンボタン(見本の「Icon button」の明るいタイル)。
 *  - Secondary(既定): 明るいタイル。戻るなど
 *  - Primary: 明るいタイル + 主ボタンと同じ強い光。編集など、その画面の主操作
 *  - Danger: 危険色のアイコン。明るいタイルの上では危険色が読めないので、膜はふつうのガラスにする
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Secondary,
) {
    val c = SoftGlassTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = when {
        style == GlassButtonStyle.Danger -> if (pressed) c.glassPressed else c.glass
        pressed -> c.glassTile.copy(alpha = 0.6f)
        else -> c.glassTile
    }
    val tint = if (style == GlassButtonStyle.Danger) c.danger else c.ink
    Box(
        modifier = modifier
            .size(SoftGlassSize.iconButton)
            .glassSurface(
                shape = SoftGlassShapes.control,
                fill = fill,
                glow = if (style == GlassButtonStyle.Primary || pressed) GlassGlow.Strong else GlassGlow.Soft,
            )
            .clip(SoftGlassShapes.control)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/**
 * すりガラスの入力欄(見本の「Icon button + Text field」)。
 * ラベルは部品の上、[leadingIcon] は左端の明るいタイルに入る。
 * フォーカス中は縁が白く濃くなり、下端の光が強くなる。エラー時は縁と注記が危険色。
 */
@Composable
fun GlassTextField(
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
    val c = SoftGlassTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        isError -> c.danger
        focused -> c.focusBorder
        else -> c.glassBorder
    }

    Column(modifier = modifier) {
        GlassLabel(text = label)
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
            textStyle = SoftGlassType.field.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = keyboardOptions,
            interactionSource = interaction,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SoftGlassSize.control)
                        .glassSurface(
                            shape = SoftGlassShapes.control,
                            fill = if (focused) c.glassPressed else c.glass,
                            glow = if (focused) GlassGlow.Strong else GlassGlow.Medium,
                            border = borderColor,
                            borderWidth = if (isError || focused) 1.5.dp else 1.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Box(
                            modifier = Modifier
                                .size(SoftGlassSize.control)
                                .glassSurface(
                                    shape = SoftGlassShapes.control,
                                    fill = c.glassTile,
                                    glow = GlassGlow.Soft,
                                    castShadow = false,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = leadingIcon, contentDescription = null, tint = c.ink, modifier = Modifier.size(24.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = SoftGlassType.field, color = c.inkFaint, maxLines = 1)
                        }
                        innerTextField()
                    }
                }
            },
        )
        if (isError && errorText != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = errorText, style = SoftGlassType.caption, color = c.danger)
        }
    }
}

/**
 * 取り消せない操作の確認ダイアログ。不透明なすりガラスの板に、
 * [キャンセル(副)] [確定(危険色)] の2つのボタンを並べる。
 */
@Composable
fun GlassConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = SoftGlassTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        // 影と光がダイアログの窓の外で切れないよう、周りに余白を取る
        Box(modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 24.dp, bottom = 28.dp)) {
            Column(
                modifier = Modifier
                    .glassSurface(shape = SoftGlassShapes.dialog, fill = c.dialog, glow = GlassGlow.Soft)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = title, style = SoftGlassType.cardCompany, color = c.ink)
                Text(text = message, style = SoftGlassType.body, color = c.inkMuted)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassButton(
                        text = dismissLabel,
                        onClick = onDismiss,
                        style = GlassButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        text = confirmLabel,
                        onClick = onConfirm,
                        style = GlassButtonStyle.Danger,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
