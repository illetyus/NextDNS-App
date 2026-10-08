#!/usr/bin/env python3
"""Validate preparation files; publication requires real completed evidence."""
import json
import re
import subprocess
import sys
from pathlib import Path

root=Path(__file__).resolve().parents[1]
readiness=json.loads((root/'docs/release/readiness.json').read_text(encoding='utf8'))
for language in ('en-US','tr-TR','de-DE','fr-FR','es-ES'):
 directory=root/'fastlane/metadata/android'/language
 title=(directory/'title.txt').read_text(encoding='utf8').strip()
 short=(directory/'short_description.txt').read_text(encoding='utf8').strip()
 full=(directory/'full_description.txt').read_text(encoding='utf8').strip()
 assert title=='Open Source Client for NextDNS' and len(title)<=30
 assert 0<len(short)<=80 and 0<len(full)<=4000
 assert 'https://github.com/illetyus/open-source-client-for-nextdns' in full
assert readiness['territories']==['TR','EU']
assert readiness['free'] and not readiness['ads'] and not readiness['inAppPurchases']
cases=readiness['browserstack']['cases']
assert len(cases)==22 and {c['id'] for c in cases}=={f'B{x:02}' for x in range(1,23)}
assert all(c['status'] in ('NOT_RUN','PASS','FAIL','BLOCKED') for c in cases)
assert all(c['status']!='PASS' or c['evidence'] for c in cases),'PASS requires retained evidence'
if '--draft' in sys.argv:
 print('Five store localizations and B01–B22 ledger structure verified; publication not authorized.')
 sys.exit(0)
subprocess.run([sys.executable,str(root/'scripts/verify_legal_assets.py'),'--release'],check=True)
assert readiness['status']=='APPROVED' and readiness['publisherReview'],'Final publisher review is pending'
candidate=readiness['releaseCandidate']
assert candidate and re.fullmatch('[a-f0-9]{40}',candidate['commit'])
for key in ('apkSha256','aabSha256','instrumentedApkSha256','testSuiteSha256'):
 assert re.fullmatch('[a-f0-9]{64}',candidate[key])
assert readiness['productionSigningApproval'] and readiness['licenseApproval']
browserstack=readiness['browserstack']
assert browserstack['appLiveSession'] and browserstack['appAutomateBuild'],'BrowserStack lanes have not both completed'
for lane,apk_key in (('appLiveSession','apkSha256'),('appAutomateBuild','instrumentedApkSha256')):
 evidence=browserstack[lane]
 assert evidence['commit']==candidate['commit'] and evidence['apkSha256']==candidate[apk_key]
 assert evidence['url'].startswith('https://')
assert browserstack['appAutomateBuild']['testSuiteSha256']==candidate['testSuiteSha256']
for case in cases:
 assert case['status']=='PASS' and case['evidence'],f'{case["id"]} has not passed'
 for evidence in case['evidence']:
  assert evidence['commit']==candidate['commit']
  assert evidence['apkSha256'] in (candidate['apkSha256'],candidate['instrumentedApkSha256'])
  assert evidence['url'].startswith('https://') and evidence['device'] and evidence['androidVersion']
equivalence=next(case for case in cases if case['id']=='B14')
assert any(e['apkSha256']==candidate['apkSha256'] for e in equivalence['evidence']),'B14 lacks actual production-signed APK evidence'
for field in ('internalTest','closedTest','prelaunchReport','dataSafetyApproval'):
 assert readiness['play'][field],f'Play evidence pending: {field}'
print('Publication evidence structure is complete; no upload or rollout is performed by this script.')
