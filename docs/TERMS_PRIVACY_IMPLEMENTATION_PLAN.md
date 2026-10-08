# Faz 8 / Faz 9 — Terms of Use & Privacy Policy implementation plan

**Decision date:** 2026-10-08  
**Owner:** illetyus (single project owner per owner statement).  
**State:** **Approved product requirements / technical IMPLEMENTATION PLAN**, not finished legal documents, not implemented consent UI, and not a legal review.  
**Repository baseline reviewed:** `phase8-compliance` (Oct 8, 2026).  
**Existing release constraints:** First public release is free/ad-free; direct Android-device-to-NextDNS model; Apache License 2.0; final brand/name and public Play listing are Phase 9 decisions.

## A. Six confirmed product decisions

| ID | Confirmed decision | Implementation obligation |
|---|---|---|
| A1 | First launch: user must affirmatively check an acceptance checkbox before continuing. | Checkbox unchecked by default; explicit `I agree to the Terms of Use`; agreement gate shown **before** credential entry, demo mode or pre-existing account refresh. Privacy Policy separately linked/readable and does not itself count as consent to all processing. |
| A2 | Material changes to terms require fresh acceptance; typo/editorial changes do not. | Version separate `terms_revision` / `privacy_revision` and a human-reviewed `material_change` flag; block continued account-management functions until updated terms accepted. Inform of Privacy Policy changes and obtain legally required independent consent if ever needed. |
| A3 | General audience; not specifically directed at children. | Do not assert the service is 18+ or target children. Play Console target audience and content classification reviewed separately in Phase 9. |
| A4 | No application-specific account ownership restriction. Users must obey NextDNS service terms and applicable law. | Do **not** assert users may only use their personally owned account. Terms prohibit unlawful/unauthorized access to the extent required by applicable law/NextDNS terms without inventing an additional ownership restriction. |
| A5 | Logout removes API key, local account/profile caches and their notification tasks; preserves optional display/theme preferences. | Purge `NextDnsPreferences` profile settings and key; clear `NotificationPreferences` all account-scoped digests, timestamps, daily summary baseline, and selected account notification settings; cancel active WorkManager jobs; stop SSE/polling and invalidate in-flight calls before another account signs in. Exported documents written to user-chosen storage remain user-managed; NextDNS remote account and DNS logs **not** deleted by logout. |
| A6 (revised after the language choice) | Five UI and Terms of Use languages: Turkish, English, German, French and Spanish. The Privacy Policy remains English-only for now. | Prepare five legally faithful Terms translations, retain one material revision across translations, and show the selected app language. English-only Privacy must be clearly labeled; determine whether local-language privacy notices are legally necessary before publication in target territories. |

## B. Delivery slices (phased)

### Slice 1 — Legal documents and publication metadata (Phase 8: drafts / Phase 9: final)

1. Prepare a formally structured English drafting master for `Terms of Use` and legally reviewed Turkish, German, French and Spanish translations of the **same revision**, each written in its own language's **academic, formal, legally precise register**, with clause-by-clause semantic equivalence and reader comprehensibility. This style requirement also applies to the independent-client, API-change and responsibility-disclaimer clauses; do not insert unrequested new developer commitments in the user-facing clauses. See [PHASE9_LOCALIZATION_PLAN.md](PHASE9_LOCALIZATION_PLAN.md). All five should cover independent/unofficial client identity, API key and service functionality, proper/legal use and NextDNS terms, service updates/API outages, software AS-IS warranty limits only as permitted by law, data deletion distinctions, relevant contact details, changes and effective date, applicable law/dispute terms after review.
2. Prepare separate English `Privacy Policy` describing: key encryption Android Keystore AES-GCM; what profile/config, DNS logs/IP/device information the app reads; local plaintext profile cache that is inside a backup-excluded preferences file; NextDNS API transfers; NextDNS-provided URL used for log exports; user-created files saved outside the application's sandbox; optional background notification polling; notification settings DataStore; exact retention/deletion behavior; absence of developer-controlled telemetry/ad SDK in launch design, subject to final APK check; data requests and developer privacy contact.
3. Do not place a blanket `I consent to processing` in the Terms checkbox. For processing needing specific consent (if ever present), use a separate optional or required-before-feature disclosure/consent tied to the processing.
4. Complete before publication: **verified developer legal/display name, support & privacy contact, final app/store name, canonical HTTPS URLs, policy effective date, policy change log, NextDNS trademark/IP review, per-territory legal check**. No made-up contacts/URLs or blanket liability waiver.
5. Host the final English Privacy Policy on an **active publicly accessible, non-geofenced, read-only/non-editable HTTPS web page (not PDF)** and link it in-app and in Play Console; host or bundle all five Terms translations with clear language selectors. Consider static hosting (GitHub Pages or other controlled HTTPS pages), do not require GitHub login.
6. Ensure Play Console Data Safety and user-facing notices agree with tested release APK's actual data flows and SDKs.

