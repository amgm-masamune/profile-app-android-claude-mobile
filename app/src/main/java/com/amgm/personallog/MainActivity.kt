package com.amgm.personallog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.amgm.personallog.ui.startup.StartupGate
import com.amgm.personallog.ui.theme.PersonalLogTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalLogTheme {
                StartupGate()
            }
        }
    }
}
