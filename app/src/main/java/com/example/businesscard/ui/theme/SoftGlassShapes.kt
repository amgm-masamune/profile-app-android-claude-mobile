package com.example.businesscard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 角丸トークン。見本の部品は「pillではなく、控えめな角丸の長方形」。
 * 見本の比率(高さの約1/5)に合わせ、56dpの部品なら11dpにしている。
 */
object SoftGlassShapes {
    /** ボタン・入力欄・ドロップダウン・タブの外枠 */
    val control = RoundedCornerShape(11.dp)

    /** 部品の中のタイル(アイコンの四角、選択中のタブ) */
    val tile = RoundedCornerShape(9.dp)

    /** 名刺 */
    val card = RoundedCornerShape(20.dp)

    /** ダイアログ */
    val dialog = RoundedCornerShape(22.dp)

    /** トグル(スイッチ)だけは完全な丸み */
    val pill = RoundedCornerShape(percent = 50)
}

/** 大きさトークン。 */
object SoftGlassSize {
    /** ボタン・入力欄の高さ(タップしやすい48dp以上) */
    val control = 56.dp

    /** 四角いアイコンボタンの一辺 */
    val iconButton = 56.dp

    val switchWidth = 64.dp
    val switchHeight = 36.dp
    val switchThumb = 26.dp
}

/**
 * 光と影のトークン。影や光の形は、この値から壁の照明シェーダーが計算する(ui/glass/GlassShaders.kt)。
 */
object SoftGlassLight {
    /**
     * 部品が壁から浮いている高さ。左上のキーライトで、影はこの高さに比例して右下へずれ(約22dp, 20dp)、
     * 壁から離れるほどぼける。光源が壁を照らす広がりもこの高さで決まる。
     */
    val elevation = 24.dp

    /** すりガラスが背後をぼかす強さ(RenderEffect のぼかし半径) */
    val frostBlur = 18.dp
}

/** 余白トークン(4dpグリッド)。 */
object SoftGlassSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** Material3 の標準部品(メニュー等)に効かせる角丸。 */
val SoftGlassMaterialShapes = Shapes(
    extraSmall = SoftGlassShapes.tile,
    small = SoftGlassShapes.tile,
    medium = SoftGlassShapes.control,
    large = SoftGlassShapes.card,
    extraLarge = SoftGlassShapes.dialog,
)
