package com.example.businesscard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 角丸トークン。見本の部品は「pillではなく、控えめな角丸の長方形」。
 * 高さに対して約1/4の角丸(56dpの部品なら14dp)にしている。
 */
object SoftGlassShapes {
    /** ボタン・入力欄・ドロップダウン・タブの外枠 */
    val control = RoundedCornerShape(14.dp)

    /** 部品の中のタイル(アイコンの四角、選択中のタブ) */
    val tile = RoundedCornerShape(12.dp)

    /** 名刺 */
    val card = RoundedCornerShape(22.dp)

    /** ダイアログ */
    val dialog = RoundedCornerShape(26.dp)

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
 */
object SoftGlassLight {
    val shadowOffsetX = 14.dp
    val shadowOffsetY = 14.dp
    val shadowBlur = 12.dp
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
