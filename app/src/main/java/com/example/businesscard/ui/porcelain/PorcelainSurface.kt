package com.example.businesscard.ui.porcelain

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.glass.GlassFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.graphics.Paint as NativePaint

/** 部品の面の色の種類。見本の「明るいボタン」「濃いボタン」「使えないボタン(薄いグレー)」。 */
enum class PorcelainTone { Light, Dark, Muted }

/**
 * 押されたボタンの沈み具合。0 = 浮いている、1 = 板に沈みきった。
 * 離した直後は少しだけ負(浮きすぎ)になってから 0 に戻る。
 */
@Stable
class PorcelainPress internal constructor() {
    internal val depth = Animatable(0f)
    val value: Float get() = depth.value
}

/**
 * [interactionSource] の押す・離すに合わせて [PorcelainPress] を動かし、触覚を返す。
 * 素早く叩いたときも「底に当たった」所までは見せてから戻す。
 */
@Composable
fun rememberPorcelainPress(interactionSource: InteractionSource, feedback: GlassFeedback?): PorcelainPress {
    val press = remember { PorcelainPress() }
    LaunchedEffect(interactionSource, feedback) {
        var pressIn: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    feedback?.pressDown()
                    pressIn = launch { press.depth.animateTo(1f, PorcelainMotion.pressSpring) }
                }
                is PressInteraction.Release -> {
                    feedback?.pressUp()
                    val sinking = pressIn
                    launch {
                        // 底に当たるまでは見せてから戻す
                        sinking?.join()
                        press.depth.animateTo(0f, PorcelainMotion.releaseSpring)
                    }
                }
                is PressInteraction.Cancel -> launch { press.depth.animateTo(0f, PorcelainMotion.releaseSpring) }
            }
        }
    }
    return press
}

/**
 * 陶器のような板を描く Modifier。
 *
 * 描く順(下から): 光る縁のにじみ → 左上の光 → 右下の柔らかい影 → 接地の影 → 面(上が明るいグラデーション)
 * → 押し込まれた分の内側の影 → 上の縁のつや。
 *
 * 影は Android の BlurMaskFilter(GPU 描画対応は Android 9 以上)で本当にぼかして描く。
 *
 * @param pressed 押し込まれた深さ(0..1)。描くときに読むので、動いても組み立て直しは起きない
 * @param sunken true なら常に彫り込まれた溝として描く(入力欄)
 * @param glow 縁が暖かく光る強さ 0..1(見本の Hover with glow。入力中の欄などに使う)
 */
@Composable
fun Modifier.porcelainSurface(
    shape: Shape,
    tone: PorcelainTone = PorcelainTone.Light,
    elevation: PorcelainElevation = PorcelainElevation.Control,
    pressed: PorcelainPress? = null,
    sunken: Boolean = false,
    glow: Float = 0f,
    glowColor: Color = PorcelainTheme.colors.focusGlow,
    pressScale: Boolean = true,
): Modifier {
    val c = PorcelainTheme.colors
    val glowNow by animateFloatAsState(glow, tween(PorcelainMotion.CHANGE_MILLIS), label = "glow")
    val (top, bottom) = when (tone) {
        PorcelainTone.Light -> c.lightTop to c.lightBottom
        PorcelainTone.Dark -> c.darkTop to c.darkBottom
        PorcelainTone.Muted -> c.mutedTop to c.mutedBottom
    }
    val dark = tone != PorcelainTone.Light

    val squash = if (pressed != null && pressScale) {
        Modifier.graphicsLayer {
            val s = 1f - (1f - PorcelainMotion.PRESS_SCALE) * pressed.value.coerceIn(0f, 1f)
            scaleX = s
            scaleY = s
        }
    } else {
        Modifier
    }

    return this.then(squash).drawWithCache {
        val geometry = SurfaceGeometry(shape, size, layoutDirection, this)
        val paints = SurfacePaints(this, elevation)
        val fill = Brush.verticalGradient(listOf(top, bottom), startY = 0f, endY = size.height)
        val rimWidth = 1.dp.toPx()
        val rimPath = Path().apply {
            addOutline(shape.createOutline(Size(size.width - rimWidth, size.height - rimWidth), layoutDirection, this@drawWithCache))
            translate(Offset(rimWidth / 2f, rimWidth / 2f))
        }
        onDrawBehind {
            val p = if (sunken) 1f else pressed?.value ?: 0f
            // 押し込むほど外の影は消える。離した直後の浮きすぎ(p < 0)では、影が少し伸びる
            val lift = (1f - p).coerceIn(0f, 1.15f)
            val sink = p.coerceIn(0f, 1f)

            if (glowNow > 0.001f) drawGlow(geometry, paints, glowColor, glowNow)
            if (lift > 0.001f) drawRaisedShadows(geometry, paints, elevation, lift, dark)
            drawPath(geometry.path, fill)
            if (sink > 0.001f) drawInset(geometry, paints, sink, dark)

            val rimTop = if (dark) 0.18f else 0.7f
            drawPath(
                rimPath,
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = rimTop * (1f - 0.7f * sink)),
                    0.55f to Color.Transparent,
                    startY = 0f,
                    endY = size.height,
                ),
                style = Stroke(rimWidth),
            )
        }
    }
}

