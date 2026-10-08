package com.example.businesscard.ui.component

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 見本で使われている線画のアイコンのうち、Material の標準アイコンに無いもの。 */
object GlassIcons {
    /** 見本の「Icon button」にある、右上へ曲がる矢印(転送・共有)。線だけで描く。 */
    val ForwardArrow: ImageVector by lazy {
        ImageVector.Builder(
            name = "ForwardArrow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14f, 9f)
                verticalLineTo(5f)
                lineToRelative(7f, 7f)
                lineToRelative(-7f, 7f)
                verticalLineToRelative(-4.1f)
                curveToRelative(-5f, 0f, -8.5f, 1.6f, -11f, 5.1f)
                curveToRelative(1f, -5f, 4f, -10f, 11f, -11f)
                close()
            }
        }.build()
    }
}
