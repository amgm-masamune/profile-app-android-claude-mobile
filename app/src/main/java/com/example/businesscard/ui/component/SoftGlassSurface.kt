package com.example.businesscard.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.businesscard.R
import com.example.businesscard.ui.theme.SoftGlassElevation
import com.example.businesscard.ui.theme.SoftGlassTheme

/**
 * ガラス面。半透明の塗り + 上から下へ薄れる白い縁 + 青みのある柔らかい影。
 * 実ブラー(背面のぼかし)は使わず、背景の発光ブロブが透けることでガラスらしさを出す。
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    elevation: Dp = SoftGlassElevation.raised,
    fill: Color = SoftGlassTheme.colors.glass,
): Modifier {
    val c = SoftGlassTheme.colors
    return this
        .shadow(elevation = elevation, shape = shape, ambientColor = c.shadow, spotColor = c.shadow)
        .background(
            brush = Brush.verticalGradient(listOf(fill, fill.copy(alpha = fill.alpha * 0.78f))),
            shape = shape,
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(c.glassBorder, c.glassBorder.copy(alpha = c.glassBorder.alpha * 0.35f)),
            ),
            shape = shape,
        )
}

/** 画面の背景。淡いグラデーションに、暖色と寒色の発光ブロブを重ねる。 */
@Composable
fun SoftGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = SoftGlassTheme.colors
    val warmAlpha = if (c.isDark) 0.32f else 0.50f
    val coolAlpha = if (c.isDark) 0.30f else 0.45f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.backgroundTop, c.backgroundBottom)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(c.blobWarm.copy(alpha = warmAlpha), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.06f),
                        radius = size.maxDimension * 0.55f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(c.blobCool.copy(alpha = coolAlpha), Color.Transparent),
                        center = Offset(size.width * 0.04f, size.height * 0.96f),
                        radius = size.maxDimension * 0.6f,
                    ),
                )
            },
        content = content,
    )
}

/**
 * 全画面共通の骨格。背景は画面ごとに不透明で描く
 * (画面遷移のクロスフェード中に、透明な2画面が重なって見えないようにするため)。
 */
@Composable
fun SoftGlassScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    content: @Composable (PaddingValues) -> Unit,
) {
    SoftGlassBackground(modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = SoftGlassTheme.colors.ink,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = content,
        )
    }
}

/** 画面上部。戻る操作は左上の丸ボタン、タイトルはその右。 */
@Composable
fun SoftGlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            GlassIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = SoftGlassTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
    }
}
