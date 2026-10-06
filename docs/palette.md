# Tepera — палітра кольорів

Довідник кольорів застосунку. Джерело істини — код: `ui/theme/TeperaColors.kt` (токени теми),
`ui/theme/TeperaPalette.kt` (ролі інтерфейсу), `data/DefaultCategories.kt` (кольори категорій),
`res/values/colors.xml` і `res/drawable/*.xml` (ресурси). Значення не змінені — це фіксація поточного стану.
Коли змінюєш колір у коді, онови цей файл.

Формат: `#RRGGBB` або `#AARRGGBB` (альфа першою, як у Compose `0xAARRGGBB`).

## 1. Токени теми (`TeperaColors.kt`)

Тема йде за системним режимом (світла / темна). Легасі-набір (`LegacyTeperaColors`) — запасний варіант, поки екран не переведено на редизайн.

| Роль | Світла | Темна | Де використовується |
|---|---|---|---|
| Фон, верх → низ | `#F9F0DF` → `#EDE9E8` | `#062814` → `#11322E` | Градієнт екранів |
| Розмита пляма, вгорі праворуч | `#FFF2D2` | — | Фон Home (світла) |
| Розмита пляма, внизу ліворуч | `#C2BDF6` @30% | — | Фон Home (світла) |
| Основний текст / іконки (ink) | `#003926` | `#FEFFEF` | Текст, заголовки |
| Другорядний текст | `#767676` | `#C5DCD9` | Підписи |
| Фірмовий акцент (brand) | `#006944` | `#FEFFEF` | Дрібні елементи, чіпи часу |
| Картка (card) | `#EDF1E5` | `#164233` | Картки |
| Скляна картка / рядок | `#FFFFFF` @70% | `#164233` | "Твій день", Щоденник, Налаштування |
| Поверхня чипа (surfaceBrandLight) | `#DCF6ED` | `#1A3D38` | Чіпи, тональні поверхні |
| Primary-кнопка / головна дія | `#003926` (текст `#FFFFFF`) | `#FEFFEF` (текст `#062924`) | Кнопки, play, вибраний сегмент |
| Невибраний чип | `#FEFEFE` (текст `#003926`) | `#003926` (текст `#FEFFEF`) | Чіпи |
| Кола-кнопки шапки | `#FCFBF7` | `#164233` | Шапка Home |
| Кнопка "додати час" | `#C5E2CB` | `#C5E2CB` | Картка категорії |
| Навбар (пігулка) | `#4DFFFFFF` (скло) | `#4DFFFFFF` | Нижній навбар |
| Вибрана вкладка навбару | `#006944` (вміст `#DCF6ED`) | `#FEFFEF` (вміст `#062924`) | Навбар |
| Невибрана вкладка навбару | `#FFFFFF` (іконка `#505050`) | `#1A3D38` (іконка `#FEFFEF`) | Навбар |
| Вимкнений трек перемикача | `#F0F3F4` | `#1A3D38` | Перемикачі |
| Крапки пейджера | `#003926` @25% | `#FEFFEF` @30% | Картки Home |
| Поле вводу | `#F0F3F4` | `#003926` | Пошук, поля |
| Рамка поля | `#DDE2E4` | `#FEFFEF` @30% | Поля часу |
| Рамка картки активності | `#FFFFFF` | — | Картки Home |

Легасі-значення (поки що використовуються іншими екранами): ink `#003926`, brand `#006944`, текст `#505050`,
картка `#FFFFFF` @70%, primary `#006944`, навбар `#FFFFFF` @70%, вибрана вкладка `#B2E5D3`.

## 2. Категорії

| Категорія | Колір | id |
|---|---|---|
| Природа | `#00B938` | `default-nature` |
| Читання | `#D28FDF` | `default-reading` |
| Хобі/творчість | `#00D8CD` | `default-hobby` |
| Рух/спорт | `#FD5B5E` | `default-movement` |
| Живе спілкування | `#FF73D0` | `default-social` |
| Справи | `#A362FF` | `default-errands` |

