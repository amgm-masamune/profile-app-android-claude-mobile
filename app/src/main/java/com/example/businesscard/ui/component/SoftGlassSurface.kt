package com.example.businesscard.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
import com.example.businesscard.ui.glass.rememberGlassFeedback
import com.example.businesscard.ui.glass.rememberGlassPress
import com.example.businesscard.ui.glass.touchLight
import com.example.businesscard.ui.theme.SoftGlassMotion
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
 * [interactionSource] を渡すと、触ったときに物として反応する:
 * 押すと壁へ沈み(少し小さくなり、影が縮む)、触った所が光り、離すとばねで戻る。
 * [haptics] が true なら、押す・離すで短く振動する。
 *
 * 光の強さ・浮く高さ・色が変わるときは、パッと切り替えずに短く移り変わる。
 *
 * @param fill [GlassLayer.Floating] では背後の壁に混ぜる色(アルファが混ぜる量)、
 *   [GlassLayer.Inset] では重ねる膜の色
 * @param border 縁の色。左上ほど明るく照らされる
 * @param elevation 壁から浮いている高さ。高いほど影が遠く・柔らかくなる
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
    elevation: Dp = SoftGlassLight.elevation,
    interactionSource: InteractionSource? = null,
    haptics: Boolean = true,
): Modifier {
    val c = SoftGlassTheme.colors
    val emitTop = glowEdge == GlowEdge.Top
    val change = tween<Float>(SoftGlassMotion.GLOW_CHANGE_MILLIS)
    val emit by animateFloatAsState(glow.strength, change, label = "glow")
    val fillNow by animateColorAsState(fill, tween(SoftGlassMotion.GLOW_CHANGE_MILLIS), label = "fill")
    val borderNow by animateColorAsState(border, tween(SoftGlassMotion.GLOW_CHANGE_MILLIS), label = "border")
    val elevationNow by animateDpAsState(elevation, tween(SoftGlassMotion.GLOW_CHANGE_MILLIS), label = "elevation")

    val press = if (interactionSource != null) {
        rememberGlassPress(interactionSource, if (haptics) rememberGlassFeedback() else null)
    } else {
        null
    }
    val pressDepth = press?.depthValue ?: 0f
    val pressTravelPx = with(LocalDensity.current) { SoftGlassMotion.pressTravel.toPx() }
    val pressed = if (press != null) {
        Modifier.graphicsLayer {
            // 壁へ沈むぶん、遠ざかって少し小さく見える(大きな部品ほど縮む割合は小さい)
            val longest = maxOf(size.width, size.height, 1f)
            val s = (1f - pressTravelPx / longest * pressDepth).coerceAtLeast(SoftGlassMotion.MIN_PRESS_SCALE)
            scaleX = s
            scaleY = s
        }
    } else {
        Modifier
    }

    return this.then(pressed).then(
        when (layer) {
            GlassLayer.Floating -> Modifier.floatingGlass(
                shape = shape,
                tint = fillNow,
                emit = emit,
                emitTop = emitTop,
                rim = borderNow,
                rimWidth = borderWidth,
                lightColor = c.glow,
                elevation = elevationNow,
                frostBlur = SoftGlassLight.frostBlur,
                press = press,
            )
            GlassLayer.Inset -> Modifier.insetGlass(
                shape = shape,
                fill = fillNow,
                emit = emit,
                emitTop = emitTop,
                rim = borderNow,
                rimWidth = borderWidth,
                lightColor = c.glow,
                press = press,
            )
        },
    )
}

/**
 * ガラスの板は描かず、押した所だけを光らせる。タブ・選択肢の1つ1つに使う。
 */
@Composable
fun Modifier.glassTouchLight(interactionSource: InteractionSource, shape: Shape): Modifier {
    val press = rememberGlassPress(interactionSource, feedback = null)
    return this.touchLight(shape, SoftGlassTheme.colors.glow, press)
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
