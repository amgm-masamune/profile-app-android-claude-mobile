package com.example.businesscard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 角丸トークン。ボタンはpill(完全な丸み)、面は大きめの角丸。 */
object SoftGlassShapes {
    val pill = RoundedCornerShape(percent = 50)
    val field = RoundedCornerShape(22.dp)
    val card = RoundedCornerShape(28.dp)
    val dialog = RoundedCornerShape(32.dp)
}

/** 影の高さトークン。 */
object SoftGlassElevation {
    val raised = 10.dp
    val floating = 18.dp
}

/** 余白トークン(4dpグリッド)。 */
object SoftGlassSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** Material3 の標準コンポーネント(ダイアログ等)に効かせる角丸。 */
val SoftGlassMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = SoftGlassShapes.field,
    large = SoftGlassShapes.card,
    extraLarge = SoftGlassShapes.dialog,
)
