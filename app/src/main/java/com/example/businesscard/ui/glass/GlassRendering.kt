package com.example.businesscard.ui.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

/** 壁を画面の外まで少し広く描く幅(端の部品がぼかし・屈折で壁の外をのぞいても途切れないように)。 */
private val WallMargin = 64.dp

/** ガラスがぼかし・屈折のために読む、部品の外側の幅。 */
private val GlassMargin = 40.dp

/** 光のにじみ(グレア)を描く、部品の外側の幅。 */
private val GlareMargin = 48.dp

/**
 * 壁。部屋の照明を計算するシェーダーで描き、その絵を [GlassScene.backdrop] に記録して、
 * 中に置いたガラスの部品が透かし見られるようにする。
 */
@Composable
internal fun GlassSceneHost(
    modifier: Modifier,
    wallTop: Color,
    wallBottom: Color,
    lightColor: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    val scene = remember { GlassScene() }
    val backdrop = rememberGraphicsLayer()
    scene.backdrop = backdrop
    val shader = remember { RuntimeShader(GlassShaders.WALL) }
    val rects = remember { FloatArray(MAX_ELEMENTS * 4) }
    val props = remember { FloatArray(MAX_ELEMENTS * 4) }

    CompositionLocalProvider(LocalGlassScene provides scene) {
        Box(
            modifier = modifier
                .onGloballyPositioned { scene.origin = it.positionInRoot() }
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val origin = scene.origin
                    rects.fill(0f)
                    props.fill(0f)
                    var n = 0
                    for (element in scene.elements.values) {
                        if (n == MAX_ELEMENTS) break
                        val b = element.bounds
                        if (b.width < 1f || b.height < 1f) continue
                        rects[n * 4] = b.left - origin.x
                        rects[n * 4 + 1] = b.top - origin.y
                        rects[n * 4 + 2] = b.right - origin.x
                        rects[n * 4 + 3] = b.bottom - origin.y
                        props[n * 4] = element.cornerRadius
                        props[n * 4 + 1] = element.elevationDp
                        props[n * 4 + 2] = element.emit
                        props[n * 4 + 3] = if (element.emitTop) 1f else 0f
                        n++
                    }
                    shader.setFloatUniform("size", w, h)
                    shader.setFloatUniform("dp", density)
                    shader.setIntUniform("count", n)
                    shader.setFloatUniform("rects", rects)
                    shader.setFloatUniform("props", props)
                    shader.setColorUniform("albedoTop", wallTop.toArgb())
                    shader.setColorUniform("albedoBottom", wallBottom.toArgb())
                    shader.setColorUniform("lightColor", lightColor.toArgb())

                    // 壁は部品が動いたときだけ描き直せばよいので、画面外の絵(テクスチャ)として持っておく
                    val m = WallMargin.roundToPx()
                    backdrop.compositingStrategy = CompositingStrategy.Offscreen
                    backdrop.topLeft = IntOffset(-m, -m)
                    backdrop.record(IntSize(w.roundToInt() + 2 * m, h.roundToInt() + 2 * m)) {
                        translate(m.toFloat(), m.toFloat()) {
                            drawRect(
                                brush = ShaderBrush(shader),
                                topLeft = Offset(-m.toFloat(), -m.toFloat()),
                                size = Size(w + 2 * m, h + 2 * m),
                            )
                        }
                    }
                    drawLayer(backdrop)
                },
            content = content,
        )
    }
}

/**
 * 壁から浮いているすりガラスの板。
 *
 * 1. 自分の位置を場に登録する(壁がこの板の影と、この板の光源が照らす光を描く)
 * 2. 壁の絵の、自分の後ろにあたる部分を GraphicsLayer に描き、
 *    RenderEffect で「実際にぼかす → ガラスのシェーダーで屈折・陰影・つやを付ける」
 * 3. 光源の光(内部の散乱・縁の導光・芯・グレア)を加算合成で重ねる
 *
 * 壁の無い場所(ダイアログの窓の中など)では、[insetGlass] と同じ描き方になる。
 */
