package com.example.businesscard.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * 動きと手触りのトークン。「壁から浮いたガラス板」が、触ると物として反応するように決めている。
 *
 * - 押す: 板が指に吸い寄せられ、壁から少し手前へ浮き上がる(影が伸びてぼけ、近づくぶん少し大きく見える)。
 *   奥へ沈むと指から逃げるように感じるので、指の方へ寄せる
 * - 離す: 吸い付きが外れてばねで壁側へ戻り、少しだけ行き過ぎて止まる
 * - 登場: 画面を開くと、上の部品から順に壁から浮き上がり、光源が灯る
 */
object SoftGlassMotion {
    /** 押したとき、浮いている高さが増える割合(24dp → 約31dp) */
    const val PRESS_LIFT = 0.3f

    /** 押したとき、長い辺がこれだけ大きくなる(大きな部品ほど大きくなる割合は小さい) */
    val pressGrow = 4.dp

    /** 押したときの最大の大きさ(小さなアイコンボタンが大きくなりすぎないように) */
    const val MAX_PRESS_SCALE = 1.04f

    /** 押したとき、光源がこの強さまで明るくなる(主ボタンの 1.0 を少し超える) */
    const val PRESSED_GLOW = 1.1f

    /**
     * 押し始めのばね。少しだけ行き過ぎて、指へ「ピタッ」と吸い付く。
     * 約0.06秒で寄りきる(素早く叩いたときも、ここまでは見せてから戻す)
     */
    val pressSpring: AnimationSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 1500f)
    const val TOUCH_IN_MILLIS = 140

    /** 離したあと、触った所の光が消えるまで */
    const val TOUCH_FADE_MILLIS = 480

    /** 離したときのばね。壁側へ少し行き過ぎて止まる */
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
}
