package com.example.businesscard.ui.porcelain

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * デザインシステム「Porcelain(ポーセリン、仮称)」のトークン。
 *
 * 見本(グレーの壁の前に、明るいグレーの板が浮き、その上に丸いボタンが並ぶUIキット。
 * 板の中ほどを波打つ薄い板が横切り、その縁から暖かい光がにじむ)から、色・形・影を採寸した。
 *
 *  - 壁: 中間のグレー。左上が明るく、右下が暗い
 *  - 板(パネル): 明るいグレーの陶器のような面。右下へ長く濃い影を落とす
 *  - ボタン: 角が完全に丸い(pill)。明るい板(白っぽい)と、濃いグレーの板の2種類。
 *    どちらも左上に光、右下に柔らかい影があり、上の縁につやがある
 *  - 押したボタン(見本の Active): 外の影が消え、内側の上に影ができる(板に沈み込む)
 *  - 光: 波の縁から、白〜暖色の光がにじむ(描き方は PorcelainShaders.kt)
 *
 * 画面に直接 Color(0x...) を書かず、必ずこのトークン経由で使う。
 */
@Immutable
data class PorcelainColors(
    // 壁(画面の地)
    val wallLight: Color,
    val wallDark: Color,
    // 板(パネル)
    val panelTop: Color,
    val panelBottom: Color,
    // 光
    val glowCore: Color,
    val glowWarm: Color,
    val sheetTint: Color,
    val focusGlow: Color,
    // 部品の面
    val lightTop: Color,
    val lightBottom: Color,
    val darkTop: Color,
    val darkBottom: Color,
    val mutedTop: Color,
    val mutedBottom: Color,
    // 文字
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val onDark: Color,
    val onDarkMuted: Color,
    val danger: Color,
    val scrim: Color,
)

val DefaultPorcelainColors = PorcelainColors(
    wallLight = Color(0xFFBCBCBC),
    wallDark = Color(0xFF9C9C9C),
    panelTop = Color(0xFFDADADA),
    panelBottom = Color(0xFFC6C6C6),
    glowCore = Color(0xFFFFFDF8),
    glowWarm = Color(0xFFFFE6D0),
    sheetTint = Color(0xFFF5CDA8),
    focusGlow = Color(0xFFFFE3C4),
    lightTop = Color(0xFFE4E4E4),
    lightBottom = Color(0xFFDAD9D8),
    darkTop = Color(0xFF727171),
    darkBottom = Color(0xFF656464),
    mutedTop = Color(0xFFA3A3A3),
    mutedBottom = Color(0xFF999999),
    ink = Color(0xFF2F2F2F),
    inkMuted = Color(0xFF5C5C5C),
    inkFaint = Color(0xFF8A8A8A),
    onDark = Color(0xFFF4F4F4),
    onDarkMuted = Color(0xFFD2D2D2),
    danger = Color(0xFF9A3A33),
    scrim = Color(0x66000000),
)

val LocalPorcelainColors = staticCompositionLocalOf { DefaultPorcelainColors }

/** `PorcelainTheme.colors.ink` のように参照する入口。 */
object PorcelainTheme {
    val colors: PorcelainColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPorcelainColors.current
}

/** 形のトークン。ボタン・入力欄は完全な丸み(pill)、板は大きめの角丸。 */
object PorcelainShapes {
    val pill = RoundedCornerShape(percent = 50)
    val card = RoundedCornerShape(24.dp)
    val panel = RoundedCornerShape(30.dp)
    val dialog = RoundedCornerShape(30.dp)
}

/** 大きさのトークン。 */
object PorcelainSize {
    /** ボタン・入力欄の高さ(見本の比率のまま、タップしやすい 48dp 以上にした) */
    val control = 52.dp

    /** 丸いアイコンボタンの直径 */
    val iconButton = 52.dp

    /** 入力欄の左の丸いアイコン台の直径 */
    val fieldIcon = 40.dp
}

/** 余白のトークン(4dpグリッド)。 */
object PorcelainSpacing {
    /** 壁から板(パネル)までの余白。右下は影が落ちるので少し広い */
    val panelStart = 10.dp
    val panelTop = 8.dp
    val panelEnd = 14.dp
    val panelBottom = 16.dp

    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 28.dp
}

