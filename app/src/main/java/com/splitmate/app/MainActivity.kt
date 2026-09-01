package com.splitmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.splitmate.app.ui.SplitmateApp
import com.splitmate.app.ui.theme.SplitmateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitmateTheme {
                SplitmateApp()
            }
        }
    }
}
