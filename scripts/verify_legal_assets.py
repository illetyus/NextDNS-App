#!/usr/bin/env python3
"""Validate draft bundle integrity; --release additionally requires approved metadata."""
import hashlib
import json
import re
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
assets = root / 'app/src/main/assets/legal'
registry = json.loads((assets / 'registry.json').read_text(encoding='utf8'))
assert registry['digest_format'] == 'sha256-utf8-lf'
expected = {f'terms_{language}.txt' for language in ('tr', 'en', 'de', 'fr', 'es')} | {'privacy_en.txt'}
assert {document['path'] for document in registry['documents']} == expected
assert len(registry['documents']) == len(expected)
for document in registry['documents']:
    content = (assets / document['path']).read_text(encoding='utf8')
    assert hashlib.sha256(content.encode('utf8')).hexdigest() == document['sha256'], document['path']
    revision = registry['terms_revision'] if document['path'].startswith('terms_') else registry['privacy_revision']
    assert f'Revision: {revision}' in content, document['path']
    if document['path'].startswith('terms_'):
        assert len(re.findall(r'^### \d+\.', content, re.M)) == 12, document['path']
        assert 'Apache-2.0' in content, document['path']
    if registry['status'] == 'DRAFT': assert 'DRAFT' in content and document['status'] == 'DRAFT'
store = (root / 'app/src/main/java/com/example/data/legal/LegalAcceptanceStore.kt').read_text(encoding='utf8')
assert f'CURRENT_TERMS_REVISION = "{registry["terms_revision"]}"' in store
if '--release' in sys.argv:
    assert registry['status'] == 'APPROVED', 'Legal bundle is still DRAFT'
    for field in ('publisher', 'contact', 'public_policy_url', 'approval_reference'):
        assert registry.get(field), f'Unverified release metadata: {field}'
    assert registry['public_policy_url'].startswith('https://')
    assert all(d['status'] == 'APPROVED' for d in registry['documents'])
print('Legal bundle integrity and revision verified; status:', registry['status'])
