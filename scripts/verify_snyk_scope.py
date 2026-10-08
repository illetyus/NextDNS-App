#!/usr/bin/env python3
"""Reject empty or wrong-scope Snyk scans even if the CLI returned success."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
result = json.loads((root/'build/compliance/snyk-app-release.json').read_text(encoding='utf8'))
reports = result if isinstance(result,list) else [result]
assert len(reports) == 1, 'Expected one Android app release scan'
report = reports[0]
assert report.get('packageManager') == 'gradle', report.get('packageManager')
assert report.get('dependencyCount',0) > 20, 'No complete Android app graph was scanned'
assert report.get('projectName') == 'open-source-client-for-nextdns-app-release', report.get('projectName')
assert report.get('ok') is True, 'Snyk did not approve the high-severity scan'
assert not report.get('vulnerabilities'), 'Unresolved high/critical dependency findings'
print('Snyk app release graph verified:', report['dependencyCount'], 'dependencies')
