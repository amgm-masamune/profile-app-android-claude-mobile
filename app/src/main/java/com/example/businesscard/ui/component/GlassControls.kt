package com.example.businesscard.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.theme.SoftGlassElevation
import com.example.businesscard.ui.theme.SoftGlassShapes
import com.example.businesscard.ui.theme.SoftGlassTheme

/** ボタンの種類。1画面に Primary は1つまで(いちばん大事な操作だけ光らせる)。 */
enum class GlassButtonStyle { Primary, Secondary, Danger }

/** pill形のボタン。Primary はオレンジの発光、Secondary/Danger はガラス面。 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Primary,
    icon: ImageVector? = null,
) {
    val c = SoftGlassTheme.colors
    val shape = SoftGlassShapes.pill
    val contentColor = when (style) {
        GlassButtonStyle.Primary -> c.onAccent
        GlassButtonStyle.Secondary -> c.ink
        GlassButtonStyle.Danger -> c.danger
    }
    val surface = if (style == GlassButtonStyle.Primary) {
        Modifier
            .shadow(
                elevation = SoftGlassElevation.floating,
                shape = shape,
                ambientColor = c.accentWarm.copy(alpha = 0.5f),
                spotColor = c.accentWarm.copy(alpha = 0.9f),
            )
            .background(Brush.verticalGradient(listOf(c.accentWarmSoft, c.accentWarm)), shape)
    } else {
        Modifier.glassSurface(shape)
    }

    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .then(surface)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 丸いアイコンボタン(48dp)。`primary = true` のときだけオレンジに発光する。 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    tint: Color = SoftGlassTheme.colors.ink,
) {
    val c = SoftGlassTheme.colors
    val surface = if (primary) {
        Modifier
            .shadow(
                elevation = SoftGlassElevation.floating,
                shape = CircleShape,
                ambientColor = c.accentWarm.copy(alpha = 0.5f),
                spotColor = c.accentWarm.copy(alpha = 0.9f),
            )
            .background(Brush.verticalGradient(listOf(c.accentWarmSoft, c.accentWarm)), CircleShape)
    } else {
        Modifier.glassSurface(CircleShape)
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .then(surface)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (primary) c.onAccent else tint,
        )
    }
}

/** ガラス面の入力欄。フォーカス中は縁がオレンジになり、面が少し濃くなる。 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
) {
    val c = SoftGlassTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = if (errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        singleLine = true,
        shape = SoftGlassShapes.field,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.ink,
            unfocusedTextColor = c.ink,
            errorTextColor = c.ink,
            focusedContainerColor = c.glassStrong,
            unfocusedContainerColor = c.glass,
            errorContainerColor = c.glassStrong,
            cursorColor = c.focusRing,
            focusedBorderColor = c.focusRing,
            unfocusedBorderColor = c.glassBorder,
            errorBorderColor = c.danger,
            focusedLabelColor = c.ink,
            unfocusedLabelColor = c.inkMuted,
            errorLabelColor = c.danger,
            errorSupportingTextColor = c.danger,
        ),
        modifier = modifier,
    )
}

/** 取り消せない操作の確認ダイアログ。確定側は危険色、キャンセル側は通常色。 */
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
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SoftGlassShapes.card,
        containerColor = c.dialog,
        titleContentColor = c.ink,
        textContentColor = c.inkMuted,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = c.danger)) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = c.ink)) {
                Text(dismissLabel)
            }
        },
    )
}
