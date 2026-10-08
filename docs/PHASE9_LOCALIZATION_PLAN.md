# Phase 9 — Android localization and multilingual Terms of Use

**Decision date:** 2026-10-08. **Status:** Approved product requirements and execution plan; translations and runtime language switching are **not implemented**.

## Launch languages

| Language | Locale | Android resources |
|---|---|---|
| Turkish | `tr` | `res/values-tr/` |
| English | `en` | `res/values/` (recommended source/fallback locale) |
| German | `de` | `res/values-de/` |
| French | `fr` | `res/values-fr/` |
| Spanish | `es` | `res/values-es/` |

The **user interface and Terms of Use** are to be available in all five languages from the first public release. **Privacy Policy remains English-only under the existing user decision**, pending any additional local-language disclosures necessary under applicable laws and target territories. Do not claim all legal notices are available in all five languages. The Apache License 2.0 file is not translated or modified.

## Existing source-level findings

The `app/src/main/res/values/strings.xml` file currently contains only the app label. `LoginScreen.kt` and other Jetpack Compose UI sources have hard-coded Turkish labels such as the API key prompt, login button, demo mode and messages. Merely creating five resource folders without extracting these strings is insufficient.

## Work packages

1. **Inventory and terminology.** Extract every user-facing string from screens, navigation, error handling, dialogs, accessibility labels, notification channels and user-facing export messages. Document DNS-specific translations consistently. Do not translate NextDNS service names, domain names, API keys, API field names or user-supplied content.
2. **Android resource extraction.** Place the complete source-language string set in `values/strings.xml`; provide `values-tr`, `values-de`, `values-fr`, `values-es` translations. Use `stringResource()`, Android `getString()`, plural resources and positional placeholders; localize dates, times, grouping and numbers appropriately.
3. **Locale selection (UX proposal to confirm in Phase 9).** Follow supported device locale on first launch, fall back to English for unsupported locales; support a user-selectable `System default` plus five languages in Settings. Consider Android 13 per-app languages and backward-compatible language switching. Store language preference independently of account data, preserving it on logout.
4. **Five Terms of Use versions.** Include a narrowly scoped NextDNS-controlled API-change/availability disclaimer in all five languages; explain that external API changes can cause bugs, incompatibilities and temporary or permanent feature unavailability, using the concise approved disclaimer without extra qualifying phrases in the user-facing copy. Maintain a reviewed English drafting master and faithful Turkish, German, French and Spanish translations. Provide a user-readable copy in the **selected app language**, along with all five language choices where practical. Require the already-agreed **initial unchecked acceptance checkbox** before login, demo use or background account requests. Revision identifiers and material-change reacceptance are tied to the legal terms revision, not to the selected language. Switching app language must not silently change the terms, erase acceptance, or falsely force reacceptance when legally identical versions apply.
5. **Translation and legal QA.** Terms translations must preserve legal intent, notices, rights and limitations; they require qualified review for jurisdictions where distributed. Avoid claiming one language overrides locally mandatory consumer/protection rules. Track matching document revision/date and translations for every updated Terms edition. Accessibility and readable offline Terms also need testing.
6. **Privacy Policy.** Publish the English Privacy Policy with clear localized links marked `Privacy Policy (English)`. Legal review of whether privacy notice/local language is required in any target territory is a **release gate**. If needed, arrange compliant translation/additional notices instead of claiming English alone necessarily suffices.
7. **Tests and release.** Automated tests for missing/invalid resource keys, placeholder/plural mismatches, unsupported-locale fallback, saved manual preference, locale change with API session and Terms acceptance preserved, English-only Privacy Policy link, and material Terms change. BrowserStack manual/automated tests across 5 locales, large fonts, rotation, long German/French text, Turkish `I/İ/ı/i`, diacritics, notifications and RTL-compatible structure (without committing RTL as a launch language). English legal document content remains inspectable offline when bundled.
8. **Play listing.** Whether store descriptions/screenshots are localized to all five languages is a separate **Phase 9 store-marketing decision**; do not silently settle it by the UI-language choice. Final app name, icon and brand disclaimer also remain Phase 9 decisions.

## Binding legal-writing style decision — all five languages (2026-10-08)

**User decision:** The **Terms of Use, independent-client statement and NextDNS API-availability/responsibility clause** must be drafted in a **formal, academic and juridically precise register in every supported language**: Turkish (`tr`), English (`en`), German (`de`), French (`fr`) and Spanish (`es`). This decision concerns **legal/contractual and institutional disclosures**, not everyday navigation buttons or error text: application UI must remain natural, clear and accessible.

**Drafting rules:**
1. Treat one reviewed master clause structure as authoritative for drafting; adapt each translation to *locally appropriate legal terminology*, not a literal word-for-word rendering. Avoid colloquial phrasing, marketing hyperbole, ambiguous causal claims or artificial verbosity.
2. Preserve the same scope in all languages: independent/unofficial third-party client; no affiliation/endorsement; NextDNS-controlled DNS infrastructure/accounts/subscriptions/API/server-side processing; potential technical errors, incompatibility or feature unavailability from external API changes; the user-approved concise statement declining responsibility for NextDNS API/service-induced connection errors, compatibility issues and loss of functionality.
3. **Do not insert additional developer undertakings or unsolicited acknowledgments of liability into user-facing notices.** Legal review may separately assess unenforceable/mandatory-law issues without expanding the user-approved public-facing clause.
4. Use consistent section headings, defined terms, obligations, terminology, revision identifiers and dates. Maintain a five-column equivalence matrix mapping clauses by legal effect and highlighting non-equivalent formulations for review.
5. Check each locale's legal drafting conventions with qualified translators/reviewers before release (for example `sorumluluk`, `liability`, `Haftung`, `responsabilité`, `responsabilidad`). No translation is deemed legally equivalent solely because automated translation produces text.
6. Keep documents comprehensible to an ordinary reader; formal academic/juridical style must not sacrifice intelligibility, accessibility or required plain-language privacy disclosures. **Privacy Policy remains English-only** under the current decision; a Turkish KVKK notice or other legally mandatory local notice is separate.
7. Any substantive revision must be propagated to all five Terms versions and tested as one release, with explicit review of whether reacceptance is required.

**Definition of done:** All five legal versions receive a language/legal-style review, clause-level semantic equivalence check and ordinary-user readability check; differences documented and corrected before publication.

## Release acceptance conditions

- [ ] Five launch languages fully translated throughout the app; no prominent mixed-language flows.
- [ ] Terms of Use faithfully localized into those same five languages, accessible before agreement, versioned and reviewed.
- [ ] English Privacy Policy accessible through clearly marked links, with local-law requirements independently checked.
- [ ] Locale change does not bypass consent, leak another account's data or break logout data deletion.
- [ ] Translation coverage, unit/UI tests, BrowserStack real-device checks and Play's final disclosures passed.
- [ ] Final documents include verified developer identity, contact address and public non-login URLs.

**This file documents the plan only. It does not assert that translation files or multilingual legal agreements already exist.**