Палітра для користувацьких категорій (`CategoryVisuals.kt`): `#7D9D8C`, `#C17C74`, `#8E7CC3`, `#4D8FAC`,
`#D4A373`, `#6B8E6B`, `#A65D7C`, `#5B7B9A`.

## 3. Дані: Online, "Решта дня", орієнтир, теплова карта

| Елемент | Колір | Примітка |
|---|---|---|
| Online | `#F5C401` (непрозорий) | Сегмент шкали, рядок Online |
| Лінія Online-тренду | `#006944` → `#00CF87` @0% | Градієнт під лінією (`OnlineTrendChart.kt`) |
| "Офлайн-життя" (решта дня) | `#C5E2CB` | Сегмент шкали |
| Орієнтир | заливка `#33F5C401`, рамка `#F5C401` | Тонка вертикальна лінія (`BalanceCard.kt`) |
| Теплова карта: 0–1 хв | `#C3C3C3` (світла) / `#003926` (темна) | Кошики |
| Теплова карта: пік | `#E69D00` | Бурштин |
| Теплова карта: немає даних | `#F0F3F4`, рамка `#D4DADD` | |
| Майбутнє (сітка віджета) | `#A7A7A7` @50% | |
| Початок дня (до точки старту) | `#A172FF` | Сітка віджета, онбординг |

## 4. Віджет

| Елемент | Колір |
|---|---|
| Фон | Градієнт teal → майже чорний → бордовий (`widget_gradient_bg.xml`) |
| Невибране коло | `#FFFFFF` @30% |
| Вибране коло | `#FFFFFF` |
| Гліф вибраний | `#003926` (однаковий для всіх категорій) |
| Гліф невибраний | `#FFFFFF` на склі |
| Занедбана категорія | Бурштинова обвідка `#FFC876` |
| Ресурси для лаунчера (`values` / `values-night`) | `widget_circle_unselected` `#FFFFFF`, `widget_glyph_unselected` `#505050` |
| Превʼю віджета | `#A9C9B6` → `#7DA28F`, `#ECC3F2`, `#FFA172FF`, `#80A7A7A7` |

## 5. Іконка застосунку, заставка, онбординг

| Елемент | Колір |
|---|---|
| Заставка (фон, `splash_background`) | `#8AC396`; плями `#65FF93`, `#FDFFD2`, `#BFC1EB`, `#FFFFB1`, `#004C51` |
| Іконка застосунку | Фон `#F5E3C0` → `#FFF6E4`; пагорби `#B2E5D3`; знак `#003926` → `#699129` |
| Підкладка віджета (`widget_gradient_bg.xml`) | `#12171F` — базова заливка під градієнтом; темний онбординг, для якого колір був, прибрано |

## 6. Усі hex-значення (автоматично)

Повний перелік літералів у `app/src/main` (`.kt` і `.xml`), з кількістю входжень і файлами, де вони зустрічаються.
Значення `0xAARRGGBB` з Kotlin приведені до `#AARRGGBB`. Порядок — за кількістю. Файли показані без шляху.

