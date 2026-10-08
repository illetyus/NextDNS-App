#!/usr/bin/env bash
# Fail-closed legal-document pre-publication check.
# Run this explicitly before upload to Play, not in ordinary draft PR CI.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

assets="app/src/main/assets/legal"
missing=0
for language in tr en de fr es; do
  file="$assets/terms_$language.txt"
  if [[ ! -s "$file" ]]; then
    printf 'BLOCK: Missing Terms for %s\n' "$language" >&2
    missing=1
    continue
  fi
  if grep -Eiq 'DRAFT|NOT FOR RELEASE|NOT FOR PUBLICATION|\[FINAL APPLICATION NAME\]|\[UYGULAMANIN NİHAİ ADI\]|\[VERIFIED|\[DATE' "$file"; then
    printf 'BLOCK: Unapproved legal wording in %s\n' "$file" >&2
    missing=1
  fi
done
privacy="$assets/privacy_en.txt"
if [[ ! -s "$privacy" ]] || grep -Eiq 'DRAFT|NOT FOR RELEASE|NOT FOR PUBLICATION|\[FINAL APP NAME\]|\[VERIFIED|\[WORKING|\[PUBLIC|\[DATE' "$privacy"; then
  printf 'BLOCK: English Privacy Policy missing or not approved\n' >&2
  missing=1
fi

revision="app/src/main/java/com/example/data/legal/LegalAcceptanceStore.kt"
if grep -Fq 'DRAFT' "$revision"; then
  printf 'BLOCK: LegalAcceptanceStore still references a provisional Terms revision\n' >&2
  missing=1
fi

if [[ "$missing" != "0" ]]; then
  printf 'The application must NOT be publicly distributed with draft legal documents.\n' >&2
  exit 1
fi

printf 'Document placeholders and draft markers not detected. This does NOT replace human legal review or Play Data Safety checks.\n'
