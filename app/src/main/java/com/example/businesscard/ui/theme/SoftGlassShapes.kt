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
 * 光と影のトークン。見本は「左上から光が当たり、壁から少し離れて浮いている」。
 * 影は大きさに関係なく同じだけ右下にずれる(どの部品も壁から同じ距離に浮いているため)。
 * 見本の影は「ぼけた影」ではなく「縁だけ柔らかい板状の影」なので、ずれを大きく、ぼかしを小さくしている。
 */
object SoftGlassLight {
    val shadowOffsetX = 22.dp
    val shadowOffsetY = 20.dp
    val shadowBlur = 7.dp
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
