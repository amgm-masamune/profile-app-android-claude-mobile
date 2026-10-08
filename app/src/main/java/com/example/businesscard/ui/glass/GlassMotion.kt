package com.example.businesscard.ui.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.businesscard.ui.theme.SoftGlassMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 触ったときの体の感覚(振動・音)。端末の「タッチ時の振動」「タッチ操作音」の設定に従う。
 * 振動は Android の決まった種類から選ぶので、端末ごとに最適な振動になる。
 */
@Stable
class GlassFeedback internal constructor(private val view: View) {
    /** 押し込んだ瞬間(キーを押した感触) */
    fun pressDown() = haptic(HapticFeedbackConstants.VIRTUAL_KEY)

    /** 指を離した瞬間(キーが戻る感触。押し込みより軽い) */
    fun pressUp() = haptic(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)

    /** ボタンが確定したときのクリック音 */
    fun click() = view.playSoundEffect(SoundEffectConstants.CLICK)

    /** トグルのオン・オフ。Android 14 以上はオンとオフで感触が違う */
    fun toggle(on: Boolean) = haptic(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        },
    )

    /** タブ・選択肢の切り替え(目盛りをまたぐ小さな感触) */
    fun tick() = haptic(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        },
    )

    /** 保存・削除などがうまくいった */
    fun confirm() = haptic(HapticFeedbackConstants.CONFIRM)

    /** 入力が足りないなど、受け付けられなかった */
    fun reject() = haptic(HapticFeedbackConstants.REJECT)

    private fun haptic(type: Int) {
        view.performHapticFeedback(type)
    }
}

@Composable
fun rememberGlassFeedback(): GlassFeedback {
    val view = LocalView.current
    return remember(view) { GlassFeedback(view) }
}

/**
 * 押されたガラスの状態。指の動きに合わせてアニメーションする。
 *
 * - [depth]: 0 = 休んでいる、1 = 指に吸い寄せられて手前へ浮いている。離すとばねで戻り、少しだけ負(壁側)へ行き過ぎる
 * - [touch]: 触った所の光の強さ 0..1
 * - [point]: 触った位置(部品の左上からのピクセル)
 */
@Stable
class GlassPress internal constructor() {
    internal val depth = Animatable(0f)
    internal val touch = Animatable(0f)
    internal var point by mutableStateOf(Offset.Unspecified)

    /** いまの押し込みの深さ(読むと、変わるたびに読んだ所が描き直される) */
    val depthValue: Float get() = depth.value

    /** 押し始めの動き [pressIn] が、指へ寄りきる(いちど 1 に届く)か、止められるまで待つ。 */
    internal suspend fun awaitReached(pressIn: Job?) {
        if (pressIn == null) return
        snapshotFlow { depth.value >= 0.98f || !pressIn.isActive }.first { it }
    }
}

/**
 * [interactionSource](押す・離す・取り消し)を見て、[GlassPress] を動かす。
 * 素早く叩いても吸い付く動きが目に見えるよう、指へ寄りきる(いちど 1 に届く)のを待ってから戻し始める。
 */
@Composable
fun rememberGlassPress(interactionSource: InteractionSource, feedback: GlassFeedback?): GlassPress {
    val press = remember { GlassPress() }
    LaunchedEffect(interactionSource, feedback) {
        var pressIn: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    press.point = interaction.pressPosition
                    feedback?.pressDown()
                    pressIn = launch { press.depth.animateTo(1f, SoftGlassMotion.pressSpring) }
                    launch { press.touch.animateTo(1f, tween(SoftGlassMotion.TOUCH_IN_MILLIS)) }
                }
                is PressInteraction.Release -> {
                    feedback?.pressUp()
                    val pressing = pressIn
                    launch {
                        press.awaitReached(pressing)
                        press.depth.animateTo(0f, SoftGlassMotion.releaseSpring)
                    }
                    launch {
                        press.awaitReached(pressing)
                        press.touch.animateTo(0f, tween(SoftGlassMotion.TOUCH_FADE_MILLIS))
                    }
                }
                is PressInteraction.Cancel -> {
                    launch { press.depth.animateTo(0f, SoftGlassMotion.releaseSpring) }
                    launch { press.touch.animateTo(0f, tween(SoftGlassMotion.TOUCH_FADE_MILLIS)) }
                }
            }
        }
    }
    return press
}

/** 「アニメーションを削除」が設定されているか。登場と傾きの動きを出さないために使う。 */
internal fun Context.prefersReducedMotion(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * 端末の傾き(-1..1)。部屋の光の向きを少し動かして、影とつやを傾きに追従させる。
 * 描画のときに値を読むので、State のまま配る(傾くたびに画面を組み立て直さない)。
 */
val LocalLightTilt = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

/**
 * 重力センサーで端末の傾きを測る。画面を表示している間だけセンサーを使う。
 * 持ち方が変わっても光がずれたままにならないよう、基準の傾きはゆっくり今の持ち方へ追従する。
 */
@Composable
fun rememberLightTilt(): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    val reduceMotion = remember(context) { context.prefersReducedMotion() }
    if (reduceMotion) return tilt
    LifecycleResumeEffect(context) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = if (sensorManager != null && sensor != null) {
            TiltListener(tilt).also { sensorManager.registerListener(it, sensor, SensorManager.SENSOR_DELAY_GAME) }
        } else {
            null
        }
        onPauseOrDispose {
            if (listener != null) sensorManager?.unregisterListener(listener)
        }
    }
    return tilt
}

private class TiltListener(private val out: MutableState<Offset>) : SensorEventListener {
    private var baseX = Float.NaN
    private var baseY = 0f
    private var x = 0f
    private var y = 0f

    override fun onSensorChanged(event: SensorEvent) {
        val gx = event.values[0] / SensorManager.GRAVITY_EARTH
        val gy = event.values[1] / SensorManager.GRAVITY_EARTH
        if (baseX.isNaN()) {
            baseX = gx
            baseY = gy
        }
        baseX += (gx - baseX) * BASELINE_FOLLOW
        baseY += (gy - baseY) * BASELINE_FOLLOW
        val tx = ((gx - baseX) / FULL_TILT).coerceIn(-1f, 1f)
        val ty = ((gy - baseY) / FULL_TILT).coerceIn(-1f, 1f)
        x += (tx - x) * SMOOTHING
        y += (ty - y) * SMOOTHING
        val current = out.value
        if (abs(x - current.x) > MIN_CHANGE || abs(y - current.y) > MIN_CHANGE) out.value = Offset(x, y)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        /** これだけ傾けると光の動きが最大になる(重力の割合。約17度) */
        const val FULL_TILT = 0.3f
        const val BASELINE_FOLLOW = 0.008f
        const val SMOOTHING = 0.18f
        const val MIN_CHANGE = 0.008f
    }
}

/** 部屋のキーライトの向き(シェーダーの lightDir)。ふだんは左上・手前。傾きで少し動く。 */
internal fun lightDirection(tilt: Offset): FloatArray = floatArrayOf(
    -0.55f - SoftGlassMotion.TILT_LIGHT * tilt.x,
    -0.5f + SoftGlassMotion.TILT_LIGHT * tilt.y,
    0.6f,
)