/**
 * 影と光のトークン(見本の写真から採寸)。光は左上から当たり、影は右下へ落ちる。
 * 値は docs/porcelain/tools/surfaces.py と同じ(図の作成・見本との見比べに使う模型)。
 */
@Immutable
data class PorcelainElevation(
    /** 左上の光(白い影)のずれ・ぼけ・濃さ */
    val lightDx: Dp,
    val lightDy: Dp,
    val lightBlur: Dp,
    val lightAlpha: Float,
    /** 右下へ落ちる柔らかい影 */
    val dropDx: Dp,
    val dropDy: Dp,
    val dropBlur: Dp,
    val dropAlpha: Float,
    /** 板の真下の、くっきりした接地の影 */
    val contactDy: Dp,
    val contactBlur: Dp,
    val contactAlpha: Float,
) {
    companion object {
        /** ボタン・入力欄 */
        val Control = PorcelainElevation(
            lightDx = (-2).dp, lightDy = (-2.5).dp, lightBlur = 6.dp, lightAlpha = 0.55f,
            dropDx = 2.dp, dropDy = 5.5.dp, dropBlur = 10.dp, dropAlpha = 0.36f,
            contactDy = 1.5.dp, contactBlur = 2.5.dp, contactAlpha = 0.2f,
        )

        /** 名刺(大きな板なので、高く浮いて影も長い) */
        val Card = PorcelainElevation(
            lightDx = (-3).dp, lightDy = (-3.5).dp, lightBlur = 9.dp, lightAlpha = 0.6f,
            dropDx = 4.dp, dropDy = 10.dp, dropBlur = 20.dp, dropAlpha = 0.34f,
            contactDy = 2.dp, contactBlur = 4.dp, contactAlpha = 0.18f,
        )

        /** パネル(壁から大きく浮いている。見本では右下に長く濃い影) */
        val Panel = PorcelainElevation(
            lightDx = (-2).dp, lightDy = (-2).dp, lightBlur = 10.dp, lightAlpha = 0.35f,
            dropDx = 12.dp, dropDy = 16.dp, dropBlur = 30.dp, dropAlpha = 0.42f,
            contactDy = 3.dp, contactBlur = 6.dp, contactAlpha = 0.18f,
        )
    }
}

/** 押し込まれた(見本の Active)ときの、内側の影。 */
object PorcelainInset {
    val dx = 1.dp
    val dy = 3.dp
    val blur = 6.dp
    const val ALPHA = 0.26f

    /** 下側の内側に入る光(明るい板だけ。濃い板では弱める) */
    const val LIGHT_ALPHA = 0.45f
}

/** 波と光のトークン。 */
object PorcelainLight {
    /** 板の下端から、波の縁のいちばん低い所(左端)までの高さ。下の操作ボタンのすぐ上に縁が来る */
    val waveLift = 92.dp

    /** 波の高低差は、板の短い辺のこの割合(見本の比率) */
    const val WAVE_AMPLITUDE = 0.233f
}

/**
 * 動きと手触りのトークン。「陶器の板に埋め込まれたボタン」が、押すと沈み込んで底に当たり、離すと跳ね返る。
 */
object PorcelainMotion {
    /**
     * 押し始め: 素早く沈み、底に当たってわずかに跳ねて止まる(キーを底まで押した手応え)。
     * 約0.08秒で沈みきる
     */
    val pressSpring: AnimationSpec<Float> = spring(dampingRatio = 0.62f, stiffness = 1800f)

    /** 離したとき: ばねで押し戻され、少しだけ浮きすぎてから落ち着く */
    val releaseSpring: AnimationSpec<Float> = spring(dampingRatio = 0.48f, stiffness = 600f)

    /** 押したときに縮む割合(底に当たったとき) */
    const val PRESS_SCALE = 0.975f

    /** 光や色が変わるときの時間(フォーカスで縁が光る、など) */
    const val CHANGE_MILLIS = 220

    /** 画面を開いたとき、波の縁の光が灯るまでの時間 */
    const val GLOW_IN_MILLIS = 900
}

/** 文字トークン。見本の文字は太さ Medium の落ち着いたサンセリフ。明るい板には濃い文字、濃い板には白い文字。 */
object PorcelainType {
    val title = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val dialogTitle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
    val button = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val field = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val cardName = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold)
    val cardCompany = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
}
