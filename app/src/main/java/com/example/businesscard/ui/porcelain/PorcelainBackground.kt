package com.example.businesscard.ui.porcelain

import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.businesscard.ui.glass.LocalGlowHeadroom
import com.example.businesscard.ui.glass.prefersReducedMotion
import android.graphics.Paint as NativePaint

/**
 * 波の縁を置く高さ。板(パネル)の下端から測るか、上端から測るかを選ぶ。
 * どちらも「縁のいちばん低い所(左端)」の位置。
 */
sealed interface WavePlacement {
    /** 板の下端からの高さ。下に操作ボタンがある画面では、ボタンのすぐ上に縁が来るようにする */
    data class AboveBottom(val lift: Dp) : WavePlacement

    /** 板の上端からの距離(カタログなど、上から並べる画面用) */
    data class BelowTop(val offset: Dp) : WavePlacement
}

/**
 * 画面の背景。グレーの壁の前に明るい板(パネル)が浮き、右下へ長い影を落とす。
 * 板の中ほどを波打つ薄い板が横切り、その縁の裏に隠れた帯状の光源が、奥の面を照らす(光の量を AGSL で計算して描く)。
 *
 * 板の上の部品は [LocalPorcelainLight] でこの光源を知り、光源のほうを向いた縁が照らされる。
 * 板はシステムバーとキーボードを避けて置く(中の部品は改めて避けなくてよい)。
 * 画面を開くと、光源がゆっくり灯る(「アニメーションを削除」設定では最初から灯っている)。
 * HDR 対応の画面では、光の芯が白より明るく光る(明るさの余裕は [LocalGlowHeadroom])。
 */
@Composable
fun PorcelainBackground(
    modifier: Modifier = Modifier,
    wave: WavePlacement = WavePlacement.AboveBottom(PorcelainLight.waveLift),
    content: @Composable BoxScope.() -> Unit,
) {
    val c = PorcelainTheme.colors
    val context = LocalContext.current
    val glow = remember { Animatable(if (context.prefersReducedMotion()) 1f else 0f) }
    LaunchedEffect(Unit) {
        glow.animateTo(1f, tween(PorcelainMotion.GLOW_IN_MILLIS, easing = FastOutSlowInEasing))
    }
    val field = remember(wave) { PorcelainLightField(wave) { glow.value } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(c.wallLight, c.wallDark), start = Offset.Zero, end = Offset.Infinite)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    start = PorcelainSpacing.panelStart,
                    top = PorcelainSpacing.panelTop,
                    end = PorcelainSpacing.panelEnd,
                    bottom = PorcelainSpacing.panelBottom,
                )
                .onGloballyPositioned {
                    field.origin = it.positionInRoot()
                    field.size = it.size.toSize()
                }
                .porcelainPanel(field),
        ) {
            CompositionLocalProvider(LocalPorcelainLight provides field) {
                content()
            }
        }
    }
}

/**
 * 全画面共通の骨格。上下のバーと中身を、板(パネル)の中に置く。
 * システムバーは板がすでに避けているので、Scaffold には余白を足させない。
 */
@Composable
fun PorcelainScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    PorcelainBackground(modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = PorcelainTheme.colors.ink,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
        )
    }
}

/** 板(パネル)を描く: 壁へ落ちる影 → 面(光の計算をするシェーダー) → 縁のつや。中身は板の形で切り抜かない(影が切れないように)。 */
@Composable
private fun Modifier.porcelainPanel(field: PorcelainLightField): Modifier {
    val c = PorcelainTheme.colors
    val hdr = LocalGlowHeadroom.current
    val shader = remember { RuntimeShader(PorcelainShaders.PANEL) }
    val e = PorcelainElevation.Panel
    return this.drawWithCache {
        val shape = PorcelainShapes.panel
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val native = path.asAndroidPath()
        val drop = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(e.dropBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
            color = Color.Black.copy(alpha = e.dropAlpha).toArgb()
        }
        val contact = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(e.contactBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
            color = Color.Black.copy(alpha = e.contactAlpha).toArgb()
        }
        val light = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(e.lightBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
            color = Color.White.copy(alpha = e.lightAlpha).toArgb()
        }

        val wave = waveGeometry(size, field.wave, this)
        shader.setColorUniform("panelTop", c.panelTop.toArgb())
        shader.setColorUniform("panelBottom", c.panelBottom.toArgb())
        shader.setColorUniform("sheetFilter", c.sheetFilter.toArgb())
        val brush = ShaderBrush(shader)
        val rimWidth = 1.5.dp.toPx()
        val rim = Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.6f),
            0.5f to Color.White.copy(alpha = 0f),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )

        onDrawBehind {
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                nc.save(); nc.translate(e.lightDx.toPx(), e.lightDy.toPx()); nc.drawPath(native, light); nc.restore()
                nc.save(); nc.translate(e.dropDx.toPx(), e.dropDy.toPx()); nc.drawPath(native, drop); nc.restore()
                nc.save(); nc.translate(0f, e.contactDy.toPx()); nc.drawPath(native, contact); nc.restore()
            }
            shader.setLightRig(size, wave, density, field.glow, hdr, c)
            drawPath(path, brush)
            drawPath(path, rim, style = Stroke(rimWidth))
        }
    }
}
