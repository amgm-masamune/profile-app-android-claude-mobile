package com.example.businesscard

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.example.businesscard.ui.glass.LocalGlowHeadroom
import com.example.businesscard.ui.glass.LocalLightTilt
import com.example.businesscard.ui.glass.rememberGlowHeadroom
import com.example.businesscard.ui.glass.rememberLightTilt
import com.example.businesscard.ui.navigation.AppNavHost
import com.example.businesscard.ui.theme.BusinessCardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 壁の色は中間の明るさで、文字は白。ステータスバーのアイコンも常に白にする
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            // HDR対応の画面では、光源を白より明るく光らせる。端末を傾けると部屋の光の向きが少し動く
            CompositionLocalProvider(
                LocalGlowHeadroom provides rememberGlowHeadroom(this),
                LocalLightTilt provides rememberLightTilt(),
            ) {
                BusinessCardTheme {
                    AppNavHost()
                }
            }
        }
    }
}