### Slice 2 — Consent/version gating (Phase 9 implementation, before release)

**Proposed file ownership** (implementation to choose actual packages):
- `data/legal/LegalDocumentRegistry`: bundled offline Terms and Privacy documents, canonical publication URLs, revisions, and material-change flag.
- `data/legal/LegalAcceptanceStore`: local version, time, and optional digest of accepted Terms. Persist atomically via DataStore; **no API key, personal DNS info or account identifiers**.
- `ui/screens/LegalWelcomeScreen`: scrollable, accessible screen and separate `Terms of Use` + `Privacy Policy` view links; unchecked checkbox by default.
- `MainActivity`: state machine `loading -> needs_terms -> login_or_home`.

**Critical existing-code constraint:** `NextDnsRepository` can restore credentials and call the NextDNS API during startup; `NextDnsApp.onCreate` can reconcile background notification work before `MainActivity` shows any consent UI. Rendering the gate on top of existing login is **not enough**. Delay network/account initialization and scheduled network work until the currently required Terms revision is accepted. On an update with material changes, stop existing polling/streams and block scheduled account requests until reacceptance.

**Accept flow:** only when checkbox is checked, store accepted revision/version and time locally, verify the write succeeded, and then begin account initialization. Do not send acceptance analytics to developer servers. If app is closed or acceptance declined, no account requests should start. Document pages work offline using bundled copies; links to public canonical policy URLs remain available.

**Material change flow:** keep explicit `termsRevision`, `privacyRevision`, and `requiresNewTermsAcceptance` per release; compare accepted terms revision on startup; do not force reaccept for mere formatting/typos. Include a brief change summary when reacceptance is required. Material privacy changes may trigger separate legal disclosures/consents, not just Terms acceptance.

### Slice 3 — Logout/data erasure boundary (Phase 9 implementation)

Current code inspection:
- `NextDnsRepository.logout()` already cancels named notification WorkManager tasks, calls `preferences.clear()`, empties many in-memory account flows, and stops log SSE via scoped reset.
- `NotificationPreferences` writes to a separate `notification_settings` DataStore; current logout does **not** clear that DataStore. It can retain account-specific notification digests/timestamps and enabled flags.
- General `ThemePreferences` uses distinct `settings` DataStore and may remain.
- A user-exported DNS log document outside app-private storage cannot necessarily be deleted by logout; tell user this clearly.

Implementation obligations:
1. Add suspending `NotificationPreferences.clearAccountState()` to atomically wipe account-scoped digests/notification settings/baselines (not only disable toggles).
2. Make logout an idempotent, awaited operation: stop streams/polling and cancel scheduled workers; inhibit newly scheduled jobs; securely erase stored API key and profile caches; clear account-related memory, pending fetch callbacks/cursors, and notification DataStore.
3. Guard in-flight network callbacks by generation/token to prevent stale results resurfacing or cross-account leaks; do not rely on UI reset alone.
4. Fail closed: if local deletion fails, show an error and retry; do not imply that cleanup succeeded when it did not. Backups and restore scenarios need tests.
5. Keep general theme preferences and **Terms acceptance revision**: logout does not erase a device-level terms acceptance, but if legal requirements mandate acceptance tied to account context re-evaluate. A future 'Reset app and legal state' is a separate operation.
6. Explain that logout does not remove server data from NextDNS, revoke the NextDNS API key server-side, or delete files exported by the user.

### Slice 4 — Contractual content and UI acceptance tests

