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
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.NavChevron
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.TeperaCard
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
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
fun KnowledgeBaseScreen(onOpenScrollingNotes: () -> Unit, onBack: () -> Unit) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.knowledge_base_screen_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.knowledge_base_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TeperaPalette.buttonBrandDark,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                // Кожен розділ — єдина картка застосунку (TeperaCard: скляний білий 80%, r16, заголовок
                // Golos Medium 16/24), текст — M3 bodyMedium брендовим темним кольором.
                SECTIONS.forEach { section ->
                    TeperaCard(title = stringResource(section.titleRes)) {
                        Text(
                            text = stringResource(section.bodyRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
                        )
                    }
                }
                // T-16 Б: окрема тема "що відомо / чого не відомо" — окремим екраном, щоб частина А лишалась чистою.
                GlassRow(
                    label = stringResource(R.string.scrolling_notes_row),
                    onClick = onOpenScrollingNotes,
                    leading = { TeperaIconCircle(TeperaSymbols.Info) },
                    trailing = { NavChevron() }
                )
            }
        }
    }
}

private val SCROLLING_SECTIONS = listOf(
    KnowledgeSection(R.string.scrolling_notes_1_title, R.string.scrolling_notes_1_body),
    KnowledgeSection(R.string.scrolling_notes_2_title, R.string.scrolling_notes_2_body),
    KnowledgeSection(R.string.scrolling_notes_3_title, R.string.scrolling_notes_3_body),
    KnowledgeSection(R.string.scrolling_notes_4_title, R.string.scrolling_notes_4_body),
    KnowledgeSection(R.string.scrolling_notes_5_title, R.string.scrolling_notes_5_body)
)

/**
 * T-16 частина Б (tepera-dev-spec.md, розділ 4.1) — за прямим рішенням власника, попри застереження ТЗ. Редакційні
 * правила ТЗ обов'язкові: форма лише "що відомо / чого не відомо / де вчені не згодні" з датою перегляду; жодних
 * тверджень про "дофамінову залежність" як про факт, жодної статистики, відсотків чи "досліджень як аргументу",
 * жодних порад/інструкцій/чеклистів, жодних клікабельних зовнішніх посилань (дозволу INTERNET нема). Текст
 * проходить той самий аудит на оцінювальні слова, що й UI (розділ 2.1) — при будь-якій правці перечитати.
 */
@Composable
fun ScrollingNotesScreen(onBack: () -> Unit) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.scrolling_notes_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.scrolling_notes_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TeperaPalette.buttonBrandDark,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                SCROLLING_SECTIONS.forEach { section ->
                    TeperaCard(title = stringResource(section.titleRes)) {
                        Text(
                            text = stringResource(section.bodyRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TeperaPalette.buttonBrandDark.copy(alpha = 0.85f)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.scrolling_notes_reviewed),
                    style = MaterialTheme.typography.bodySmall,
                    color = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
