package com.example.businesscard.ui.glass

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.SystemClock
import android.view.Display
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.example.businesscard.ui.theme.SoftGlassMotion
import java.util.function.Consumer

/** 壁の照明シェーダーが一度に扱える部品の数(シェーダーの配列の大きさと同じ)。 */
internal const val MAX_ELEMENTS = 16

/** 壁から浮いている部品1つぶんの情報。壁の照明(影・光源)の計算に使う。 */
@Immutable
internal data class GlassElement(
    /** 画面(ルート)座標での見えている範囲。スクロールで隠れた部分は含まない */
    val bounds: Rect,
    val cornerRadius: Float,
    /** 壁からの高さ(dp)。影のずれ・ぼけ、光の広がりがここから決まる */
    val elevationDp: Float,
    /** 光源の強さ 0..1 */
    val emit: Float,
    /** 光源が上端にあるか(ふつうは下端) */
    val emitTop: Boolean,
)

/**
 * 1画面ぶんの「壁と、そこに浮かぶガラス」の場。
 *
 * - 壁はこの場の持ち主が [backdrop] に描く(壁の照明は登録された部品の影・光を含む)
 * - ガラスの部品は自分の位置を [elements] に登録し、[backdrop] を実際にぼかして透かし見る
 */
@Stable
class GlassScene internal constructor() {
    /** 壁の左上の、画面(ルート)座標での位置 */
    internal var origin by mutableStateOf(Offset.Zero)
    internal var size by mutableStateOf(Size.Zero)
    internal val elements = mutableStateMapOf<Any, GlassElement>()
    internal var backdrop: GraphicsLayer? = null
    private val createdAt = SystemClock.uptimeMillis()

    /** 画面ができた直後か(このとき現れた部品だけ、浮き上がる登場の動きをする) */
    internal fun isEntering(): Boolean =
        SystemClock.uptimeMillis() - createdAt < SoftGlassMotion.ENTRANCE_WINDOW_MILLIS
}

/** いま描いている壁。ダイアログなど別の窓の中では null(背後の壁を透かせない)。 */
val LocalGlassScene = staticCompositionLocalOf<GlassScene?> { null }

/**
 * HDR表示の余裕(白の何倍まで明るく光らせられるか)。1 = 通常(SDR)の画面。
 * 光源の芯をこの倍率で明るくする。
 */
val LocalGlowHeadroom = compositionLocalOf { 1f }

/** 光源の芯を、白の最大何倍まで明るくするか。 */
private const val MAX_GLOW_HEADROOM = 3f

/**
 * HDRに対応した画面(Android 14 以上)なら、窓をHDRモードにして、使える明るさの余裕を返す。
 * 対応していなければ 1(白より明るくはしない)。
 */
@Composable
fun rememberGlowHeadroom(activity: Activity): Float {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return 1f
    return rememberHdrHeadroom(activity)
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun rememberHdrHeadroom(activity: Activity): Float {
    val display = activity.display ?: return 1f
    if (!display.isHdrSdrRatioAvailable) return 1f
    var ratio by remember(display) { mutableFloatStateOf(display.hdrSdrRatio) }
    DisposableEffect(activity, display) {
        activity.window.colorMode = ActivityInfo.COLOR_MODE_HDR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            activity.window.setDesiredHdrHeadroom(MAX_GLOW_HEADROOM)
        }
        val listener = Consumer<Display> { ratio = it.hdrSdrRatio }
        display.registerHdrSdrRatioChangedListener(activity.mainExecutor, listener)
        onDispose { display.unregisterHdrSdrRatioChangedListener(listener) }
    }
    return ratio.coerceIn(1f, MAX_GLOW_HEADROOM)
}
