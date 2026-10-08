#!/usr/bin/env bash
# Source-level dependency gate, not a complete OSS legal review.
# Requires JDK/Android SDK and the Gradle wrapper used by the CI build.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
mkdir -p build/compliance
report="build/compliance/releaseRuntimeClasspath.txt"

./gradlew :app:dependencies --configuration releaseRuntimeClasspath --no-daemon > "$report"

if ! grep -Fq 'releaseRuntimeClasspath' "$report"; then
  echo "Dependency report did not contain releaseRuntimeClasspath" >&2
  exit 1
fi

if grep -Eq -- '--- .* FAILED|Could not resolve|UNRESOLVED' "$report"; then
  echo "Unresolved release dependency detected (review build/compliance/releaseRuntimeClasspath.txt)." >&2
  exit 1
fi

# The first public release is free of ads, in-app purchasing and app-owner
# telemetry. This guard intentionally checks *group coordinates* only;
# it does not prove absence of tracking, network traffic or shaded binaries.
if grep -En -- 'com\.google\.firebase:|com\.google\.android\.gms:play-services-ads|com\.android\.billingclient:|io\.sentry:' "$report"; then
  echo "An unapproved analytics, ads, billing or crash-telemetry SDK is present in releaseRuntimeClasspath." >&2
  exit 1
fi

echo "PASS: release dependency graph resolved; no named disallowed SDK groups found."
echo "Inspect $report for the complete graph and verify each artifact's license separately."
