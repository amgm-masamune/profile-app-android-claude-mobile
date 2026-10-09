package com.example.businesscard.ui.porcelain

import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.businesscard.ui.glass.GlassFeedback
import com.example.businesscard.ui.glass.LocalGlowHeadroom
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
 * 描く順(下から): 光る縁のにじみ → 左上の光 → 右下の柔らかい影 → 接地の影 → 面 → 押し込まれた分の内側の影
 * → 上の縁のつや。
 *
 * 面は AGSL シェーダー(PorcelainShaders.SURFACE)で陰影を計算して描く。縁の丸み(ベベル)の向きで部屋の光
 * (左上)の当たり方が変わり、板の光源([LocalPorcelainLight])のほうを向いた縁は暖かく照らされる。
 * 押し込むほど丸みは平らになる(内側の影に任せる)。
 *
 * 影は Android の BlurMaskFilter(GPU 描画対応は Android 9 以上)で本当にぼかして描く。
 *
 * @param pressed 押し込まれた深さ(0..1)。描くときに読むので、動いても組み立て直しは起きない
 * @param sunken true なら常に彫り込まれた溝として描く(入力欄)
 * @param glow 縁が暖かく光る強さ 0..1(見本の Hover with glow。入力中の欄などに使う)
 * @param bevel 縁の丸みの幅。大きな板(名刺・ダイアログ)は [PorcelainRelief.cardBevel]
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
    bevel: Dp = PorcelainRelief.controlBevel,
): Modifier {
    val c = PorcelainTheme.colors
    val field = LocalPorcelainLight.current
    val hdr = LocalGlowHeadroom.current
    val shader = remember { RuntimeShader(PorcelainShaders.SURFACE) }
    // 自分の左上の、画面(ルート)座標での位置。板の光源からの光の計算に使う
    var position by remember { mutableStateOf(Offset.Unspecified) }
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

    val locate = Modifier.onGloballyPositioned { position = it.positionInRoot() }

    return this.then(locate).then(squash).drawWithCache {
        val geometry = SurfaceGeometry(shape, size, layoutDirection, this)
        val paints = SurfacePaints(this, elevation)
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("radius", geometry.cornerRadius)
        shader.setFloatUniform("sheen", if (dark) PorcelainRelief.DARK_SHEEN else PorcelainRelief.LIGHT_SHEEN)
        shader.setColorUniform("topColor", top.toArgb())
        shader.setColorUniform("bottomColor", bottom.toArgb())
        val fill = ShaderBrush(shader)
        val bevelPx = bevel.toPx()
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
            // 面: 押し込むほど縁の丸みは平らになる(へこみは内側の影で見せる)
            shader.setFloatUniform("bevel", bevelPx * (1f - sink))
            shader.setLight(field, position, hdr, c, density)
            drawRect(fill)
            if (sink > 0.001f) drawInset(geometry, paints, sink, dark)

            val rimTop = if (dark) 0.12f else 0.45f
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
    private val outline = shape.createOutline(size, layoutDirection, density)
    val path = Path().apply { addOutline(outline) }
    val native: android.graphics.Path = path.asAndroidPath()

    /** 角丸の半径(ピクセル)。丸い形(CircleShape・pill)は短い辺の半分、角丸でない形は 0 */
    val cornerRadius: Float = when (outline) {
        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
        is Outline.Rectangle -> 0f
        is Outline.Generic -> minOf(size.width, size.height) / 2f
    }

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

/**
 * 部品のシェーダーに、板の光源を設定する。板の外(ダイアログなど)や、まだ位置が分からないときは光を受けない。
 */
private fun RuntimeShader.setLight(
    field: PorcelainLightField?,
    position: Offset,
    hdr: Float,
    colors: PorcelainColors,
    density: Float,
) {
    val panel = field?.size
    if (field != null && panel != null && panel.width > 0f && position.isSpecified) {
        val wave = waveGeometry(panel, field.wave, Density(density))
        setLightRig(panel, wave, density, field.glow, hdr, colors)
        val origin = position - field.origin
        setFloatUniform("origin", origin.x, origin.y)
        setFloatUniform("receive", 1f)
    } else {
        setLightRig(Size(1f, 1f), WaveGeometry(0f, 0f), density, 0f, hdr, colors)
        setFloatUniform("origin", 0f, 0f)
        setFloatUniform("receive", 0f)
    }
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
