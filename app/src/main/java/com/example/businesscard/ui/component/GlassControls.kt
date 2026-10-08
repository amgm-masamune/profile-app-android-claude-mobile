package com.example.businesscard.ui.component

import android.view.WindowManager
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.example.businesscard.ui.glass.LocalGlassScene
import com.example.businesscard.ui.glass.rememberGlassFeedback
import com.example.businesscard.ui.theme.SoftGlassLight
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

/**
 * すりガラスのボタン。押すと指に吸い寄せられて手前へ浮き(少し大きくなり、影が伸びる)、指の所が光り、光源が強まる。
 * 離すとばねで戻る。押す・離すで短く振動し、確定するとクリック音(端末の設定に従う)。
 */
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
    val feedback = rememberGlassFeedback()
    val textStyle = if (style == GlassButtonStyle.Secondary) SoftGlassType.button else SoftGlassType.buttonStrong
    val contentColor = if (style == GlassButtonStyle.Danger) c.danger else c.ink
    val glow = when (style) {
        GlassButtonStyle.Primary -> GlassGlow.Strong
        GlassButtonStyle.Secondary -> GlassGlow.Medium
        GlassButtonStyle.Danger -> GlassGlow.Soft
    }

    Row(
        modifier = modifier
            .heightIn(min = SoftGlassSize.control)
            .glassSurface(
                shape = SoftGlassShapes.control,
                fill = if (pressed) c.glassPressed else c.glass,
                glow = glow,
                interactionSource = interaction,
            )
            .clip(SoftGlassShapes.control)
            // 押した反応はガラス自身(指へ寄る動き・指の所の光)で返すので、標準の波紋は出さない
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
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
    val feedback = rememberGlassFeedback()
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
                glow = if (style == GlassButtonStyle.Primary) GlassGlow.Strong else GlassGlow.Soft,
                interactionSource = interaction,
            )
            .clip(SoftGlassShapes.control)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    feedback.click()
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/**
 * すりガラスの入力欄(見本の「Icon button + Text field」)。
 * ラベルは部品の上、[leadingIcon] は左端の明るいタイルに入る。
 * フォーカス中は縁が白く濃くなり、下端の光が強くなり、少し手前に浮き上がる(影が伸びる)。
 * 触った所も光る。エラー時は縁と注記が危険色。
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
                            elevation = if (focused) SoftGlassLight.elevation + 6.dp else SoftGlassLight.elevation,
                            interactionSource = interaction,
                            haptics = false,
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
                                    layer = GlassLayer.Inset,
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
 * 取り消せない操作の確認ダイアログ。半透明のすりガラスの板に、
 * [キャンセル(副)] [確定(危険色)] の2つのボタンを並べる。
 *
 * ダイアログは別の窓なので、画面の壁を透かし見ることはできない。
 * 代わりに Android 12 以上の「窓の背面ぼかし」で、後ろの画面全体を実際にぼかす
 * (端末の設定や省電力で背面ぼかしが切られているときは、ふつうの暗転になる)。
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
        BlurBehindDialogWindow()
        CompositionLocalProvider(LocalGlassScene provides null) {
            // 光とにじみがダイアログの窓の外で切れないよう、周りに余白を取る
            Box(modifier = Modifier.padding(24.dp)) {
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
}

/** ダイアログの窓の後ろにある画面全体を、OSの機能で実際にぼかす。 */
@Composable
private fun BlurBehindDialogWindow() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val radius = with(LocalDensity.current) { 24.dp.roundToPx() }
    SideEffect {
        if (window.windowManager.isCrossWindowBlurEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also { it.blurBehindRadius = radius }
        }
    }
}
