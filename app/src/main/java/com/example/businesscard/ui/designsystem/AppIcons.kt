package com.example.businesscard.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 見た目に関係なく使うアイコンのうち、Material の標準アイコン(core)に無いもの。 */
object AppIcons {
    /** 見た目の切り替え。丸の左半分を塗った印(明暗・配色の切り替えでよく使われる形) */
    val Theme: ImageVector by lazy {
        ImageVector.Builder(
            name = "Theme",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // 外の丸(線)
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
                moveTo(12f, 3.5f)
                arcToRelative(8.5f, 8.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 17f)
                arcToRelative(8.5f, 8.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -17f)
                close()
            }
            // 左半分(塗り)
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3.5f)
                arcToRelative(8.5f, 8.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0f, dy1 = 17f)
                close()
            }
        }.build()
    }
}