@Composable
internal fun Modifier.floatingGlass(
    shape: Shape,
    tint: Color,
    emit: Float,
    emitTop: Boolean,
    rim: Color,
    rimWidth: Dp,
    lightColor: Color,
    elevation: Dp,
    frostBlur: Dp,
): Modifier {
    val scene = LocalGlassScene.current
        ?: return insetGlass(shape, tint, emit, emitTop, rim, rimWidth, lightColor)
    val hdr = LocalGlowHeadroom.current
    val localDensity = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val key = remember { Any() }
    val layer = rememberGraphicsLayer()
    val glassShader = remember { RuntimeShader(GlassShaders.GLASS) }
    val lightShader = remember { RuntimeShader(GlassShaders.LIGHT) }
    var positionInRoot by remember { mutableStateOf<Offset?>(null) }
    val currentEmit by rememberUpdatedState(emit)
    val currentEmitTop by rememberUpdatedState(emitTop)

    DisposableEffect(scene, key) {
        onDispose { scene.elements.remove(key) }
    }
    // 押す・フォーカスで光の強さが変わったら、壁の照明にも伝える
    SideEffect {
        val current = scene.elements[key]
        if (current != null && (current.emit != emit || current.emitTop != emitTop)) {
            scene.elements[key] = current.copy(emit = emit, emitTop = emitTop)
        }
    }

    return this
        .onGloballyPositioned { coordinates ->
            positionInRoot = coordinates.positionInRoot()
            val element = GlassElement(
                bounds = coordinates.boundsInRoot(),
                cornerRadius = shape.cornerRadius(coordinates.size.toSize(), layoutDirection, localDensity),
                elevationDp = elevation.value,
                emit = currentEmit,
                emitTop = currentEmitTop,
            )
            if (scene.elements[key] != element) scene.elements[key] = element
        }
        .drawBehind {
            val backdrop = scene.backdrop ?: return@drawBehind
            val position = positionInRoot ?: return@drawBehind
            val offset = position - scene.origin
            val m = GlassMargin.roundToPx()
            val w = size.width
            val h = size.height
            val radius = shape.cornerRadius(size, layoutDirection, this)

            // 自分の後ろの壁(と周り)を写し取る
            layer.record(IntSize(w.roundToInt() + 2 * m, h.roundToInt() + 2 * m)) {
                translate(m - offset.x, m - offset.y) { drawLayer(backdrop) }
            }
            glassShader.setFloatUniform("size", w, h)
            glassShader.setFloatUniform("margin", m.toFloat())
            glassShader.setFloatUniform("radius", radius)
            glassShader.setFloatUniform("dp", density)
            glassShader.setFloatUniform("rimWidth", rimWidth.toPx())
            glassShader.setColorUniform("tint", tint.toArgb())
            glassShader.setColorUniform("rimColor", rim.toArgb())
            val blur = frostBlur.toPx()
            // 先にぼかし(すりガラスの拡散)、その結果をガラスのシェーダーに渡す
            layer.renderEffect = RenderEffect.createChainEffect(
                RenderEffect.createRuntimeShaderEffect(glassShader, "backdrop"),
                RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP),
            ).asComposeRenderEffect()
            translate(-m.toFloat(), -m.toFloat()) { drawLayer(layer) }

            if (emit > 0f) drawEmission(lightShader, radius, emit, emitTop, hdr, lightColor)
        }
}

/**
 * ほかのガラスの上に重ねるタイル(アイコンの四角・選択中のタブ)。
 * 背後はすでにすりガラスなので透かし直さず、白い膜・縁の陰影・つや・光源の光を重ねる。
 */
@Composable
internal fun Modifier.insetGlass(
    shape: Shape,
    fill: Color,
    emit: Float,
    emitTop: Boolean,
    rim: Color,
    rimWidth: Dp,
    lightColor: Color,
): Modifier {
    val hdr = LocalGlowHeadroom.current
    val tileShader = remember { RuntimeShader(GlassShaders.TILE) }
    val lightShader = remember { RuntimeShader(GlassShaders.LIGHT) }
    return drawBehind {
        val radius = shape.cornerRadius(size, layoutDirection, this)
        tileShader.setFloatUniform("size", size.width, size.height)
        tileShader.setFloatUniform("radius", radius)
        tileShader.setFloatUniform("dp", density)
        tileShader.setFloatUniform("rimWidth", rimWidth.toPx())
        tileShader.setColorUniform("fill", fill.toArgb())
        tileShader.setColorUniform("rimColor", rim.toArgb())
        drawRect(ShaderBrush(tileShader))
        if (emit > 0f) drawEmission(lightShader, radius, emit, emitTop, hdr, lightColor)
    }
}

/** 光る白い玉(トグルのつまみ)。球として陰影を付け、まわりに光をにじませる。 */
@Composable
internal fun Modifier.glowingBall(top: Color, bottom: Color, lightColor: Color): Modifier {
    val hdr = LocalGlowHeadroom.current
    val body = remember { RuntimeShader(GlassShaders.BALL) }
    val halo = remember { RuntimeShader(GlassShaders.BALL) }
    return drawBehind {
        fun RuntimeShader.setUp(mode: Float) {
            setFloatUniform("size", size.width, size.height)
            setFloatUniform("dp", density)
            setFloatUniform("mode", mode)
            setFloatUniform("hdr", hdr)
            setColorUniform("topColor", top.toArgb())
            setColorUniform("bottomColor", bottom.toArgb())
            setColorUniform("lightColor", lightColor.toArgb())
        }
        body.setUp(0f)
        drawRect(ShaderBrush(body))
        halo.setUp(1f)
        val g = GlareMargin.toPx() / 2f
        drawRect(
            brush = ShaderBrush(halo),
            topLeft = Offset(-g, -g),
            size = Size(size.width + 2 * g, size.height + 2 * g),
            blendMode = BlendMode.Plus,
        )
    }
}

/** 光源の光を、部品の周りまで含めて加算合成で描く。 */
private fun DrawScope.drawEmission(
    shader: RuntimeShader,
    radius: Float,
    emit: Float,
    emitTop: Boolean,
    hdr: Float,
    lightColor: Color,
) {
    val g = GlareMargin.toPx()
    shader.setFloatUniform("size", size.width, size.height)
    shader.setFloatUniform("radius", radius)
    shader.setFloatUniform("dp", density)
    shader.setFloatUniform("emit", emit)
    shader.setFloatUniform("emitTop", if (emitTop) 1f else 0f)
    shader.setFloatUniform("hdr", hdr)
    shader.setColorUniform("lightColor", lightColor.toArgb())
    drawRect(
        brush = ShaderBrush(shader),
        topLeft = Offset(-g, -g),
        size = Size(size.width + 2 * g, size.height + 2 * g),
        blendMode = BlendMode.Plus,
    )
}

/** 角丸の半径(ピクセル)。角丸長方形でない形は 0。 */
private fun Shape.cornerRadius(size: Size, layoutDirection: LayoutDirection, density: Density): Float =
    when (val outline = createOutline(size, layoutDirection, density)) {
        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
        else -> 0f
    }