| Колір | Входжень | Файли |
|---|---|---|
| `#FF000000` | 49 | ic_tile_add.xml, ic_widget2_brush.xml, ic_widget2_brush_selected.xml, ic_widget2_celebration.xml (+45) |
| `#003926` | 37 | CategoryHistoryScreen.kt, DiaryScreen.kt, OnboardingScreen.kt, DayStatsUi.kt (+14) |
| `#006944` | 28 | DiaryScreen.kt, GatePauseScreen.kt, BalanceCard.kt, HomeScreen.kt (+10) |
| `#80A7A7A7` | 21 | TeperaWidget.kt, widget_legend_rest_dot.xml, widget_preview_4x2.xml |
| `#FFFFFFFF` | 21 | TeperaColors.kt, TeperaPalette.kt, widget_circle_solid.xml, widget_legend_rest_dot.xml (+4) |
| `#B3FFFFFF` | 19 | TeperaColors.kt, TeperaPalette.kt |
| `#DCF6ED` | 16 | DiaryScreen.kt, GatePauseScreen.kt, TeperaNavHost.kt, OnboardingScreen.kt (+3) |
| `#FF006944` | 15 | DiaryScreen.kt, GatePauseScreen.kt, BalanceCard.kt, TeperaColors.kt (+1) |
| `#FFA172FF` | 15 | WidgetSuggestionScreen.kt, widget_preview_4x2.xml |
| `#FF003926` | 14 | TeperaColors.kt, TeperaPalette.kt |
| `#4DFFFFFF` | 13 | TeperaColors.kt, TeperaPalette.kt, widget_card_translucent.xml, widget_pill_translucent.xml (+5) |
| `#505050` | 12 | TeperaNavHost.kt, TeperaColors.kt, TeperaIcons.kt, TeperaWidget.kt (+7) |
| `#FFFFFF` | 12 | TeperaColors.kt, TeperaWidget.kt, ic_notification_summary.xml, ic_perm_check_off.xml (+4) |
| `#FF505050` | 11 | TeperaColors.kt, TeperaPalette.kt, TeperaWidget.kt, ic_widget2_preview_hobby.xml (+4) |
| `#FFFEFFEF` | 10 | TeperaColors.kt, TeperaWidget.kt |
| `#FFC5E2CB` | 8 | AddEntryScreen.kt, TeperaColors.kt, TeperaPalette.kt, TeperaWidget.kt |
| `#FFF5C401` | 8 | BalanceCard.kt, TeperaPalette.kt, TeperaWidget.kt, widget_preview_4x2.xml |
| `#C5E2CB` | 7 | AddEntryScreen.kt, DayStatsUi.kt, TeperaPalette.kt |
| `#12171F` | 6 | TeperaNavHost.kt, OnboardingScreen.kt, widget_gradient_bg.xml |
| `#FF0F0F10` | 6 | AddEntryScreen.kt, TeperaPalette.kt, TeperaTextField.kt |
| `#FFDCF6ED` | 6 | GatePauseScreen.kt, TeperaColors.kt, TeperaComponents.kt, TeperaPalette.kt |
| `#FFF0F3F4` | 6 | TeperaColors.kt, TeperaPalette.kt |
| `#FF1A3D38` | 5 | TeperaColors.kt, TeperaWidget.kt |
| `#FFECC3F2` | 5 | TeperaWidget.kt, widget_preview.xml, widget_preview_1x1.xml, widget_preview_2x1.xml (+1) |
| `#FFF6E4` | 5 | ic_launcher_background.xml, splash_logo_background.xml |
| `#7DA28F` | 4 | widget_preview.xml, widget_preview_1x1.xml, widget_preview_2x1.xml, widget_preview_4x2.xml |
| `#A9C9B6` | 4 | widget_preview.xml, widget_preview_1x1.xml, widget_preview_2x1.xml, widget_preview_4x2.xml |
| `#DDE2E4` | 4 | BalanceCard.kt, TrackingSettingsScreen.kt, TeperaPalette.kt |
| `#FFD28FDF` | 4 | WidgetSuggestionScreen.kt, widget_preview_4x2.xml |
| `#699129` | 3 | ic_launcher_foreground.xml, splash_logo_foreground.xml |
| `#B2E5D3` | 3 | ic_launcher_background.xml, splash_logo_background.xml |
| `#F5C401` | 3 | BalanceCard.kt, TeperaPalette.kt, TeperaWidget.kt |
| `#F5E3C0` | 3 | ic_launcher_background.xml, splash_logo_background.xml |
| `#FF062924` | 3 | TeperaColors.kt, TeperaPalette.kt |
| `#FF164233` | 3 | TeperaColors.kt |
| `#FF8AC396` | 3 | WidgetSuggestionScreen.kt, DayStatsUi.kt, TeperaPalette.kt |
| `#00CF87` | 2 | OnlineTrendChart.kt |
| `#0F0F10` | 2 | AddEntryScreen.kt, TeperaPalette.kt |
| `#1AF0F3F4` | 2 | TeperaColors.kt, TeperaPalette.kt |
| `#80FFFFFF` | 2 | BalanceCard.kt, TeperaColors.kt |
| `#8AC396` | 2 | TeperaPalette.kt, colors.xml |
| `#ADADAD` | 2 | OnlineTrendChart.kt |
| `#F0F3F4` | 2 | TeperaTextField.kt |
| `#FF00D8CD` | 2 | WidgetSuggestionScreen.kt, widget_preview_4x2.xml |
| `#FF783D83` | 2 | TeperaWidget.kt, ic_widget2_preview_reading_selected.xml |
| `#FFB2E5D3` | 2 | TeperaColors.kt, TeperaPalette.kt |
| `#FFD4DADD` | 2 | TeperaPalette.kt |
| `#FFDDE2E4` | 2 | BalanceCard.kt, TeperaPalette.kt |
| `#FFFCFBF7` | 2 | TeperaColors.kt |
| `#FFFD5B5E` | 2 | WidgetSuggestionScreen.kt, widget_preview_4x2.xml |
| `#FFFEFEFE` | 2 | TeperaColors.kt |
| `#FFFFFF4D` | 2 | TeperaColors.kt, widget_card_translucent.xml |
| `#00000000` | 1 | widget_root_none.xml |
| `#0040A8BA` | 1 | widget_gradient_bg.xml |
| `#004C51` | 1 | TeperaPalette.kt |
| `#0065FF93` | 1 | widget_gradient_bg.xml |
| `#00B938` | 1 | DefaultCategories.kt |
| `#00D8CD` | 1 | DefaultCategories.kt |
| `#00FF1C7E` | 1 | widget_gradient_bg.xml |
| `#062814` | 1 | TeperaColors.kt |
| `#11322E` | 1 | TeperaColors.kt |
| `#14003926` | 1 | TeperaPalette.kt |
| `#164233` | 1 | TeperaColors.kt |
| `#1A006944` | 1 | TeperaPalette.kt |
| `#1C1B1F` | 1 | DiaryScreen.kt |
| `#1D5945` | 1 | TeperaPalette.kt |
| `#220D99` | 1 | TeperaPalette.kt |
| `#33F5C401` | 1 | BalanceCard.kt |
| `#40A8BA` | 1 | widget_gradient_bg.xml |
| `#45FF1C7E` | 1 | widget_gradient_bg.xml |
| `#4A6FA5` | 1 | CategoryVisuals.kt |
| `#4D062924` | 1 | WidgetSuggestionScreen.kt |
| `#4D65FF93` | 1 | widget_gradient_bg.xml |
| `#4D8FAC` | 1 | CategoryVisuals.kt |
| `#4DC2BDF6` | 1 | TeperaColors.kt |
| `#4E7A51` | 1 | CategoryVisuals.kt |
| `#5B7B9A` | 1 | CategoryVisuals.kt |
| `#5C6B73` | 1 | CategoryVisuals.kt |
| `#65FF93` | 1 | widget_gradient_bg.xml |
| `#6B8E6B` | 1 | CategoryVisuals.kt |
| `#790645` | 1 | ic_widget2_hobby_selected.xml |
| `#7A5C7A` | 1 | CategoryVisuals.kt |
| `#7D9D8C` | 1 | CategoryVisuals.kt |
| `#8000C567` | 1 | TeperaPalette.kt |
| `#8040A8BA` | 1 | widget_gradient_bg.xml |
| `#8A8F5C` | 1 | CategoryVisuals.kt |
| `#8E7CC3` | 1 | CategoryVisuals.kt |
| `#A172FF` | 1 | DailyGridCalculator.kt |
| `#A362FF` | 1 | DefaultCategories.kt |
| `#A65D7C` | 1 | CategoryVisuals.kt |
| `#A7A7A7` | 1 | widget_legend_rest_dot.xml |
| `#A87DFF` | 1 | TeperaWidget.kt |
| `#B08968` | 1 | CategoryVisuals.kt |
| `#C17C74` | 1 | CategoryVisuals.kt |
| `#C9704F` | 1 | CategoryVisuals.kt |
| `#CCFFFFFF` | 1 | TeperaColors.kt |
| `#D28FDF` | 1 | DefaultCategories.kt |
| `#D4A373` | 1 | CategoryVisuals.kt |
| `#E2DED1` | 1 | BalanceCard.kt |
| `#EBFAE6` | 1 | CategoryVisuals.kt |
| `#EDE9E8` | 1 | TeperaColors.kt |
| `#EDF1E5` | 1 | TeperaPalette.kt |
| `#F1F0F1` | 1 | TeperaColors.kt |
| `#F5F5F5` | 1 | PatternMiniCard.kt |
| `#F9F0DF` | 1 | TeperaColors.kt |
| `#FADEEE` | 1 | ic_widget2_hobby_selected.xml |
| `#FD5B5E` | 1 | DefaultCategories.kt |
| `#FEFEFE` | 1 | TeperaComponents.kt |
| `#FF004340` | 1 | TeperaWidget.kt |
| `#FF004C51` | 1 | TeperaPalette.kt |
| `#FF00744C` | 1 | TeperaWidget.kt |
| `#FF026813` | 1 | WeeklyDigestCard.kt |
| `#FF062814` | 1 | TeperaColors.kt |
| `#FF11322E` | 1 | TeperaColors.kt |
| `#FF1C7E` | 1 | widget_gradient_bg.xml |
| `#FF1D5945` | 1 | TeperaPalette.kt |
| `#FF1F5A44` | 1 | WidgetSuggestionScreen.kt |
| `#FF213260` | 1 | TeperaWidget.kt |
| `#FF220D99` | 1 | WeeklyDigestCard.kt |
| `#FF42550F` | 1 | TeperaWidget.kt |
| `#FF65FF93` | 1 | TeperaPalette.kt |
| `#FF73D0` | 1 | DefaultCategories.kt |
| `#FF750D99` | 1 | WeeklyDigestCard.kt |
| `#FF767676` | 1 | TeperaColors.kt |
| `#FF790645` | 1 | TeperaWidget.kt |
| `#FF7C5B14` | 1 | TeperaWidget.kt |
| `#FF9162` | 1 | TeperaPalette.kt |
| `#FF986800` | 1 | WeeklyDigestCard.kt |
| `#FFA87DFF` | 1 | TeperaWidget.kt |
| `#FFADADAD` | 1 | OnlineTrendChart.kt |
| `#FFBCC9FA` | 1 | TeperaWidget.kt |
| `#FFBFC1EB` | 1 | TeperaPalette.kt |
| `#FFC3C3C3` | 1 | TeperaPalette.kt |
| `#FFC5DCD9` | 1 | TeperaColors.kt |
| `#FFC6F1EF` | 1 | TeperaWidget.kt |
| `#FFDCD5C6` | 1 | TeperaPalette.kt |
| `#FFE2DED1` | 1 | BalanceCard.kt |
| `#FFE3EAE5` | 1 | TeperaColors.kt |
| `#FFE3EFC4` | 1 | TeperaWidget.kt |
| `#FFE69D00` | 1 | TeperaPalette.kt |
| `#FFEBFAE6` | 1 | CategoryVisuals.kt |
| `#FFEDE9E8` | 1 | TeperaColors.kt |
| `#FFEDF1E5` | 1 | TeperaColors.kt |
| `#FFF9F0DF` | 1 | TeperaColors.kt |
| `#FFFADEEE` | 1 | TeperaWidget.kt |
| `#FFFAECCC` | 1 | TeperaWidget.kt |
| `#FFFCFCFB` | 1 | TeperaColors.kt |
| `#FFFDFFD2` | 1 | TeperaPalette.kt |
| `#FFFFB1` | 1 | TeperaPalette.kt |
| `#FFFFC876` | 1 | widget_circle_outline_neglected.xml |
| `#FFFFF2D2` | 1 | TeperaColors.kt |
| `#FFFFFFB1` | 1 | TeperaPalette.kt |

Примітка: `#FF000000` (чорний) і `#FFFFFFFF` (білий) часто входять у `Color.Black` / `Color.White` і в
Compose-коді — цей перелік лише про явні hex-літерали. Іменовані `Color.White`, `Color.Black`, `Color.Transparent`
у Compose-коді не перераховані.
