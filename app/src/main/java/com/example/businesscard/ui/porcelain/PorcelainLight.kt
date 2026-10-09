package com.example.businesscard.ui.porcelain

import android.graphics.RuntimeShader
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density

/**
 * 1画面ぶんの「板(パネル)と、その波の縁の裏に隠れた帯状の光源」。
 *
 * 板を描く [PorcelainBackground] が作って配り、板の上の部品(ボタン・名刺・入力欄)は、
 * 自分の位置をこれと比べて、光源からどれだけ光を受けるかを計算する(PorcelainShaders.SURFACE)。
 * ダイアログなど板の外の窓では null(光源が無い)。
 */
@Stable
class PorcelainLightField internal constructor(
    internal val wave: WavePlacement,
    private val glowState: () -> Float,
) {
    /** 板の左上の、画面(ルート)座標での位置 */
    internal var origin by mutableStateOf(Offset.Zero)

    /** 板の大きさ(ピクセル)。まだ置かれていなければ Size.Zero */
    internal var size by mutableStateOf(Size.Zero)

    /** 光源の灯り具合 0..1(画面を開くとゆっくり灯る) */
    internal val glow: Float get() = glowState()
}

/** いま描いている板の光源。板の外(ダイアログなど)では null。 */
internal val LocalPorcelainLight = staticCompositionLocalOf<PorcelainLightField?> { null }

/** 波の縁の位置(板の座標、ピクセル)。[top] がいちばん高い所、[amp] が高低差。 */
internal data class WaveGeometry(val top: Float, val amp: Float)

/** 板の大きさと置き方から、波の縁の位置を決める。高低差は板の短い辺に比例(見本の比率)。 */
internal fun waveGeometry(size: Size, wave: WavePlacement, density: Density): WaveGeometry {
    val amp = minOf(size.width, size.height) * PorcelainLight.WAVE_AMPLITUDE
    val lowest = with(density) {
        when (wave) {
            is WavePlacement.AboveBottom -> size.height - wave.lift.toPx()
            is WavePlacement.BelowTop -> wave.offset.toPx()
        }
    }
    return WaveGeometry(top = lowest - amp, amp = amp)
}

/**
 * 照明の模型(LIGHT_RIG)の uniform をまとめて設定する。板のシェーダーと部品のシェーダーで共通。
 * @param hdr HDR表示の余裕(白の何倍まで出せるか)。通常の画面は 1
 */
internal fun RuntimeShader.setLightRig(
    panelSize: Size,
    wave: WaveGeometry,
    density: Float,
    glow: Float,
    hdr: Float,
    colors: PorcelainColors,
) {
    setFloatUniform("panelSize", panelSize.width, panelSize.height)
    setFloatUniform("dp", density)
    setFloatUniform("waveTop", wave.top)
    setFloatUniform("waveAmp", wave.amp)
    setFloatUniform("glow", glow)
    setFloatUniform("hdr", hdr)
    setColorUniform("lightColor", colors.lightColor.toArgb())
}
