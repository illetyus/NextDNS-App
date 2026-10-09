# Phase 9.2 — five-language interface

The interface resources cover English, Turkish, German, French and Spanish. English is the fallback. The fixed product title is not translated. The user can select the system language or a specific language before accepting the Terms and from Settings.

Android 13 and later use LocaleManager and the system per-app language selector. Android 7–12 retain the choice in separate local UI preferences and recreate the Activity. A saved older-device choice is migrated once on upgrading to Android 13; subsequent system choices are authoritative. Language and appearance preferences are separate from account data. The Terms revision is independent of the locale.

Navigation titles resolve resources when displayed. Notifications, repository errors, background text, number formatting, relative-time plurals and the offline legal reader use the same selected locale. Terms remain editorial drafts in five languages; Privacy Policy remains English. Server-supplied profile names, domains, catalogue/provider names and protocol identifiers are preserved.

Existing retention, storage-location and analytics-filter codec values remain stable. UiLabels translates their displayed labels while callbacks use the original values. Neither API field names/endpoints nor Kotlin/package identities change.

Validation: scripts/verify_localization.py requires all five resource sets, compatible format arguments and resource references, and rejects literal Compose labels outside an explicit technical allowlist. Robolectric checks cover Android 12 and earlier, Android 13 and later, unsupported-locale fallback, platform/in-app synchronization, unchanged API retention values and acceptance persistence. Compose records all five home screens at 320 dp and 1.5 font scale, plus five deterministic connection cards with the actual 16 dp outer margins. Connection titles must fit and wrap at word boundaries. CI retains twelve PNGs and actual JUnit/JaCoCo results; final artifact inspection must pass before merging.

These tests do not certify professional linguistic/legal equivalence or replace the BrowserStack device gate. Long instructional text, OEM per-app language behavior and actual system settings remain part of the release-device matrix.
