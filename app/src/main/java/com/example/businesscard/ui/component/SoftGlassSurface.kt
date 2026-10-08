package com.example.businesscard.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.businesscard.R
import com.example.businesscard.ui.glass.GlassSceneHost
import com.example.businesscard.ui.glass.floatingGlass
import com.example.businesscard.ui.glass.insetGlass
import com.example.businesscard.ui.theme.SoftGlassLight
import com.example.businesscard.ui.theme.SoftGlassTheme
import com.example.businesscard.ui.theme.SoftGlassType

/**
 * 光の強さ。見本では主ボタン(Launch / Premium plan)がいちばん強く光り、
 * 入力欄・副ボタンは中くらい、ドロップダウンやタブの外枠は弱い。
 */
enum class GlassGlow(internal val strength: Float) {
    None(0f),
    Soft(0.4f),
    Medium(0.7f),
    Strong(1f),
}

/** 光源のある辺。ふつうは下端。選択中のタブだけ上端が光る。 */
enum class GlowEdge { Bottom, Top }

/** ガラスの置き方。 */
enum class GlassLayer {
    /** 壁から浮いた板。背後の壁を実際にぼかして透かし、壁に影を落とし、光源で壁を照らす */
    Floating,

    /** ほかのガラスの上に重ねたタイル(アイコンの四角・選択中のタブ)。半透明の白い膜として重ねる */
    Inset,
}

/**
 * すりガラスの部品を描く Modifier。描き方の中身は `ui/glass/`(AGSLシェーダーと RenderEffect)。
 *
 * @param fill [GlassLayer.Floating] では背後の壁に混ぜる色(アルファが混ぜる量)、
 *   [GlassLayer.Inset] では重ねる膜の色
 * @param border 縁の色。左上ほど明るく照らされる
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    fill: Color = SoftGlassTheme.colors.glass,
    glow: GlassGlow = GlassGlow.Medium,
    glowEdge: GlowEdge = GlowEdge.Bottom,
    layer: GlassLayer = GlassLayer.Floating,
    border: Color = SoftGlassTheme.colors.glassBorder,
    borderWidth: Dp = 1.dp,
): Modifier {
    val c = SoftGlassTheme.colors
    val emitTop = glowEdge == GlowEdge.Top
    return when (layer) {
        GlassLayer.Floating -> this.floatingGlass(
            shape = shape,
            tint = fill,
            emit = glow.strength,
            emitTop = emitTop,
            rim = border,
            rimWidth = borderWidth,
            lightColor = c.glow,
            elevation = SoftGlassLight.elevation,
            frostBlur = SoftGlassLight.frostBlur,
        )
        GlassLayer.Inset -> this.insetGlass(
            shape = shape,
            fill = fill,
            emit = glow.strength,
            emitTop = emitTop,
            rim = border,
            rimWidth = borderWidth,
            lightColor = c.glow,
        )
    }
}

/**
 * 画面の背景(壁)。部屋の照明(環境光・左上のキーライト・斜めの日差し)を GPU で計算して描く。
 * 中に置いたガラスの部品の影と、部品の光源が壁を照らす光も、ここで一緒に計算される。
 */
@Composable
fun SoftGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = SoftGlassTheme.colors
    GlassSceneHost(
        modifier = modifier.fillMaxSize(),
        wallTop = c.wallTop,
        wallBottom = c.wallBottom,
        lightColor = c.glow,
        content = content,
    )
}

/**
 * 全画面共通の骨格。背景(壁)は画面ごとに不透明で描く
 * (画面遷移のクロスフェード中に、半透明の2画面が重なって見えないようにするため)。
 */
@Composable
fun SoftGlassScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    SoftGlassBackground(modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = SoftGlassTheme.colors.ink,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
        )
    }
}

/** 画面上部。戻る操作は左上の四角いアイコンボタン、タイトルはその右。 */
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (onBack != null) {
            GlassIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Text(
            text = title,
            style = SoftGlassType.title,
            color = SoftGlassTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
    }
}
