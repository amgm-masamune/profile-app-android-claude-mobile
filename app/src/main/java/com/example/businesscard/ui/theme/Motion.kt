package com.example.businesscard.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * 動きと手触りのトークン。「壁から浮いたガラス板」が、触ると物として反応するように決めている。
 *
 * - 押す: 板が壁へ沈む(浮いている高さが下がって影が縮み、遠ざかるぶん少し小さく見える)
 * - 離す: ばねで戻り、少しだけ手前へ行き過ぎて止まる
 * - 登場: 画面を開くと、上の部品から順に壁から浮き上がり、光源が灯る
 */
object SoftGlassMotion {
    /** 押し込んだとき、浮いている高さが減る割合(24dp → 約8dp) */
    const val PRESS_SINK = 0.65f

    /** 押し込んだとき、長い辺がこれだけ縮む(大きな部品ほど縮む割合は小さい) */
    val pressTravel = 8.dp

    /** 押し込んだときの最小の大きさ(小さなアイコンボタンが縮みすぎないように) */
    const val MIN_PRESS_SCALE = 0.93f

    /** 押し込んだとき、光源がこの強さまで明るくなる(主ボタンの 1.0 を少し超える) */
    const val PRESSED_GLOW = 1.2f

    const val PRESS_IN_MILLIS = 90
    const val TOUCH_IN_MILLIS = 140

    /** 離したあと、触った所の光が消えるまで */
    const val TOUCH_FADE_MILLIS = 480

    /** 離したときのばね。少し行き過ぎて止まる */
    val releaseSpring: AnimationSpec<Float> = spring(dampingRatio = 0.42f, stiffness = 520f)

    /** 登場: 画面のいちばん下の部品が、いちばん上よりこれだけ遅れて浮き上がる */
    const val ENTRANCE_STAGGER_MILLIS = 260

    /** 画面ができてからこの時間内に現れた部品だけ登場の動きをする(スクロールで出てきた名刺は動かさない) */
    const val ENTRANCE_WINDOW_MILLIS = 500L

    val entranceSpring: AnimationSpec<Float> = spring(dampingRatio = 0.62f, stiffness = 190f)

    /** 選択中のタブ・タイル、トグルのつまみが滑るばね */
    val slideSpring: AnimationSpec<Float> = spring(dampingRatio = 0.72f, stiffness = 420f)
    val thumbSpring: AnimationSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 500f)

    /** 光源の強さ・浮く高さが変わるときの時間(フォーカスやトグルで点く・消える) */
    const val GLOW_CHANGE_MILLIS = 220

    /** 端末の傾きで光の向きが動く量 */
    const val TILT_LIGHT = 0.35f
}
