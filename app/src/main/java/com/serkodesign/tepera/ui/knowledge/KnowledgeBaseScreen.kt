package com.serkodesign.tepera.ui.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaPalette

private data class KnowledgeSection(val titleRes: Int, val bodyRes: Int)

private val SECTIONS = listOf(
    KnowledgeSection(R.string.knowledge_base_section_1_title, R.string.knowledge_base_section_1_body),
    KnowledgeSection(R.string.knowledge_base_section_2_title, R.string.knowledge_base_section_2_body),
    KnowledgeSection(R.string.knowledge_base_section_3_title, R.string.knowledge_base_section_3_body),
    KnowledgeSection(R.string.knowledge_base_section_4_title, R.string.knowledge_base_section_4_body)
)

/**
 * "База знань" — T-16 частина А (tepera-dev-spec.md): офлайн-пояснення того, звідки беруться
 * дані, як працюють паузи/ворота/вікно сну і чому немає сповіщень/стріків/оцінок. Частина Б
 * (вікі про думскролінг/дофамін) лишається заблокованою рішенням власника — свідомо відсутня.
 * Увесь контент видно одразу на одному екрані (без переходу на рядок — Trampoline UX), кожен
 * розділ пройшов той самий аудит на оцінювальні слова, що й решта UI (інваріант 2.1/2.5).
 */
@Composable
fun KnowledgeBaseScreen(onBack: () -> Unit) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.knowledge_base_screen_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.knowledge_base_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                SECTIONS.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(section.titleRes),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = stringResource(section.bodyRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(TeperaPalette.cardTranslucentLight)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
