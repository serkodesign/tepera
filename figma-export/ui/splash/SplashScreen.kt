package com.serkodesign.tepera.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.splashGradientBackground
import kotlinx.coroutines.delay

/** Скільки тримати заставку, перш ніж [SplashScreen] викличе [onFinished]. */
private const val SPLASH_DURATION_MS = 1200L

/**
 * Заставка при холодному запуску — Figma "App concept" (k6s4prQ9oK9x2uUvzHRghR), node 294:1901.
 * Показується рівно [SPLASH_DURATION_MS], потім [onFinished] веде на Home — звідти вже працює
 * наявний ланцюжок онбордингу (`TeperaNavHost.nextOnboardingRoute`/`HomeScreen`), тож сама
 * заставка нічого не знає про категорії/дозволи/стан онбордингу.
 *
 * Хвилясті лінії (node 294:1904, "Group 3") — той самий SVG-актив, що вже лежить у проєкті як
 * `perm_bg_waves.xml` (побайтово ідентичний pathData/viewBox — той самий Figma-вузол, раніше
 * малював колишній темний екран дозволів, невикористаний після його редизайну на світлий
 * градієнт). Положення — буквальні координати кадру 375x812 (той самий підхід, що вже
 * використовує [splashGradientBackground] для розмитих плям): не адаптується під інший aspect
 * ratio екрана, але це вже усталений і перевірений на 4 тестових пристроях компроміс для
 * декоративного фону цього застосунку.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onFinished()
    }
    Box(modifier = Modifier.splashGradientBackground()) {
        Image(
            painter = painterResource(R.drawable.perm_bg_waves),
            contentDescription = null,
            modifier = Modifier
                .offset(x = (-219).dp, y = 123.dp)
                .size(width = 731.dp, height = 802.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(RoundedCornerShape(41.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_logo_background),
                    contentDescription = null,
                    modifier = Modifier.size(144.dp)
                )
                Image(
                    painter = painterResource(R.drawable.splash_logo_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(144.dp)
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                color = TeperaPalette.buttonBrandDark,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
