# Open Source Client for NextDNS (development repository)

An **independent, unofficial** native Android client for managing a user's own
NextDNS account through the [documented NextDNS API](https://nextdns.github.io/api/).

> **Development status:** The application is not ready for public Google Play
> release. The name is **Open Source Client for NextDNS**. Five-language UI,
> original network-node artwork and draft store/legal documents are prepared;
> final signed-package and real-device release evidence remains open. This repository
> is not affiliated with or endorsed by NextDNS.

## Features in development

- Manage NextDNS profiles and configuration.
- Review security, privacy and parental-control settings.
- Manage allowlists and denylists.
- View DNS logs and analytics made available by a user's NextDNS account.
- Opt-in, privacy-conscious notifications.
- Adaptive Android user interface built with Kotlin and Jetpack Compose.

The project is an **account-management client**, not a replacement DNS resolver,
VPN service, or NextDNS subscription. The user supplies the NextDNS API key needed for a permitted account connection
and must comply with NextDNS's service terms and applicable law. This app does not
add a separate requirement that the connected account be owned by the device user.
Service availability and supported API operations depend on NextDNS.


## Independent service responsibilities

NextDNS independently operates its DNS service, remote infrastructure, accounts,
subscriptions, API and server-side data handling. This client does not control
those services and is not sponsored, endorsed or officially supported by NextDNS.
Client code, on-device processing and API requests are analysed separately from
NextDNS-operated services under applicable law. The draft does not add an SLA,
support, indemnity or service-continuity undertaking, or claim blanket immunity.

Our [independent-client and Google Play disclosure plan](docs/INDEPENDENT_CLIENT_AND_GOOGLE_PLAY_DISCLOSURE_PLAN.md)
tracks required product wording, privacy declarations, Google/API integrations,
Play review access, and checks that must pass before publication.

## Privacy architecture

The intended network design is **Android device ↔ NextDNS API** using the
account owner's API key. The project does not operate a separate backend for
collecting users' DNS histories or API keys. Its Android Keystore encryption,
permissions, backup exclusions, and network behavior are subject to final
release validation; do not infer a guarantee from the architectural intent.

Do not put real API keys or personal DNS query logs in public issues, builds,
CI logs, screenshots, or sample data.

## License

The project's **original code and project-authored documentation** are licensed
under the [Apache License 2.0](LICENSE). See [NOTICE](NOTICE) for copyright
and third-party trademark information.

Third-party dependencies keep their own license terms. The
[source and dependency license inventory](docs/SOURCE_AND_LICENSE_PROVENANCE.md)
records the pending final runtime artifact/license verification.

The name, brand, logos, service, API and intellectual property of NextDNS are
not licensed to this project by the Apache-2.0 license. The project-authored
network-node artwork and its provenance are recorded in
[the asset review](docs/BRAND_ASSET_PROVENANCE.md). Final launcher/store presentation
remains part of the release review.

## Release process

1. Phase 8: legal, privacy, provenance and third-party license review.
2. Phase 9: naming, five-language UI, original icon and verified offline legal
   drafts are followed by release dependency/package evidence and final review.
3. **Mandatory BrowserStack real-device tests** before Google Play public
   release; see [the release gate](docs/PHASE9_BROWSERSTACK_RELEASE_GATE.md).
4. Google Play internal/closed testing and prelaunch evidence followed by a release decision.
5. Phase 10: API compatibility, dependency and release maintenance after publication.

The [master phase record](docs/PROJECT_PHASE_STATUS.md) preserves the scope of
Phases 2–10; the [literature review](docs/legal/LITERATURE_REVIEW_TR_EU_2026-10-09_DRAFT.md)
and legal texts remain **DRAFT**. Naming completion does not complete Phase 9.

The project is under active development. Do not consider a draft PR or a
successful unit-test run a public release approval.

The [10 October review](docs/CODE_QUALITY_REVIEW_TR_2026-10-10.md) identified
10 conditional defects (3 P1, 7 P2). The [fix report](docs/CODE_QUALITY_FIXES_TR_2026-10-10.md)
records corrections and 37 required regressions within 104 passing JVM tests.
The [defect ledger](docs/HATA_DEFTERI_TR.md) marks them as code/JVM verified
and awaiting device acceptance. BrowserStack, legal and publication gates remain open.

## Source and feedback

Repository: https://github.com/illetyus/open-source-client-for-nextdns

Security, policy and API issues should be reported without disclosing live
API credentials, profile identifiers, or DNS logs.
