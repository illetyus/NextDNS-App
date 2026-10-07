# Security Threat Model

This document describes the security assumptions and mitigations for the unofficial
NextDNS Android client.

## Protected assets

- NextDNS API key.
- Profile configuration, allowlist, denylist and parental-control state.
- DNS query history and device metadata.
- Profile identifiers and account metadata.

## Trust boundaries

1. **Android app process** — trusted while the device and OS are not compromised.
2. **Android Keystore** — stores the non-exportable AES key used to protect the API key.
3. **App-private storage** — may hold encrypted credentials and cached configuration.
4. **NextDNS HTTPS endpoints** — authoritative source for remote configuration and logs.
5. **Public log-download URL** — used only after NextDNS returns it; the API key is not
   forwarded to this host.
6. **Clipboard / screen capture** — treated as disclosure surfaces and minimized.

## Implemented controls

### Credential at rest

- The NextDNS API key is encrypted with AES-256-GCM.
- The AES key is generated and retained by Android Keystore.
- Existing plaintext `saved_api_key` values are migrated on first read and removed.
- If secure credential storage fails, login is not reported as successful.
- The sensitive SharedPreferences file is excluded from cloud backup and device transfer.

### Network and logs

- Cleartext network traffic is disabled at manifest level.
- Production HTTP logging is disabled.
- `X-Api-Key` is registered as a redacted header for debug logging.
- Repository exception logging is debug-only.
- Log export requests a public URL with `redirect=0`, then downloads it without the API key.

### DNS history

- DNS query history is memory-only.
- Legacy `saved_logs_*` disk caches are deleted during preferences initialization.
- Query history therefore does not survive a normal process/app restart.

### Credential disclosure UI

- The API key is masked by default.
- Revealed API keys automatically hide again after 10 seconds.
- Screen capture is blocked while the API key is revealed.
- Clipboard content is marked sensitive.
- The clipboard is cleared after 30 seconds if it still contains the copied API key.

### Dependency and third-party network minimization

Unused Firebase AI, Firebase App Check, Google Services, Secrets Gradle, Room, Coil,
Credential Manager, Google ID, camera, location and permission-library entries are removed.

Remote favicon and JavaScript chart requests are removed so DNS/domain analytics are not
sent to icon services or loaded through a JavaScript-enabled WebView.

## Residual risks

- A rooted or otherwise fully compromised device may inspect process memory or hook app calls.
- A user can deliberately disclose an API key to another app or person.
- Android Keystore protects key material but cannot make a compromised runtime trustworthy.
- NextDNS API availability and undocumented endpoints remain external dependencies.
- A public log-download URL should be treated as temporarily sensitive until it expires.
- Screenshots taken before the secure-display flag is enabled are outside the app's control.

## Security invariants

- Never persist a plaintext API key.
- Never include the API key in logs, analytics or crash reporting.
- Never forward the API key to the public log-download host.
- Never persist DNS query history unless a future feature explicitly requires it and receives
  a separate privacy/security review.
- Server mutations are successful only after NextDNS accepts them and authoritative state can
  be re-read.
