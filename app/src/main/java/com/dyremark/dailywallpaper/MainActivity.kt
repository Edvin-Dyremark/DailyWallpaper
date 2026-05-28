package com.dyremark.dailywallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dyremark.dailywallpaper.ui.HomeScreen
import com.dyremark.dailywallpaper.ui.theme.DailyWallpaperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyWallpaperTheme {
                HomeScreen()
            }
        }
    }
}
