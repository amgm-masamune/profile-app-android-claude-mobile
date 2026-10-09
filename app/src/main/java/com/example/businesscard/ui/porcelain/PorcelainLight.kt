package com.example.businesscard.ui.porcelain

import android.graphics.RuntimeShader
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density

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
 * 照明の模型(PorcelainShaders の LIGHT_RIG)の uniform をまとめて設定する。
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
