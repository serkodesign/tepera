package com.serkodesign.tepera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.serkodesign.tepera.ui.navigation.TeperaNavHost
import com.serkodesign.tepera.ui.theme.TeperaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TeperaApp
        setContent {
            TeperaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TeperaNavHost(
                        categoryRepository = app.categoryRepository,
                        activityRepository = app.activityRepository
                    )
                }
            }
        }
    }
}