Required automated tests:
- Fresh install with no consent: login, demo, auto-login and network polling blocked.
- Checkbox is initially unchecked; Continue disabled; only deliberate acceptance unlocks the app.
- Restart process/offline launch after acceptance: persists; no extra developer-network request.
- Migrating existing installation with stored API key: terms gating occurs **before** auto-login/API request.
- Minor text edit: no forced reaccept; material change: reaccept required and background work paused.
- Privacy-only update: noticeable policy update; separate consent only if legally required.
- Logout with notifications enabled and populated caches: API key removed; cached records and notification DataStore removed; workers cancelled; theme remains.
- Logout then login to a different NextDNS profile: no stale notifications, SSE, DNS log or previous user's cached settings.
- Exported user file remains outside app deletion (accurately disclosed).
- Localized Terms reading/acceptance, user-facing links, HTML rendering and screen-reader accessibility in Turkish, English, German, French and Spanish; label the separate English Privacy Policy clearly. Reject missing/offline URLs by providing bundled offline document content.
- Terms of Use is a product agreement only; Privacy Policy is available without treating its display as independent legal consent.

Manual/device release tests (Phase 9):
- BrowserStack real Android device scenarios: fresh install, upgrade, offline access, orientation, Dark/Light mode, Back button / closing app, privacy links, locale changes, logout + WorkManager/backup/restore and multiple accounts.
- Cross-check final Privacy Policy and Data Safety against observed host list, log-export redirects, and final resolved APK/AAB dependencies.

### Slice 5 — Shipping approval and maintenance (Phase 9 / Faz 10)

Acceptance criteria:
- [ ] English Privacy Policy and five matching Terms of Use language versions completed, peer-reviewed for clarity and legally reviewed in relevant jurisdictions; validated developer/support identity and working URLs.
- [ ] User can read both texts without sign-in or live API key and may decline Terms (no hidden backend requests).
- [ ] Only Terms checkbox governs agreement; distinct privacy consent/disclosure handled independently if applicable.
- [ ] Relevant revisions are stored locally, enforced consistently across process restarts/upgrades and remain offline-capable.
- [ ] Logout fully clears app-owned account/notification records and stops background work without affecting theme; user exports / NextDNS-hosted data limitations explicit.
- [ ] CI automated tests and BrowserStack tests green; no unchecked release/privacy discrepancy.
- [ ] Play privacy URL and Data Safety accurate; NextDNS unofficial nature and app trademark/name resolved.
- [ ] Clear updated Terms and Privacy documents in-app for the app's lifetime; release notes flag material changes.

## C. Legal/policy sources consulted (2026-10-08)

- Google Play User Data / Privacy Policy / Data Safety: https://support.google.com/googleplay/android-developer/answer/10144311?hl=en
- Google Play prominent disclosure and separate consent: https://support.google.com/googleplay/android-developer/answer/11150561?hl=en
- KVKK understandable, plain-language privacy disclosures: https://www.kvkk.gov.tr/Icerik/6765/AYDINLATMA-YUKUMLULUGUNUN-YERINE-GETIRILMESI-HAKKINDA-KAMUOYU-DUYURUSU
- KVKK notice and explicit consent separation (2026/347): https://www.kvkk.gov.tr/Icerik/8710/veri-sorumlulari-tarafindan-acik-riza-ve-aydinlatma-metinlerinin-ayri-ayri-duzenlenmesi-gerektigi-hakkinda-kisisel-verileri-koruma-kurulunun-18-02-2026-tarihli-ve-2026-347-sayili-ilke-kararina-iliskin-kamuoyu-duyurusu

## D. Status / no premature implementation claim

This commit **documents** the approved design and required changes. It does **not** add a consent screen, publish final terms, finalize the developer identity, change the Android logout implementation, or constitute a legal compliance certification.

### Localization addendum (2026-10-08)

After the original six questionnaire answers, the developer selected **five launch UI languages** and authorized **Terms of Use in those same languages**. This amends only the language portion of A6: the Privacy Policy remains English-only unless changed by a later decision or a legally required local disclosure. See [PHASE9_LOCALIZATION_PLAN.md](PHASE9_LOCALIZATION_PLAN.md). The five Terms translations must be aligned to the same reviewed version; language switching alone must not trigger a new acceptance for unchanged material terms.
