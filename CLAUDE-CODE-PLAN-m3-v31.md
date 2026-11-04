# Tepera · перехід на дизайн-систему v3.1 (Material 3) — план для Claude Code

> **Для Claude Code.** Цей файл є єдиним джерелом правди для цієї роботи. Контекст чату може зникнути будь-якої миті (вимкнення світла), тому все потрібне записано тут. **Перед кожною сесією** прочитай розділи «Протокол відновлення» та «Журнал прогресу».

---

## 0. Що робимо і чого НЕ робимо

**Мета:** перевести UI Tepera на нові токени з Figma-сторінки **v3.1 · M3** (файл «Tepera»):
- кольори: M3-ролі з окремими Light/Dark;
- шрифти: шкала M3 на Golos Text;
- форми, відступи;
- поп-апи у стилі Material 3.

Figma: https://www.figma.com/design/k6s4prQ9oK9x2uUvzHRghR/Tepera?node-id=1011-291
(усі значення нижче вже вивантажені з Figma, доступ до неї для роботи не потрібен).

**Не робимо:**
- не змінюємо бізнес-логіку, моделі даних, AccessibilityService, WorkManager, логіку вікон ризику;
- не додаємо нових фіч v3.0. Якщо екрана/поп-апа з макета ще немає в коді, робимо лише візуальний компонент із `@Preview` (без підключення до логіки) і позначаємо це в журналі;
- не вмикаємо dynamic color (кольори зі шпалер): `dynamicColor = false`;
- не змінюємо шрифт: лишається **Golos Text**.

---

## 1. Правила роботи, бо світло вимикають

