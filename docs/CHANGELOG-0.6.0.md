# Tepera 0.6.0 (versionCode 9) — збірка для тестування

Файл: `app/build/outputs/bundle/release/app-release.aab` (підписаний release-ключем).

## Що нового (для тестувальників, UA)
- **Орієнтир часу — новий вигляд.** Степпер «− 2 год +» з чіпом значення (і в Налаштуваннях → Відстеження, і під час онбордингу). На шкалі «Твій день» — жовта підкладка-орієнтир.
- **Шкала «Твій день» перероблена.** Білий контейнер із рамкою, сегмент «Офлайн» (час поза телефоном) суцільним зеленим, незайнята частина дня — бежева зі штрихуванням.
- **Таб-бари й вибір:** сіра обводка (чутливість пауз, мова, перемикач статистики, ворота).
- **Навбар:** матове скло — розмиття фону за навбаром; навбар трохи піднято.
- **Статистика:** вкладка «День» показує вчорашню добу; патерн доби реагує на період (день / тиждень / місяць); «Патерн екрану вчора» — прямокутники із заокругленням 8.
- **Щоденник:** кнопка «+» зелена (#006944), опущена нижче.
- **Пошук:** поле білого кольору (Виключені застосунки, Ворота).
- **Вікно сну:** поля «Початок» / «Кінець» у стилі поля «Назва».
- **Відступи між картками** на всіх екранах (крім Home) зведено до 6dp.
- **Оформлення:** брендовий зелений #006944 скрізь; палітра очищена від дублів і мертвого коду.

## What's new (for testers, EN)
- **Reference time — new look.** A stepper "− 2 h +" with a value chip (both in Settings → Tracking and during onboarding). On the "Your day" bar — a yellow reference highlight.
- **Redesigned "Your day" bar.** White container with a border; the "Offline" segment (time away from the phone) is solid green; the unused part of the day is beige with hatching.
- **Tabs and selectors:** light gray outline (pause sensitivity, language, statistics switch, gates).
- **Navigation bar:** frosted glass — the background blurs behind it; the bar was raised slightly.
- **Statistics:** the "Day" tab shows yesterday; the daily pattern follows the period (day / week / month); "Yesterday's screen pattern" tiles have 8dp corners.
- **Diary:** the "+" button is green (#006944) and sits lower.
- **Search:** white search field (Excluded apps, Gates).
- **Sleep window:** "Start" / "End" fields styled like the "Name" field.
- **Card spacing** on all screens except Home is now 6dp.
- **Visual cleanup:** brand green #006944 everywhere; duplicate and unused colors removed.

## Відомі обмеження
- Розбіжність Online-часу (число і смуга на Home) на окремих пристроях (Motorola G84, Android 15) ще не виправлена: число може бути завищеним через відкриті сесії застосунків без події завершення.
- Вибір теми (світла / темна / системна) приховано в цій збірці; застосунок світлий.
- Екран паузи воріт на скріншотах Play лишився з попередньої версії.
