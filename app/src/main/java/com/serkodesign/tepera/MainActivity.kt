package com.serkodesign.tepera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.serkodesign.tepera.ui.theme.TeperaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeperaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomePlaceholder()
                }
            }
        }
    }
}

// Плейсхолдер до Фази 1 (екран Home/Сьогодні, FR-1.x).
@Composable
private fun HomePlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.home_today_summary),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
