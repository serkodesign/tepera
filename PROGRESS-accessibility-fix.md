# Прогрес: Accessibility fix (1.1, versionCode 32)

- [x] Крок 0. Аудит (AUDIT-accessibility.md)
- [x] Крок 1. Назва й опис служби: рядки UA/EN за брифом, `event.className` прибрано з логу; конфіг XML перевірено, без змін. Компіляція beta і юніт-тести проходять.
- [x] Крок 2. `util/A11yConsentStore.kt` (isAccepted/accept/listen) + `A11yFeatureState` (compute/current), тест `A11yFeatureStateTest`. Компіляція beta і юніт-тести проходять.
- [x] Крок 3. `ui/gates/AccessibilityDisclosureScreen.kt` (повноекранний, M3Background + M3Button, BackHandler = «Не зараз»), рядки UA/EN. Екран ще нікуди не підключено (крок 4). Не перевірено на пристрої.
- [x] Крок 4. `rememberAccessibilityRequest` (ui/gates/AccessibilityGateUi.kt) — єдина точка входу; GatesScreen, OnboardingScreen, V3ModeToggle переведено, `AccessibilityDisclosureDialog` і `a11y_disclosure_body` видалено. Екран розкриття показується повноекранним `Dialog`. Компіляція beta/standard і тести проходять; на пристрої не перевірено.
- [x] Крок 5. Служба: кеш згоди (`consentAccepted`) + слухач, `onAccessibilityEvent` і слухач pending soft-exit одразу виходять без згоди; `ConsentPromptOnLaunch` у MainActivity показує розкриття раз за запуск, якщо службу ввімкнено без згоди. Компіляція і тести проходять; на пристрої не перевірено.
- [x] Крок 6. `util/ScreenPackageRule.kt` (пакет → END_SESSION/IGNORE/NORMAL) + тест; служба для налаштувань (`com.android.settings`, `.intelligence` на S23), Google Play, інсталятора, дозволів викликає `softExit.endNow()`; `MainActivity.onResume` завершує сесію через `SoftExitSessionBridge` (крім паузи воріт). SystemUI і клавіатури сесію не завершують. Компіляція і тести проходять. НЕ перевірено на S23: малювання затемнення поверх шторки сповіщень.
- [x] Крок 7. Стан «вимкнено» тепер = немає згоди АБО служби (`A11yFeatureState`): картка «Затримка при будь-якому відкритті» і перемикач «Підказка поверх стрічки» (checked = увімкнено && активно) + кнопка «Увімкнути доступ» через `requestAccessibilityFeature`; стан перечитується в `LifecycleResumeEffect`. Решту застосунку не чіпали. Компіляція і тести проходять; на пристрої не перевірено.
- [ ] Крок 8. Версія і збірка (наступний)
