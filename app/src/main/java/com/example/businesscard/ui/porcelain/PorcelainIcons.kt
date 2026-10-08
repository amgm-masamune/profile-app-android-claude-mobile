package com.example.businesscard.ui.porcelain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 見本で使われている線画のアイコンのうち、Material の標準アイコン(core)に無いもの。 */
object PorcelainIcons {
    /** 空の丸(見本の「○ Hover with glow」)。選んでいない選択肢の印 */
    val Ring: ImageVector by lazy {
        ImageVector.Builder(
            name = "Ring",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
                moveTo(12f, 4f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 16f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -16f)
                close()
            }
        }.build()
    }
}