1. **Гілка:** `ui/m3-v31`. Ніколи не працюй у `main`.
2. **Одна ітерація = 15–25 хвилин і не більше ~5 файлів.** Якщо ітерація розростається, зупинись, закоміть те, що компілюється, і розбий решту на під-ітерації (`I13a`, `I13b`…) прямо в цьому файлі.
3. **Кінець кожної ітерації (обов'язково, саме в такому порядку):**
   1. Швидка перевірка: `./gradlew :app:compileDebugKotlin`. Повний `assembleDebug` запускай лише там, де це вказано в ітерації.
   2. Постав `[x]` у чекліст і додай рядок у «Журнал прогресу» (дата, ітерація, що зроблено, що лишилось).
   3. Один коміт на ітерацію, разом з цим файлом: `git add -A && git commit -m "M3-I07: <коротко>"`.
   4. Якщо налаштований remote: `git push`. Push захищає від пошкодження диска при раптовому вимкненні.
4. **Ніколи не починай наступну ітерацію, поки попередня не закомічена.**
5. **Жодних масових пошуків-замін по всьому проєкту за раз.** Тільки в межах файлів поточної ітерації.
6. Не запускай довгих задач (повні тести, `lint` по всьому проєкту), якщо ітерація цього не вимагає.

### Протокол відновлення (після вимкнення світла або нової сесії)

1. Прочитай цей файл повністю. Знайди першу ітерацію без `[x]`.
2. `git status` та `git log --oneline -5`.
3. Якщо Git скаржиться на пошкоджений індекс (`index file corrupt`, `bad signature`):
   - `del .git\index` (Windows) або `rm .git/index`;
   - потім `git reset`;
   - потім `git fsck --full`. Якщо `fsck` показує биті об'єкти, зупинись і напиши Сергію, нічого не видаляй.
4. Якщо є незакомічені зміни:
   - вони належать поточній ітерації і компілюються → доведи ітерацію до кінця за п. 3 правил;
   - не компілюються або незрозуміло, що це → `git stash push -m "half-I<NN>"` і почни ітерацію заново (вона маленька, це дешевше, ніж розплутувати).
5. Якщо Gradle поводиться дивно після вимкнення:
   - спершу `./gradlew --stop`;
   - потім знову `:app:compileDebugKotlin`;
   - тільки якщо не допомогло → `./gradlew clean`.
6. Допиши в журнал рядок `відновлено після перерви`.

---

## 2. Токени (фінальні значення з Figma)

### 2.1 Кольори — ролі M3 → `MaterialTheme.colorScheme`

| Роль | Light | Dark |
|---|---|---|
| primary | `#003926` | `#83D8AA` |
| onPrimary | `#FFFFFF` | `#003926` |
| primaryContainer | `#DCF6ED` | `#005234` |
| onPrimaryContainer | `#003926` | `#9EF4C5` |
| surface | `#FAF9F9` | `#121414` |
| surfaceDim | `#DADADA` | `#121414` |
| surfaceBright | `#FAF9F9` | `#38393A` |
| surfaceContainerLowest | `#FFFFFF` | `#0D0E0F` |
| surfaceContainerLow | `#F4F3F4` | `#1A1C1C` |
| surfaceContainer | `#EEEEEE` | `#1E2020` |
| surfaceContainerHigh | `#E8E8E8` | `#292A2B` |
| surfaceContainerHighest | `#E3E2E2` | `#333535` |
| onSurface | `#1A1C1C` | `#E3E2E2` |
| onSurfaceVariant | `#434749` | `#C3C7C8` |
| outline | `#737879` | `#8D9193` |
| outlineVariant | `#C3C7C8` | `#434749` |
| inverseSurface | `#2F3131` | `#E3E2E2` |
| inverseOnSurface | `#F1F0F1` | `#2F3131` |
| inversePrimary | `#83D8AA` | `#003926` |
| error | `#BA1A1A` | `#FFB4AB` |
| onError | `#FFFFFF` | `#690005` |
| errorContainer | `#FFDAD6` | `#93000A` |
| onErrorContainer | `#410002` | `#FFDAD6` |
| scrim | `#000000` | `#000000` |

Для `secondary*` і `tertiary*` пропиши ті самі значення, що й для `primary*`. Так стандартні компоненти M3 не підхоплять фіолетові кольори за замовчуванням.

Поверхні навмисно **нейтрально-сірі, без бежевого**. Не «теплішай» їх.

### 2.2 Кольори Tepera → `TeperaTheme.colors` (CompositionLocal)

Цих ролей немає в M3. Тримай їх в окремому `data class TeperaColors`, а не в `ColorScheme`. Примітка: у Figma в codeSyntax для `signal*` помилково стоїть `MaterialTheme.colorScheme.signal`, у коді правильно `TeperaTheme.colors.signal`. Формат `#RRGGBB@NN%` означає колір з альфою NN%.

| Роль | Light | Dark | Де використовується |
|---|---|---|---|
| signal | `#805600` | `#FFBA47` | текст/іконки «вікна ризику» |
| onSignal | `#FFFFFF` | `#442B00` | |
| signalContainer | `#FFDDB0` | `#614000` | бейдж «6 з 7 вечорів» |
| onSignalContainer | `#614000` | `#FFDDB0` | текст бейджа |
| signalVivid | `#E69D00` | `#FFBA47` | крапки повторюваності, смуги графіка, легенда «стрічки» |
| inverseSignal | `#FFBA47` | `#805600` | іконка в snackbar |
| card | `#FFFFFF` | `#292A2B` | фон карток |
| cardStroke | `#FFFFFF` | `#FFFFFF@8%` | обвідка карток 1dp |
| onDark | `#FFFFFF` | `#FFFFFF` | текст на зеленій hero-картці |
| glass | `#FFFFFF@62%` | `#FFFFFF@8%` | скляні рядки/хедери |
| glassStrong | `#FFFFFF@80%` | `#FFFFFF@14%` | круглі кнопки в шапці |
| bgGradientTop | `#F9F0DF` | `#1B1B1D` | фон-градієнт (як у v3.0) |
| bgGradientBottom | `#EDE9E8` | `#03190C` | |
| bgBlobWarm | `#FFF2D2` | `#FFF2D2@10%` | пляма вгорі праворуч |
| bgBlobCool | `#C2BDF6@30%` | `#C2BDF6@10%` | пляма внизу ліворуч |
| dataHeat0 | `#E8E8E8` | `#2F3131` | порожня клітинка хітмапи |
| dataHeat1 | `#FFEEDB` | `#442B00` | |
| dataHeat2 | `#FFDDB0` | `#614000` | |
| dataHeat3 | `#FFCC81` | `#805600` | |
| dataHeat4 | `#F5AA1A` | `#C38400` | |
| dataHeat5 | `#E69D00` | `#FFBA47` | пік |
| dataTrack | `#E3E2E2` | `#38393A` | доріжка прогрес-барів |
| dataNature | `#5D905A` | `#96C590` | категорія |
| dataReading | `#A373AC` | `#D8A9DE` | |
| dataHobby | `#30928B` | `#79C6BF` | |
| dataMovement | `#BF6D6A` | `#F6A3A0` | |
| dataSocial | `#B26E97` | `#E8A5CB` | також аватар застосунку в шторці |
| dataErrands | `#9278B8` | `#C7AEE9` | |
| dataOnline | `#9C812B` | `#D3B666` | |

Хітмапа: рівень клітинки 0–5 визначається частотою стрічки в цю годину. Старі прозорості бурштину переводяться так:

| Стара прозорість | Рівень |
|---|---|
| немає даних | heat0 |
| ≤0.19 | heat1 |
| ≤0.30 | heat2 |
| ≤0.60 | heat3 |
| ≤0.90 | heat4 |
| 1.0 | heat5 |

### 2.3 Шрифти → `Typography` (Golos Text)

Ваги: Display/Headline/Title/Label використовують `FontWeight.Medium`, Body використовує `FontWeight.Normal`. Значення в sp, letterSpacing в sp.

| Стиль M3 | size / lineHeight | letterSpacing | Де в макеті |
|---|---|---|---|
| displayLarge | 57 / 64 | −0.86 | |
| displayMedium | 45 / 52 | −0.68 | |
| displaySmall | 36 / 44 | −0.36 | |
| headlineLarge | 32 / 40 | −0.32 | заголовок онбордингу |
| headlineMedium | 28 / 36 | −0.28 | |
| headlineSmall | 24 / 32 | −0.12 | заголовок шторок і діалогів, шапка головної |
| titleLarge | 22 / 28 | −0.11 | час вікна «22:40 – 00:10», заголовки з «назад» |
| titleMedium | 16 / 24 | 0 | заголовок плашки виходу, лейбл кнопки M |
| titleSmall | 14 / 20 | 0 | назва застосунку в шторці |
| bodyLarge | 16 / 24 | 0 | текст плану в шторці |
| bodyMedium | 14 / 20 | 0 | основний текст |
| bodySmall | 12 / 16 | 0 | мета-рядки, пояснення |
| labelLarge | 14 / 20 | 0 | лейбл кнопки S |
| labelMedium | 12 / 16 | 0 | бейджі, чипи |
| labelSmall | 11 / 16 | +0.11 | осі й легенди графіків |

Стара шкала → нова (для міграції екранів):

| Стара | Нова |
|---|---|
| Header 0 | displaySmall |
| Header 1 | headlineMedium |
| Header 2 | titleLarge |
| Subheader 1 | titleLarge |
| Subheader 2 | titleSmall |
| Subheader 3 | titleMedium |
| Body 1 | bodyLarge |
| Body 3 | bodyMedium |
| Body 2 | bodySmall |
| Tag | labelSmall |
| Onboarding/Title | headlineLarge |
| текст у кнопках | labelLarge або titleMedium за розміром кнопки |

`Onboarding/Numeral` (156sp) лишається окремим кастомним стилем.

### 2.4 Форми → `Shapes` + `TeperaShapes`

- **M3 `Shapes`:**
  - extraSmall = 4dp
  - small = 8dp
  - medium = 12dp
  - large = 16dp
  - extraLarge = 28dp
- **`TeperaShapes`:**
  - largeIncreased = 20dp
  - extraLargeIncreased = 32dp
  - full = `CircleShape` (50%)
- **Правило:**
  - картки: 16dp;
  - шторки: 28dp тільки зверху;
  - плашка виходу: 28dp;
  - кнопки, чипи, бейджі: full;
  - snackbar: 4dp;
  - клітинки хітмапи: 2dp (виняток).

### 2.5 Відступи → `object TeperaSpacing`

Шкала: `2, 4, 8, 12, 16, 20, 24, 32, 40` dp. Значення поза шкалою заборонені.

| Що | dp |
|---|---|
| Поле екрана зліва/справа | 16 |
| Між картками / секціями | 12 |
| Вступний текст → перший контент | 16 |
| Поле всередині картки (всі боки) | 16 |
| Групи всередині картки | 12 |
| Рядки одного блоку (час → мета → крапки) | 4 |
| Пара «заголовок / підпис» | 0–4 |
| Між кнопками | 8 |
| Шапка головної та екрана «День N з 7», горизонтально | 16 |
| Кнопки-іконки в шапках | **48×48** (мін. ціль дотику M3) |
| Крапки повторюваності, між крапками | 8 |

---

## 3. Специфікації компонентів

### 3.1 `TeperaButton(type, size, text, onClick, modifier)`

- **Type:**

  | Type | Фон | Текст |
  |---|---|---|
  | Filled | primary | onPrimary |
  | Tonal | surfaceContainerHighest | onSurface |
  | Outlined | без фону, обвідка 1dp outlineVariant | primary |
  | Text | без фону | primary |

- **Size:**

  | Size | Висота | Поле по горизонталі | Лейбл |
  |---|---|---|---|
  | S | 40dp | 16dp (Text: 12dp) | labelLarge |
  | M | 56dp | 24dp | titleMedium |

- Форма full. Для S додай `Modifier.minimumInteractiveComponentSize()` (ціль 48dp).

### 3.2 Фон `TeperaBackground`

- Вертикальний градієнт bgGradientTop → bgGradientBottom.
- Дві м'які плями діаметром ~604dp на макеті 375×812:
  - bgBlobWarm з центром приблизно (369, 20);
  - bgBlobCool з центром приблизно (9, 781).
- Якщо такий composable уже є, **лише заміни кольори на токени**, геометрію не чіпай.

### 3.3 Картка вікна ризику (`RiskWindowCard`)

- **Контейнер:** фон card, обвідка 1dp cardStroke, форма 16dp, поле 16dp, групи через 12dp.
- **Рядок 1:** час (titleLarge, onSurface) і праворуч бейдж «6 з 7 вечорів»:
  - кольори signalContainer / onSignalContainer;
  - стиль labelMedium;
  - поля 12×4dp, форма full.
- **Рядок 2:** мета (bodySmall, onSurfaceVariant), через 4dp крапки.
- **Крапки:** ~7dp, між ними 8dp. Заповнена — signalVivid, порожня — dataHeat0.
- **Роздільник:** outlineVariant 1dp.
- **Низ:**
  - є план: іконка 24dp (onSurfaceVariant) на колі surfaceContainerHighest, текст плану bodyMedium, шеврон;
  - немає плану: «Плану ще немає» (bodyMedium, onSurfaceVariant) і `TeperaButton(Filled, S)`.

### 3.4 Шторка плану `PlanSheet` (M3 ModalBottomSheet)

- **Контейнер:** surfaceContainerLow, форма 28dp зверху / 0 знизу.
- **Ручка:** 32×4dp, onSurfaceVariant з альфою 0.4, відступ зверху 14dp.
- **Вміст:** поля 24dp з боків, 8dp зверху (після ручки), 24dp знизу; вертикальні проміжки 16dp.
- **Рядок застосунку:** аватар 40dp (dataSocial для Instagram), назва titleSmall onSurface, підпис bodySmall onSurfaceVariant.
- **Заголовок** «Твій план на цей час»: headlineSmall, onSurface.
- **Картка плану:**
  - фон surfaceContainerHighest, форма 16dp, поле 16dp;
  - іконка 24dp onSurfaceVariant;
  - текст bodyLarge onSurface.
- **Контекст** «Ти написав це в неділю…»: bodySmall, onSurfaceVariant.
- **Кнопки** (колонка, проміжок 8dp):
  - `Filled M` на всю ширину («Беру книгу»);
  - під нею рядок з проміжком 8dp: `Outlined S` («Відкрити на 5 хв», weight 1) і `Text S` («Не сьогодні», weight 1).
- **Тема:** вечірнє вікно на макеті темне, обіднє світле. Шторка просто бере поточну тему системи. **Ніде не має бути темно-зеленого тла**, тільки surfaceContainerLow.

### 3.5 Підтвердження «Ще 5 хвилин?» `MoreConfirmSheet`

- Та сама оболонка, що в 3.4.
- Заголовок headlineSmall.
- Текст bodyMedium onSurfaceVariant, проміжок до заголовка 8dp.
- Кнопки: `Filled M` «Вийти» на всю ширину, під нею `Text S` «Так, ще 5 хв» на всю ширину.

### 3.6 Плашка «час минув» `ExitPill` (оверлей поверх приглушеного екрана)

- **Тема:** завжди темна (огорни в `TeperaTheme(darkTheme = true)`), бо лежить на затемненні.
- **Контейнер:** surfaceContainerHigh (`#292A2B`), форма 28dp, без обвідки, поля 16dp зверху/знизу і 20dp з боків, проміжки 12dp. Відступи від країв екрана 16dp, від низу ~72dp.
- **Голова:**
  - іконка 22dp кольору signal (`#FFBA47`);
  - заголовок titleMedium onSurface («5 хв минуло» / «Уже 7 хв · намір був 5» / «Час спати»);
  - підпис bodyMedium onSurfaceVariant.
- **Кнопки** (рядок, проміжок 8dp): `Outlined S` «Ще 5» за вмістом, `Filled S` «Вийти»:
  - у компактній стадії за вмістом;
  - у стадіях «+2 хв» і «ніч» з `weight(1f)`.
- **Затемнення під плашкою:** чорний 50% плюс теплий шар signalVivid 18%.

### 3.7 Підтвердження виходу `ExitToast` (стиль M3 snackbar)

- Фон inverseSurface, форма 4dp, поля 12×16dp, висота ~48dp, проміжок 12dp.
- Іконка-галочка 20dp кольору inverseSignal.
- Текст bodyMedium inverseOnSurface.

### 3.8 Хітмапа тижня `HeatWeek`

- Клітинки heat0–5 (форма 2dp).
- Пунктирна рамка «години ризику» кольору primary.
- Осі й легенда labelSmall onSurfaceVariant.
- Контейнер card, поле 16dp, проміжки 8dp.

---

## 4. Відповідність старих токенів новим (з Figma v3.0)

Знайди в коді, як названі ці кольори, і заміни за таблицею.

| Старий (Figma v3.0) | Новий |
|---|---|
| brand, button/primary, button/play-fill, text/brand-dark, border/brand | primary |
| button/on-primary, button/play-content | onPrimary |
| text/primary, chip/on-unselected | onSurface |
| text/secondary, text/placeholder | onSurfaceVariant |
| text/on-dark | TeperaColors.onDark |
| surface/card, surface/default, chip/unselected, input/fill | TeperaColors.card |
| surface/brand-light, switch/track-off, button/filled | surfaceContainerHighest |
| surface/dialog, hero/surface-glass, card/guess-fill | surfaceContainerHigh |
| surface/glass, surface/glass-50 | TeperaColors.glass |
| header/circle-button | TeperaColors.glassStrong |
| border/field | outlineVariant |
| border/default, border/strong | outline |
| card/activity-border | TeperaColors.cardStroke |
| text/error, border/error | error |
| bg/gradient-top / bottom | bgGradientTop / bgGradientBottom |
| bg/blob-top-right | bgBlobWarm |
| bg/blob-bottom-left | bgBlobCool |
| data/heatmap-no-data | dataHeat0 |
| data/heatmap-peak, бурштин `#E69D00` з прозорістю | dataHeat5 / heat1–4 (див. 2.2) |
| data/offline-life, category/nature | dataNature |
| category/reading … category/errands | dataReading … dataErrands |
| category-custom/7 (аватар Instagram) | dataSocial |
| data/online | dataOnline |

Hero-картка на головній («До твоєї години / 2 год 10 хв»):
- фон primary (`#003926`);
- текст onDark;
- іконка місяця primaryContainer.

---

## 5. Ітерації

Позначай `[x]` лише після коміту. Орієнтовний час вказано в дужках.

### Етап A — підготовка
- [x] **I00 · Розвідка (15 хв, без змін коду).** Створи гілку `ui/m3-v31`. Знайди й запиши в «Журнал»:
  - файли теми (Color/Theme/Type/Shape);
  - де підключено Golos Text;
  - скільки хардкод-кольорів `Color(0x…)` у кожному файлі UI;
  - список екранів і поп-апів, які реально існують у коді, з позначкою, чи є відповідник у розділі 3;
  - як влаштовані віджети Glance.

  Скопіюй цей файл у корінь репо як `CLAUDE-CODE-PLAN-m3-v31.md`. Коміт.
- [x] **I01 · План ітерацій екранів (10 хв).** На основі розвідки допиши в етап D конкретні ітерації: по одній на екран, великі екрани розбий на 2. Коміт.

### Етап B — фундамент (візуально ще нічого не змінюється)
- [x] **I02 · Палітра (15 хв).** НОВИЙ файл `ui/theme/M3Palette.kt` (назви `TeperaPalette`/`TeperaColors`/`LocalTeperaColors` зайняті наявним кодом, рішення 2026-11-04): усі hex з 2.1 і 2.2 як `val` (лише константи). Коміт.
- [x] **I03 · ColorScheme (15 хв).** `TeperaLightColors` / `TeperaDarkColors` через `lightColorScheme(...)` / `darkColorScheme(...)` за 2.1. Поки ніде не підключати. Коміт.
- [x] **I04 · TeperaColors (20 хв).** `data class TeperaExtraColors` + `LightTeperaExtraColors` / `DarkTeperaExtraColors` + `LocalTeperaExtraColors` (staticCompositionLocalOf) + `object M3Extra { val colors @Composable get() = LocalTeperaExtraColors.current }` (у плані це `TeperaTheme.colors`; функція `TeperaTheme` вже існує, тому об'єкт названо інакше; далі в плані `TeperaTheme.colors` читати як `M3Extra.colors`). Коміт.
- [x] **I05 · Typography (15 хв).** `FontFamily` Golos Text (Regular, Medium; шрифти вже в проєкті, якщо ні — додай з Google Fonts, ліцензія OFL) + `TeperaTypography` за 2.3. Коміт.
- [x] **I06 · Shapes і Spacing (10 хв).** `TeperaShapesM3` (Shapes), `TeperaShapes` (extra), `object TeperaSpacing` за 2.4 і 2.5. Коміт.
- [x] **I07 · Підключення теми (20 хв).**
  - `TeperaTheme(darkTheme = isSystemInDarkTheme(), content)` надає `MaterialTheme(colorScheme, typography, shapes)` + `LocalTeperaColors`, `dynamicColor` вимкнено. Підключи в `MainActivity`.
  - `./gradlew assembleDebug`, встанови на S23, перевір, що застосунок запускається в обох темах.
  - Не все виглядатиме ідеально, це нормально. Коміт.
- [x] **I08 · Мостик для старих кольорів (20 хв).**
  - Старі константи кольорів позначити `@Deprecated("use TeperaTheme/MaterialTheme")` і, де можливо, перенаправити на нові значення за таблицею 4, щоб неперенесені екрани виглядали узгоджено.
  - Логіку не чіпати. Коміт.

### Етап C — компоненти (кожен з `@Preview` Light і Dark)
- [x] **I09 · TeperaButton (20 хв)** за 3.1. Коміт.
- [x] **I10 · TeperaBackground (15 хв)** за 3.2. Коміт.
- [x] **I11 · Картка-контейнер + бейдж повторюваності + крапки (20 хв)** за 3.3 (без вмісту вікна). Коміт.
- [x] **I12 · RiskWindowCard (20 хв)** за 3.3. Якщо є в коді, перевести; якщо нема, тільки компонент із превʼю. Коміт.
- [x] **I13 · HeatWeek (20 хв)** за 3.8, включно з мапінгом рівнів heat0–5. Коміт.
- [x] **I14 · PlanSheet (25 хв)** за 3.4. Якщо вийде довше: I14a оболонка + ручка, I14b вміст і кнопки. Коміт.
- [x] **I15 · MoreConfirmSheet (15 хв)** за 3.5. Коміт.
- [ ] **I16 · ExitPill (25 хв)** за 3.6, три стадії в превʼю. Якщо оверлей рендериться через `ComposeView` у WindowManager, перевір, що тема і `LocalTeperaColors` доступні всередині. Коміт.
- [ ] **I17 · ExitToast як snackbar (15 хв)** за 3.7. Коміт.

### Етап D — екрани (одна ітерація = один екран, 15–25 хв)

Для кожного екрана:
- кольори → ролі (2.1, 2.2, табл. 4);
- текст → стилі (2.3);
- `dp` → `TeperaSpacing` (2.5);
- форми → 2.4;
- готові компоненти з етапу C.

Логіку не змінювати. Кожен екран: `compileDebugKotlin`, потім коміт.

**Конкретні ітерації (I01, 2026-11-04).** Нумерація I18+ замінює орієнтир макета. Шляхи відносно `app/src/main/java/com/serkodesign/tepera/`.
- [ ] I18a · v3-онбординг: `ui/risk/V3OnboardingScreens.kt`
- [ ] I18b · `ui/onboarding/WelcomeOnboardingScreen.kt`, `OnboardingScreen.kt` (екран дозволів завжди темний, зберегти)
- [ ] I18c · `CategoryOnboardingScreen.kt`, `OnlineEstimateOnboardingScreen.kt`, `TargetOnboardingScreen.kt`, `GatesOnboardingScreen.kt`
- [ ] I19 · `ui/risk/NowHero.kt`, `V3ModeToggle.kt`
- [ ] I20a · `ui/risk/V3Components.kt`: HeatWeek, RiskWindowCard (уже існують; поєднати з I12/I13)
- [ ] I20b · `ui/risk/WindowsCarousel.kt`, `RiskHomeExtras.kt`, `WeeklySummaryBlock.kt`
- [ ] I21a · `ui/risk/PlanOverlayScreen.kt`: оболонка, ручка, заголовок (PlanSheet, 3.4)
- [ ] I21b · `PlanOverlayScreen.kt`: картка плану, кнопки; `QuickPuzzleViews.kt`
- [ ] I22a · `ui/risk/RiskHomeScreen.kt`: перша половина (шапка, hero-картка, кнопки-іконки 48)
- [ ] I22b · `RiskHomeScreen.kt`: друга половина; `FeedPickerScreen.kt`
- [ ] I23 · `ui/risk/TesterStatsScreen.kt`, `WindowEditorScreen.kt`
- [ ] I24 · `ui/risk/WindowWhyScreen.kt`
- [ ] I25a · Home: `ui/home/HomeHero.kt`, `ActivityList.kt`, `MiniPlayer.kt`
- [ ] I25b · Home: `HomeScreen.kt`, `SummarySheet.kt`, `SummaryTeasers.kt`
- [ ] I25c · Home: контекстні картки (`ContextCardStack.kt`, `HomeCardParts.kt` та решта карток)
- [ ] I25d · `ui/navigation/TeperaNavHost.kt`, `ui/splash/SplashScreen.kt`
- [ ] I25e · Статистика: `StatsScreen.kt`, `DayStatsUi.kt`, `OnlineTrendChart.kt`, `ui/pattern/HourlyHeatStrip.kt`
- [ ] I25f · `ui/diary/DiaryScreen.kt`, `ui/calendar/CalendarScreen.kt`, `ui/addentry/AddEntryScreen.kt`
- [ ] I25g · `ui/category/*`, `ui/knowledge/KnowledgeBaseScreen.kt`
- [ ] I25h · Налаштування: `SettingsScreen.kt`, `TrackingSettingsScreen.kt`, `WidgetSettingsScreen.kt`
- [ ] I25i · Налаштування: `BackupRestoreScreen.kt`, `AboutScreen.kt`, `ExclusionListScreen.kt`, `ProInterestScreen.kt`, тумблери
- [ ] I25j · Ворота: `GatePauseScreen.kt`, `GateScheduleScreen.kt`, `GatesScreen.kt`, `AccessibilityGateUi.kt`
- [ ] I25k · `app/src/beta/.../gates/SoftExitController.kt`: підключити ExitPill/ExitToast з етапу C (якщо I16/I17 цього не зробили)
- [ ] I25l · Режими «Світанок»/«За часом» (`HeroMood.kt`, Dawn/Sky набори в `TeperaColors.kt`): НЕ чіпати до окремого рішення власника

### Етап E — завершення
- [ ] **I26 · Віджети Glance (20 хв).** Перевести кольори віджетів на ті самі значення через `ColorProvider(day, night)`. Не змінювати розмітку. Коміт.
- [ ] **I27 · Прибрати мостик (20 хв).** Знайти використання `@Deprecated` кольорів. Якщо їх 0, видалити старі константи; якщо ні, записати залишки в журнал окремими ітераціями. Коміт.
- [ ] **I28 · QA на пристроях (25 хв, без змін коду).**
  - Пристрої: S23 (Android 16), Moto G84 (15), Huawei P9 (8), XZ1 Compact (9).
  - Для кожного: світла/темна тема, масштаб шрифту 1.3.
  - Знахідки записати в журнал як ітерації `I29+`, кожна на одну проблему. Коміт.

---

## 6. Критерії готовності (для всієї роботи)

- В UI-коді немає `Color(0x…)`, крім `TeperaPalette.kt`.
- Немає довільних `dp` для відступів поза `TeperaSpacing` (розміри іконок і висоти компонентів можна).
- У темній темі немає темно-зелених поверхонь шторок і плашок (`#1A3D38`, `#11322E`, `#164233` тощо). Темно-зелений лишається тільки в нижній точці фонового градієнта (`#03190C`) — так задумано.
- Поверхні карток, шторок і чипів не бежеві.
- Усі кнопки-іконки ≥ 48dp.
- Обидві теми перевірені на щонайменше двох пристроях.

---

## 7. Журнал прогресу

Формат рядка: `РРРР-ММ-ДД ЧЧ:ММ · I<NN> · що зроблено · що лишилось / нотатки`

- 2026-11-04 · план створено (дизайн v3.1 у Figma затверджено: зелений акцент, сірі поверхні без бежевого, фон-градієнт як у v3.0, поп-апи M3)
- 2026-11-04 · I00 · розвідка, код не змінювався (гілка `ui/m3-v31` від `feature/v3-mvp`, remote origin є; коренева копія плану — робоча). Тема: `ui/theme/Theme.kt` (MaterialTheme лише з shapes; вибір Системна/Світла/Темна/За часом + «Світанок»), `TeperaColors.kt` (УЖЕ є `data class TeperaColors`, `LocalTeperaColors`, Redesign/Legacy/Dawn/Sky набори, ~230 `Color(0x`), `TeperaPalette.kt` (71), `TeperaComponents.kt`. Шрифт: `res/font/golos_text.ttf` (variable); `MaterialTheme.typography` не задано. Найбільше хардкоду: TeperaColors 230, TeperaPalette 71, widget/TeperaWidget 16, widget/InfoWidgets 7, HeroMood 7, PlanOverlayScreen 7, WeeklyDigestCard 5, решта по 1–3. Віджети Glance: `widget/TeperaWidget.kt` (Responsive), `InfoWidgets.kt`, уже `ColorProvider(day, night)`. Відповідники розд. 3: PlanSheet≈`ui/risk/PlanOverlayScreen.kt`, RiskWindowCard і HeatWeek≈`ui/risk/V3Components.kt`, ExitPill/ExitToast≈`beta/gates/SoftExitController.kt`.
- 2026-11-04 · I01 · етап D розписано (I18a…I25l). Рішення власника: нові назви (`M3Palette.kt`, `TeperaExtraColors`, `LocalTeperaExtraColors`, `M3Extra.colors`), режими Dawn/Time не чіпати (I25l). `git add -A` не використовувати: в дереві незакомічені `release/` і `docs/play-store.zip`, додавати файли явно. Примітка: `python` на цій машині — заглушка Windows Store, файли правити Edit-ом. Далі: I02.
- 2026-11-04 · I02 · `ui/theme/M3Palette.kt`: `M3Palette.Light/Dark` (ролі M3, 2.1) і `ExtraLight/ExtraDark` (ролі Tepera, 2.2), лише константи; `compileStandardDebugKotlin` чисто. Нотатка: команда плану `compileDebugKotlin` не існує через flavor-и, використовувати `:app:compileBetaDebugKotlin`. Далі: I03.
- 2026-11-04 · I03 · `ui/theme/M3ColorSchemes.kt`: `TeperaLightColors`/`TeperaDarkColors` (secondary/tertiary = primary; background = surface; surfaceVariant = surfaceContainerHighest, бо в таблиці 2.1 ролі нема); `compileBetaDebugKotlin` чисто, ніде не підключено. Далі: I04.
- 2026-11-04 · I04 · `ui/theme/TeperaExtraColors.kt`: `TeperaExtraColors` (+ хелпер `heat(level)` для мапінгу рівнів хітмапи), `LightTeperaExtraColors`/`DarkTeperaExtraColors`, `LocalTeperaExtraColors`, `M3Extra.colors`; `compileBetaDebugKotlin` чисто. Далі: I05.
- 2026-11-04 · I05 · `ui/theme/TeperaTypography.kt`: `GolosTextFamily` (Regular 400 / Medium 500 з наявного `golos_text.ttf`) + `TeperaTypography` за 2.3; `compileBetaDebugKotlin` чисто, не підключено. Далі: I06.
- 2026-11-04 · I06 · `ui/theme/TeperaShapesSpacing.kt`: `TeperaShapesM3` (Shapes 4/8/12/16/28), `TeperaExtraShapes` (largeIncreased 20, extraLargeIncreased 32, full, sheetTop, heatCell 2), `TeperaSpacing` (шкала 2…40 + семантичні псевдоніми). Назву `TeperaShapes` з плану замінено на `TeperaExtraShapes`, бо `TeperaShapes` приватне в Theme.kt. `compileBetaDebugKotlin` чисто. Далі: I07 (підключення теми, потрібен пристрій).
- 2026-11-04 · I07 · `Theme.kt`: для режимів Світла/Темна/Системна `MaterialTheme(colorScheme = TeperaLight/DarkColors, typography = TeperaTypography, shapes = TeperaShapesM3)` + `LocalTeperaExtraColors`; «Світанок» і «За часом» лишились на старому шляху (I25l). `TeperaColors.kt`: `RedesignScope(colors, useOuterM3Scheme = false, content)` — нова опція не підміняти ColorScheme старою (стара `LocalTeperaColors` лишається, тож неперенесені екрани працюють через `TeperaPalette`). Перевірено на пристрої RFCW20L7MCA (assembleBetaDebug, `install -r`): запуск у світлій і темній темі без падінь, Home рендериться в новій палітрі й шрифті Golos Text. Відомі побічні ефекти: M3-`Card` без явної форми тепер 12dp (було 16dp за раніше прийнятим рішенням) — виправити в етапі D картками на `large`; Home на цьому пристрої показує «Usage access is needed» (доступ до статистики не надано, не стосується теми). Нотатка: використовувати `Sdk/platform-tools/adb.exe` (v1.0.41), `/c/adb/adb.exe` (v1.0.32) конфліктує з сервером. Далі: I08.
- 2026-11-04 · I08 · Мостик. Новий `ui/theme/M3LegacyBridge.kt`: `m3BridgeColors(dark)` — `RedesignLight/DarkColors.copy(...)` з підміною полів за таблицею 4 на значення v3.1 (brand/primaryFill/borderBrand→primary; textPrimary/chipContent→onSurface; textSecondary/textPlaceholder→onSurfaceVariant; card/cardActive/chipFill→card; surfaceBrandLight/switchTrackOff→surfaceContainerHighest; dialogSurface→surfaceContainerHigh; listItem→glass; iconButtonFill→glassStrong; border*→outline; error*→error; фон-градієнт і плями; textOnDark→onDark). `ink`: світла=primary, темна=onSurface (м'ятний primary не годиться для основного тексту — відхилення від таблиці). Підключено в `Theme.kt` лише для Світла/Темна/Системна. `TeperaPalette` позначено `@Deprecated` на рівні object (використань: 502 у 59 файлах; 540 попереджень компілятора — лічильник для I27); `TeperaColors`/`LocalTeperaColors` НЕ позначались deprecated, бо ще потрібні режимам Світанок/За часом (I25l) і самому мостику. Прямі константи в `TeperaPalette` (активні картки Home, навбар, heatmap, `Color(0xB3FFFFFF)` тощо) мостик не змінює — це робиться в етапі D. Перевірено на пристрої: Home, Статистика, Щоденник, світла і темна теми, без падінь, текст читається, акценти зелені. Далі: I09 (TeperaButton).
- 2026-11-04 · I09 · `ui/theme/M3Button.kt`: `M3Button(type: M3ButtonType{Filled,Tonal,Outlined,Text}, size: M3ButtonSize{S,M}, text, onClick, modifier, enabled)` за 3.1 (S 40dp / M 56dp, поля 16(Text 12)/24, форма full, обвідка Outlined 1dp outlineVariant, лейбл labelLarge/titleMedium, `minimumInteractiveComponentSize()` для S, стандартні disabled-стани M3). Назва `M3Button`, а не `TeperaButton`: старий `TeperaButton` (Primary/Secondary/Tertiary) не змінювався, його виклики переходять у етапі D, видалення — I27. `@Preview` Light і Dark (галерея всіх типів/розмірів + вимкнені + M на всю ширину). Додано залежності в `app/build.gradle.kts`: `compileOnly(ui-tooling-preview)` (не потрапляє в APK) і `debugImplementation(ui-tooling)`. `compileBetaDebugKotlin`, `compileBetaReleaseKotlin`, `compileStandardReleaseKotlin` чисто. НЕ перевірено візуально: превʼю Android Studio наживо не рендерилось, на пристрої компонент ще ніде не підключений. Далі: I10 (TeperaBackground).
- 2026-11-04 · I10 · `ui/theme/M3Background.kt`: `Modifier.m3Background()` + `M3Background(modifier, content)`, кольори з `M3Extra.colors` (градієнт bgGradientTop→Bottom, плями bgBlobWarm/Cool), геометрія 1:1 з наявним `teperaGradientBackground()` (радіус 302dp, σ 97.55dp, центри 6/20dp і 9/31dp від країв; `drawBlurredBlob` internal). Паралельний варіант: `teperaGradientBackground()` НЕ чіпали, бо його використовують режими Світанок/За часом; на Home/Stats він і так бере значення v3.1 через мостик I08. Підключення в `TeperaNavHost` (з відкатом на старий модифікатор для Світанок/За часом) — I25d. `@Preview` Light/Dark. `compileBetaDebugKotlin` чисто; візуально не перевірено (ніде не підключено). Далі: I11.
- 2026-11-04 · I11 · Нові примітиви за 3.3 (без вмісту вікна): `ui/theme/M3Card.kt` (`M3Card`: фон card, обвідка 1dp cardStroke, форма `shapes.large`=16dp, поле 16, групи 12; `TeperaSpacing`), `ui/risk/M3RecurrenceParts.kt` (`M3RecurrenceBadge`: signalContainer/onSignalContainer, labelMedium, поля 12×4, full; `M3RecurrenceDots`: 7dp, проміжок 8dp, HIT=signalVivid, MISS=dataHeat0, NOT_APPLICABLE=dataHeat0@40%, TalkBack-опис групи). `@Preview` Light/Dark для обох. Наявні `V3Card`/`TeperaChip`/крапки в `RiskWindowCard` НЕ чіпали — там розходження зі специфікацією (крапки 10dp/проміжок 4dp замість 7/8; `HeatWeek` теж на `V3Card`), переводяться в I12/I13. `compileBetaDebugKotlin` чисто (класи перевірено в build/). Не перевірено візуально. Нотатка: великі heredoc у Bash ламаються — файли писати через Write. Далі: I12 (RiskWindowCard).
- 2026-11-04 · I12 · `RiskWindowCard` (`ui/risk/V3Components.kt`) переведено на нові примітиви: `M3Card` (замість `V3Card`), `M3RecurrenceBadge`/`M3RecurrenceDots` (крапки 7dp/8dp замість 10dp/4dp), `MaterialTheme.typography` (titleLarge для часу, bodySmall для мети), кольори з `MaterialTheme.colorScheme`, `TeperaSpacing`. Логіка, сигнатура, клік «Чому це вікно?», паузa/вимкнено/власне — без змін. Відхилення від 3.3: (1) бейдж повторюваності НЕ в рядку 1 праворуч, а в нижньому рядку разом із крапками — картки каруселі вузькі (~260dp), поруч із часом і шевроном бейдж обрізався («4 з 7 днів»→«4»); (2) блок «план / немає плану» (іконка+текст плану / «Плану ще немає»+кнопка) НЕ додано: у `RiskWindowUi` даних про план нема, а картка свідомо лише про вікно — це нова функція, поза обсягом. Знайдено й усунено при перевірці: `FlowRow` у рядку 1 давав карусель (`IntrinsicSize.Max`) хибну висоту й обрізав картку знизу; а `Spacer(weight)` поруч із бейджем ділив місце навпіл і різав текст бейджа — обидва варіанти відкинуто. `Theme.kt`: у гілці «Світанок/За часом» тепер теж надається `LocalTeperaExtraColors` за `colors.isDark` (інакше `M3Card` там лишався б світлим). Перевірено на пристрої (assembleBetaDebug, install -r): картка вікна в світлій і темній темі, шрифт 1.0 і 1.3, без падінь. Для перевірки на пристрої вмикали `appops GET_USAGE_STATS allow` (було default). Далі: I13 (HeatWeek).
- 2026-11-04 · I13 · `HeatWeek` (`ui/risk/V3Components.kt`) на токенах v3.1: контейнер `M3Card` (поле 16dp, проміжки 8dp; у `M3Card` додано параметр `spacing`), клітинки `dataHeat0–5` через `M3Extra.colors.heat(level)`, форма клітинок `TeperaExtraShapes.heatCell` (2dp), пунктир «година ризику» і маркер легенди — `colorScheme.primary` / `signalVivid`, осі й легенда `onSurfaceVariant`. Новий `ui/risk/HeatLevels.kt` (чиста логіка): мапінг `HeatGrid.bucket` 0–4 → рівні 0–5 через стару прозорість бурштину за таблицею 2.2 (0→0, 1→1 [0.15], 2→3 [0.45], 3→4 [0.7], 4→5 [1.0]); рівень 2 цими кошиками не вживається, тому легенда «менше → більше» показує 4 вживані рівні. `HeatLevelsTest` (4 тести) проходять. ВІДХИЛЕННЯ: підписи осей і легенди — `bodySmall` 12sp, а не `labelSmall` 11sp зі специфікації 3.8, бо рішення проєкту CH-10 «змістовний текст ≥ 12sp» — потребує підтвердження власника. Поведінка зміниться: пороги кошиків і дані не чіпались, змінились лише кольори (раніше — альфа бурштину зі `TeperaPalette.heatmapRamp`). Перевірено на пристрої в екрані «Чому це вікно?» (єдине місце HeatWeek у поточному стані застосунку; у ObservingContent/FoundContent не потрапляв), світла й темна теми, без падінь. Далі: I14 (PlanSheet).
- 2026-11-04 · I14a · Оболонка шторки плану. Новий `ui/risk/PlanSheetShell.kt`: `PlanSheetShell` (контейнер `surfaceContainerLow`, форма 28dp зверху `TeperaExtraShapes.sheetTop`, ручка 32×4dp `onSurfaceVariant`@40% з відступом зверху 14dp, поля вмісту 24dp з боків / 8dp зверху / 24dp знизу, проміжки 16dp, затемнення чорний 45% як було, `navigationBarsPadding`), `PlanSheetAppRow` (аватар 40dp `dataSocial`, назва `titleSmall`/`onSurface`, підпис `bodySmall`/`onSurfaceVariant`), `PlanSheetForcedDark`. `PlanOverlayScreen.kt`: і сам лист, і `SoftExitPuzzleScreen` тепер у `PlanSheetShell`; прибрано жорсткі `Color(0xFF1B1B1D)`, `0xFFA65D7C`, форму 32dp, ручку 40×4. ТИМЧАСОВО (до I14b): шторка лишається ТЕМНОЮ незалежно від системної теми (`PlanSheetForcedDark` = M3 dark scheme + DarkTeperaExtraColors + `RedesignScope(m3BridgeColors(true), useOuterM3Scheme = true)`), бо вміст кроків (`SuggestionStep`/`WatchingStep`/`DurationStep`/`PuzzleStep`) має жорстко прописаний білий текст `Color.White`/`0xFFC5DCD9` — у світлій темі став би невидимим. Побічний ефект, очікуваний: кнопки кроків (`TeperaButton`) тепер мають мостикові кольори v3.1 (Primary — м'ятна #83D8AA замість кремової), перетвориться на `M3Button` у I14b. Крок I14b: перевести вміст кроків на токени/`M3Button`, зняти `PlanSheetForcedDark`, щоб шторка брала системну тему; прибрати з `PlanOverlayScreen.kt` невживані імпорти (`RedesignDarkColors`, `RedesignScope` тощо). Перевірено на пристрої: `compileBetaDebugKotlin` чисто; активність `PlanOverlayActivity` запускалась тимчасово з `exported=true` (повернуто `git checkout`, у коміт не потрапило) поверх застосунку (темна системна тема) і робочого столу (світла): ручка, аватар, назва, підпис, затемнення — коректно, без падінь. Не перевірено: `SoftExitPuzzleScreen`, англійська/українська локалі шторки, інші кроки (WatchingStep, DurationStep, PuzzleStep). Позначка `[~]` у чекліст: I14 розбито на a (зроблено) і b. Далі: I14b.
- 2026-11-04 · I14b · Вміст шторки плану. `PlanOverlayScreen.kt`: `SuggestionStep`/`WatchingStep`/`DurationStep` на токенах і `M3Button`; заголовки `headlineSmall`/`onSurface`, допоміжний текст `bodyMedium`/`onSurfaceVariant`, проміжки `TeperaSpacing`. `QuickPuzzleViews.kt`: усі жорсткі `Color.White`/`0xFFC5DCD9`/`0xFF9FE0C8` → `colorScheme.onSurface`/`onSurfaceVariant`/`primary`, заголовок і кнопка «Гаразд, іду» → `headlineSmall`/`M3Button(Filled, M)`. Знято `PlanSheetForcedDark` — шторка тепер бере ПОТОЧНУ тему. `PlanOverlayActivity` (beta): навбар `SystemBarStyle.auto` (темні іконки на світлій шторці), статусбар лишився dark (екран під шторкою затемнений). Кнопки за 3.4: Filled M «Так, роблю це» на всю ширину; нижче Outlined S «Інша ідея» і Text S «Відкрити на 5 хв» (weight 1) — порядок і дії з коду збережено; у специфікації Outlined/Text для «Відкрити на 5 хв»/«Не сьогодні» (у коді «Інша ідея» ~ «Не сьогодні») розподілені навпаки — перемикається однією зміною типів. WatchingStep: Filled M «Вийти» + Text S на всю ширину (за 3.5); DurationStep: три Tonal S (Tonal M обрізав «30 min» до «30»: у ряду з трьох кнопок ~61dp під текст при полі 24dp) + Text S «Назад». ЗНАЙДЕНО й виправлено на пристрої: обрізання «30 min». НЕ зроблено (поза обсягом / немає даних): картка плану з іконкою (`surfaceContainerHighest`, 16dp) і контекстний рядок «Ти написав це в неділю…» зі специфікації — у коді нема даних про власні плани людини, шторка показує ЗАПРОПОНОВАНУ ідею; внутрішні кнопки задачок (цифри, варіанти відповіді) лишились `TeperaButton`. Не враховано: режими Світанок/За часом — там `MaterialTheme.colorScheme` похідна від старих кольорів (шторка може виглядати інакше до I25l); `SystemBarStyle.auto` слідує за системною темою пристрою, а не за вибором у Налаштуваннях застосунку (при примусовій темі застосунку, відмінній від системної, іконки навбару можуть не збігатись). Перевірено на пристрої (тимчасовий `exported=true`, повернуто `git checkout`): початковий крок і вибір тривалості у світлій і темній темі, задачка (арифметика в темній, судоку в світлій), без падінь. Не перевірено наживо: WatchingStep, Tonal S для тривалостей після виправлення, шрифт 1.3, локаль uk. Побічно: тестові відкриття шторки збільшили лічильники `PlanOverlayStore` (reentry), дані екрана тестувальників на пристрої з'явились із тестів. Далі: I15 (MoreConfirmSheet).
- 2026-11-04 · I15 · `MoreConfirmSheet` = `showConfirm()` у `beta/.../gates/SoftExitController.kt`. ВАЖЛИВО для I15–I17: оверлей м'якого виходу — НЕ Compose, а звичайні `View` (`LinearLayout`/`TextView`/`GradientDrawable`) у вікнах `TYPE_ACCESSIBILITY_OVERLAY` сервісу доступності; `ComposeView` там нема (перевірка з I16 про `LocalTeperaColors` не потрібна). Тому нова `beta/.../gates/SoftExitStyle.kt` будує View за токенами v3.1: кольори з `TeperaLightColors`/`TeperaDarkColors` (`ColorScheme.toArgb()`, джерело `M3Palette`), тема як у `TeperaTheme` (`ThemeStore` Світла/Темна, інакше системна), шрифт Golos Text із `R.font.golos_text` + вісь `wght` через `Paint.fontVariationSettings` (API 26+), `text()` з розміром/інтервалом рядка/літер за шкалою M3, `headline()`/`body()`, `button(type FILLED/TONAL/OUTLINED/TEXT, size S/M)` за 3.1 (S 40dp, M 56dp, Text — 48dp без фону для цілі дотику, форма full), `confirmSheet()` за 3.5 (surfaceContainerLow, 28dp зверху, ручка 32×4 @40%, поля 24/8/24, `Filled M` «Вийти» + `Text S` «Так, ще 5 хв» на всю ширину). `showConfirm()` викликає `confirmSheet()`; рядки й логіка (`leave()`/`extend()`) не змінені; прибрано `0xFF1B1B1D`, `0xFFC5DCD9`, радіус 32dp. Плашка (`showPill`, I16), тост (`showToast`, I17) і старі `label()`/`button()` контролера НЕ чіпались — лишаються на старих кольорах до своїх ітерацій; `SoftExitStyle.button()` уже має Filled S і Outlined S для I16. Відомі обмеження: S-кнопки Filled/Outlined мають висоту 40dp без розширення цілі дотику до 48dp (у `View` немає аналога `minimumInteractiveComponentSize`; для Text — 48dp); шторка не враховує інсет навбару (як і раніше). Перевірено на пристрої ТИМЧАСОВОЮ активністю-обгорткою (видалена, маніфест повернуто `git checkout`): лист у світлій і темній темі рендериться правильно, без падінь. НЕ перевірено: справжній показ із сервісом доступності — власний сервіс Tepera на тестовому пристрої не ввімкнений (там три сторонніх), вмикати не стали (довелось би перезаписувати `enabled_accessibility_services` користувача); також сам потік «Ще 5 → Ще 5 → підтвердження», локаль uk, шрифт 1.3, Android 8 (`fontVariationSettings`). Далі: I16 (ExitPill).
