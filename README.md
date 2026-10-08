# Open Source Client for NextDNS (development repository)

An **independent, unofficial** native Android client for managing a user's own
NextDNS account through the [documented NextDNS API](https://nextdns.github.io/api/).

> **Development status:** The application is not ready for public Google Play
> release. The selected name is **Open Source Client for NextDNS**. The original
> icon and Play Store description still require Phase 9 review. This repository
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
The app developer nevertheless remains responsible for the app's own code,
security, on-device data handling, API requests and accurate disclosures to the
extent required by applicable law. This distinction is not a blanket waiver of
legal obligations.

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
not licensed to this project by the Apache-2.0 license. Current launcher
artwork requires a separate asset/brand review before any public release.

## Release process

1. Phase 8: legal, privacy, provenance and third-party license review.
2. Phase 9: the selected app name and repository rename are applied; finalize
   the independent icon, store text, signing, distribution and privacy disclosures.
3. **Mandatory BrowserStack real-device tests** before Google Play public
   release; see [the release gate](docs/PHASE9_BROWSERSTACK_RELEASE_GATE.md).
4. Google Play internal/closed testing followed by a release decision.

The project is under active development. Do not consider a draft PR or a
successful unit-test run a public release approval.

## Source and feedback

Repository: https://github.com/illetyus/open-source-client-for-nextdns

Security, policy and API issues should be reported without disclosing live
API credentials, profile identifiers, or DNS logs.