/** 形から作る道のり(パス)。押し込みの内側の影には「外側すべて」を使う。 */
private class SurfaceGeometry(shape: Shape, size: Size, layoutDirection: LayoutDirection, density: Density) {
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, density)) }
    val native: android.graphics.Path = path.asAndroidPath()

    /** 部品の外側(大きな四角から部品の形をくり抜いたもの) */
    val outside = android.graphics.Path().apply {
        addRect(-size.width, -size.height, size.width * 2f, size.height * 2f, android.graphics.Path.Direction.CW)
        addPath(native)
        fillType = android.graphics.Path.FillType.EVEN_ODD
    }
}

/** ぼかしの付いた絵筆。ぼかしの半径は dp で決めて、ここでピクセルにする。 */
private class SurfacePaints(density: Density, e: PorcelainElevation) {
    val light = blurPaint(density, e.lightBlur)
    val drop = blurPaint(density, e.dropBlur)
    val contact = blurPaint(density, e.contactBlur)
    val inset = blurPaint(density, PorcelainInset.blur)
    val glowWide = blurPaint(density, 18.dp)
    val glowNear = blurPaint(density, 7.dp)
}

private fun blurPaint(density: Density, radius: Dp): NativePaint {
    val radiusPx = with(density) { radius.toPx() }
    return NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        if (radiusPx > 0.5f) maskFilter = BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
    }
}

private fun NativePaint.tint(color: Color, alpha: Float): NativePaint {
    this.color = color.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
    return this
}

private fun DrawScope.drawRaisedShadows(
    g: SurfaceGeometry,
    paints: SurfacePaints,
    e: PorcelainElevation,
    lift: Float,
    dark: Boolean,
) {
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        // 左上の光(濃い板では弱い)
        nc.save()
        nc.translate(e.lightDx.toPx() * lift, e.lightDy.toPx() * lift)
        nc.drawPath(g.native, paints.light.tint(Color.White, e.lightAlpha * lift * if (dark) 0.45f else 1f))
        nc.restore()
        // 右下の柔らかい影
        nc.save()
        nc.translate(e.dropDx.toPx() * lift, e.dropDy.toPx() * lift)
        nc.drawPath(g.native, paints.drop.tint(Color.Black, e.dropAlpha * lift))
        nc.restore()
        // 接地の影
        nc.save()
        nc.translate(0f, e.contactDy.toPx() * lift)
        nc.drawPath(g.native, paints.contact.tint(Color.Black, e.contactAlpha * lift.coerceAtMost(1f)))
        nc.restore()
    }
}

private fun DrawScope.drawInset(g: SurfaceGeometry, paints: SurfacePaints, sink: Float, dark: Boolean) {
    clipPath(g.path) {
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            val dx = PorcelainInset.dx.toPx()
            val dy = PorcelainInset.dy.toPx()
            // 上の縁が落とす内側の影
            nc.save()
            nc.translate(dx, dy)
            nc.drawPath(g.outside, paints.inset.tint(Color.Black, PorcelainInset.ALPHA * sink))
            nc.restore()
            // 下の縁に回り込む光
            nc.save()
            nc.translate(-dx, -dy)
            nc.drawPath(g.outside, paints.inset.tint(Color.White, PorcelainInset.LIGHT_ALPHA * sink * if (dark) 0.3f else 1f))
            nc.restore()
        }
    }
}

private fun DrawScope.drawGlow(g: SurfaceGeometry, paints: SurfacePaints, color: Color, amount: Float) {
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        nc.drawPath(g.native, paints.glowWide.tint(color, 0.75f * amount))
        nc.drawPath(g.native, paints.glowNear.tint(color, 0.9f * amount))
    }
}

/** 押した深さで、中身(文字・アイコン)をわずかに下げる(板と一緒に沈む)。 */
fun Modifier.sinkWith(press: PorcelainPress?): Modifier =
    if (press == null) this else this.graphicsLayer { translationY = 1.dp.toPx() * press.value.coerceIn(0f, 1f) }
