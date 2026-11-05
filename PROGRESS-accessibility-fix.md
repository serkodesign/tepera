# Прогрес: Accessibility fix (1.1, versionCode 32)

- [x] Крок 0. Аудит (AUDIT-accessibility.md)
- [x] Крок 1. Назва й опис служби: рядки UA/EN за брифом, `event.className` прибрано з логу; конфіг XML перевірено, без змін. Компіляція beta і юніт-тести проходять.
- [x] Крок 2. `util/A11yConsentStore.kt` (isAccepted/accept/listen) + `A11yFeatureState` (compute/current), тест `A11yFeatureStateTest`. Компіляція beta і юніт-тести проходять.
- [x] Крок 3. `ui/gates/AccessibilityDisclosureScreen.kt` (повноекранний, M3Background + M3Button, BackHandler = «Не зараз»), рядки UA/EN. Екран ще нікуди не підключено (крок 4). Не перевірено на пристрої.
- [ ] Крок 4. Єдина точка входу (наступний)
