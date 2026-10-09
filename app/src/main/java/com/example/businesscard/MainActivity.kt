package com.example.businesscard

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.ui.designsystem.BusinessCardAppTheme
import com.example.businesscard.ui.designsystem.isDark
import com.example.businesscard.ui.glass.LocalGlowHeadroom
import com.example.businesscard.ui.glass.LocalLightTilt
import com.example.businesscard.ui.glass.rememberGlowHeadroom
import com.example.businesscard.ui.glass.rememberLightTilt
import com.example.businesscard.ui.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBars(ThemeStyle.DEFAULT)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            // 保存された見た目を読み込むまで(最初の一瞬)は何も描かない(違う見た目が一瞬映らないように)
            val style = (uiState as? MainActivityUiState.Ready)?.themeStyle
            if (style != null) {
                val edgelit = style == ThemeStyle.EDGELIT
                // 見た目に合わせて、ステータスバー・ナビゲーションバーのアイコンの色を変える
                DisposableEffect(style) {
                    applySystemBars(style)
                    onDispose {}
                }

                // HDR対応の画面では、光源(Edgelit の縁の光・Porcelain の波の裏の光)を白より明るく光らせる。
                // Edgelit だけ: 端末を傾けると部屋の光の向きが少し動く(ほかの見た目では傾きのセンサーを使わない)
                val headroom = rememberGlowHeadroom(this)
                val tilt = if (edgelit) rememberLightTilt() else LocalLightTilt.current
                CompositionLocalProvider(
                    LocalGlowHeadroom provides headroom,
                    LocalLightTilt provides tilt,
                ) {
                    BusinessCardAppTheme(style) {
                        AppNavHost()
                    }
                }
            }
        }
    }

    /** 背景が暗い見た目ではバーのアイコンを白に、明るい見た目では黒にする。バー自体は透明。 */
    private fun applySystemBars(style: ThemeStyle) {
        val bar = if (style.isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
    }
}
