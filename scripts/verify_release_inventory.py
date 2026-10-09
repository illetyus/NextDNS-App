#!/usr/bin/env python3
"""Validate resolved release evidence and create a CycloneDX dependency SBOM."""
import json
import re
import uuid
from pathlib import Path

root = Path(__file__).resolve().parents[1]
directory = root / 'build/compliance'
report = json.loads((directory / 'release-inventory.json').read_text(encoding='utf8'))
assert report['project'] == ':app'
assert report['configurations'] == ['releaseRuntimeClasspath', 'coreLibraryDesugaring']
components = report['components']
assert len(components) > 20, 'Empty or incomplete Android release dependency evidence'
coordinates = {item['coordinate'] for item in components}
for required in ('com.squareup.okhttp3:okhttp:', 'com.squareup.retrofit2:retrofit:', 'androidx.work:work-runtime:', 'com.android.tools:desugar_jdk_libs:'):
    assert any(coordinate.startswith(required) for coordinate in coordinates), required
for coordinate in coordinates:
    assert not coordinate.startswith(('com.google.firebase:', 'com.google.android.gms:play-services-ads', 'com.android.billingclient:', 'io.sentry:')), coordinate

sbom, missing, notices = [], [], []
for item in components:
    group, name, version = item['coordinate'].split(':')
    purl = f'pkg:maven/{group}/{name}@{version}'
    entry = {'type': 'library', 'bom-ref': purl, 'group': group, 'name': name, 'version': version, 'purl': purl}
    if item['licenses']:
        entry['licenses'] = [{'license': {k:v for k,v in license.items() if v}} for license in item['licenses']]
    else: missing.append(item['coordinate'])
    hashes = []
    for artifact in item['artifacts']:
        assert re.fullmatch(r'[a-f0-9]{64}', artifact['sha256']), artifact
        hashes.append({'alg': 'SHA-256', 'content': artifact['sha256']})
        for notice in artifact['embeddedNotices']:
            notices.append(f"\n=== {item['coordinate']} / {artifact['file']} / {notice['path']} ===\n{notice['text']}\n")
    if hashes: entry['hashes'] = hashes
    sbom.append(entry)

bom = {'bomFormat': 'CycloneDX', 'specVersion': '1.6', 'version': 1,
       'serialNumber': 'urn:uuid:' + str(uuid.uuid4()),
       'metadata': {'component': {'type': 'application', 'name': 'Open Source Client for NextDNS', 'version': report['commit']}},
       'components': sbom}
(directory / 'release-sbom.cdx.json').write_text(json.dumps(bom,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
(directory / 'embedded-third-party-notices.txt').write_text(''.join(notices),encoding='utf8')
(directory / 'missing-pom-licenses.json').write_text(json.dumps(missing,indent=2)+'\n',encoding='utf8')
print(f'Resolved release evidence: {len(components)} modules; {sum(len(x["artifacts"]) for x in components)} hashes; {len(missing)} POM license gaps.')
print('Signed-binary verification and final license/notice approval remain separate.')
