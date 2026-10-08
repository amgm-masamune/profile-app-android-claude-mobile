package com.example.businesscard.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.businesscard.R
import com.example.businesscard.ui.theme.SoftGlassLight
import com.example.businesscard.ui.theme.SoftGlassTheme
import com.example.businesscard.ui.theme.SoftGlassType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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

/** 光が当たる辺。ふつうは下端。選択中のタブだけ上端が光る。 */
enum class GlowEdge { Bottom, Top }

/**
 * すりガラスの部品を描く Modifier。見本の部品は、次の6層でできている。
 *
 * 1. 壁に落ちる影 … 右下にずれた柔らかい影(部品の外側だけに描き、ガラス越しに透けないようにする)
 * 2. 壁に漏れる光 … 光る辺の外側に広がる楕円の光
 * 3. ガラスの膜   … 白の薄い膜 + 上端のつや
 * 4. 内側の光     … 光る辺の内側にたまる光
 * 5. 縁           … 1dpの白い縁(上が明るい)
 * 6. 光る縁       … 光る辺の中央がいちばん白い線
 *
 * 背面を実際にぼかす(実ブラー)はしていない。壁がなめらかなグラデーションなので、
 * 半透明の膜だけでもすりガラスに見える。
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    fill: Color = SoftGlassTheme.colors.glass,
    glow: GlassGlow = GlassGlow.Medium,
    glowEdge: GlowEdge = GlowEdge.Bottom,
    castShadow: Boolean = true,
    border: Color = SoftGlassTheme.colors.glassBorder,
    borderWidth: Dp = 1.dp,
): Modifier {
    val c = SoftGlassTheme.colors
    return this.drawWithCache {
        val path = shape.createOutline(size, layoutDirection, this).toPath()
        val strength = glow.strength
        val shadowPaint = if (castShadow) {
            Paint().also { paint ->
                paint.asFrameworkPaint().apply {
                    isAntiAlias = true
                    // 塗りは透明にして、setShadowLayer の影だけを描く
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        SoftGlassLight.shadowBlur.toPx(),
                        SoftGlassLight.shadowOffsetX.toPx(),
                        SoftGlassLight.shadowOffsetY.toPx(),
                        c.castShadow.toArgb(),
                    )
                }
            }
        } else {
            null
        }

        val edgeY = if (glowEdge == GlowEdge.Bottom) size.height else 0f
        val glowCenter = Offset(size.width / 2f, edgeY)
        val outerRadiusX = size.width * 0.34f
        val outerRadiusY = min(size.height * 0.7f, 40.dp.toPx())
        val innerRadiusX = size.width * 0.38f
        val innerRadiusY = min(size.height * 0.5f, 26.dp.toPx())

        val sheen = Brush.verticalGradient(
            colors = listOf(c.glassSheen, Color.Transparent),
            startY = 0f,
            endY = min(size.height * 0.5f, 28.dp.toPx()),
        )
        val borderBrush = Brush.verticalGradient(
            0f to border,
            0.5f to border.copy(alpha = border.alpha * 0.55f),
            1f to border.copy(alpha = border.alpha * 0.8f),
        )
        val strokePx = borderWidth.toPx()
        val lineStart = size.width * 0.12f
        val lineEnd = size.width * 0.88f
        val lineY = if (glowEdge == GlowEdge.Bottom) size.height - strokePx else strokePx
        val edgeLine = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to c.glow.copy(alpha = 0.95f * strength),
            1f to Color.Transparent,
            startX = lineStart,
            endX = lineEnd,
        )

        onDrawBehind {
            // 1. 壁に落ちる影
            if (shadowPaint != null) {
                clipPath(path, clipOp = ClipOp.Difference) {
                    drawIntoCanvas { canvas -> canvas.drawPath(path, shadowPaint) }
                }
            }
            // 2. 壁に漏れる光
            if (strength > 0f) {
                clipPath(path, clipOp = ClipOp.Difference) {
                    drawGlow(glowCenter, outerRadiusX, outerRadiusY, c.glow.copy(alpha = 0.8f * strength))
                }
            }
            // 3. ガラスの膜とつや
            drawPath(path, color = fill)
            drawPath(path, brush = sheen)
            // 4. 内側の光
            if (strength > 0f) {
                clipPath(path) {
                    drawGlow(glowCenter, innerRadiusX, innerRadiusY, c.glow.copy(alpha = 0.55f * strength))
                }
            }
            // 5. 縁
            drawPath(path, brush = borderBrush, style = Stroke(width = strokePx))
            // 6. 光る縁
            if (strength > 0f) {
                drawLine(
                    brush = edgeLine,
                    start = Offset(lineStart, lineY),
                    end = Offset(lineEnd, lineY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/** 楕円の光。円の放射グラデーションを縦に伸縮して作る。 */
internal fun DrawScope.drawGlow(center: Offset, radiusX: Float, radiusY: Float, color: Color) {
    if (radiusX <= 0f || radiusY <= 0f || color.alpha <= 0f) return
    scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color,
                0.4f to color.copy(alpha = color.alpha * 0.5f),
                1f to Color.Transparent,
                center = center,
                radius = radiusX,
            ),
            radius = radiusX,
            center = center,
        )
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

/**
 * 画面の背景(壁)。グレージュの縦グラデーションに、
 * 左上から斜めに差し込む光の帯と、左下・下中央の陰を重ねる。
 */
@Composable
fun SoftGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = SoftGlassTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val base = Brush.verticalGradient(listOf(c.wallTop, c.wallBottom))

                // 光の帯: 左上(w*0.09, 0)から右下(w*0.64, h)へ向かう帯。帯に直交する向きにグラデーションをかける
                val beamCenter = Offset(w * 0.32f, h * 0.42f)
                val dirX = 0.55f * w
                val dirY = h
                val len = sqrt(dirX * dirX + dirY * dirY)
                val normal = Offset(dirY / len, -dirX / len)
                val half = min(w, h) * 0.45f
                val beam = Brush.linearGradient(
                    0f to Color.Transparent,
                    0.3f to Color.Transparent,
                    0.5f to c.wallLight.copy(alpha = 0.42f),
                    0.7f to Color.Transparent,
                    1f to Color.Transparent,
                    start = beamCenter - normal * half,
                    end = beamCenter + normal * half,
                )
                val shadeCorner = Brush.radialGradient(
                    0f to c.wallShade.copy(alpha = 0.7f),
                    1f to Color.Transparent,
                    center = Offset(0f, h),
                    radius = max(w, h) * 0.6f,
                )
                val shadeBottom = Brush.radialGradient(
                    0f to c.wallShade.copy(alpha = 0.35f),
                    1f to Color.Transparent,
                    center = Offset(w * 0.55f, h * 1.05f),
                    radius = min(w, h) * 0.7f,
                )
                onDrawBehind {
                    drawRect(base)
                    drawRect(shadeCorner)
                    drawRect(shadeBottom)
                    drawRect(beam)
                }
            },
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
