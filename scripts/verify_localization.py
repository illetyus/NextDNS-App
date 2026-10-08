#!/usr/bin/env python3
"""Fail on missing translations, changed formats or new literal UI labels."""
import re
import xml.etree.ElementTree as ET
from pathlib import Path

root = Path(__file__).resolve().parents[1]
resources = root / 'app/src/main/res'
catalogs = {}
for language in ('en', 'tr', 'de', 'fr', 'es'):
    folder = 'values' if language == 'en' else 'values-' + language
    tree = ET.parse(resources / folder / 'strings.xml')
    entries = {}
    for element in tree.getroot():
        if element.get('translatable') == 'false': continue
        key = (element.tag, element.get('name'))
        assert key not in entries, f'Duplicate {key} in {language}'
        if element.tag == 'plurals':
            entries[key] = {i.get('quantity'): i.text or '' for i in element}
        else: entries[key] = {'value': element.text or ''}
    catalogs[language] = entries

formats = re.compile(r'%(?:\d+\$)?[,0-9.]*[dsf]')
for language, entries in catalogs.items():
    assert entries.keys() == catalogs['en'].keys(), f'Incomplete locale: {language}'
    for key, variants in entries.items():
        assert variants.keys() == catalogs['en'][key].keys(), (language, key)
        for variant, value in variants.items():
            assert value.strip('" '), (language, key, 'empty')
            assert sorted(formats.findall(value)) == sorted(formats.findall(catalogs['en'][key][variant])), (language, key, 'format mismatch')

string_names = {name for kind, name in catalogs['en'] if kind == 'string'} | {'app_name'}
plural_names = {name for kind, name in catalogs['en'] if kind == 'plurals'}
for path in (root / 'app/src').rglob('*.kt'):
    source = path.read_text(encoding='utf8')
    for name in re.findall(r'R\.string\.(\w+)', source): assert name in string_names, (path, name)
    for name in re.findall(r'R\.plurals\.(\w+)', source): assert name in plural_names, (path, name)

# Human labels supplied directly to Compose must be resources. These are
# technical labels, brand/platform names, animation keys and value formatting.
allowed = {
    'bounceScale', 'pressScale', 'beaconPulse', 'pulseScale', 'pulseAlpha',
    'cardBorderColor', 'settingBgColor', 'settingBorderColor', 'BETA',
    'analytics_loading_alpha', 'tabBg', 'tabBorder', 'NextDnsTabContent',
    'refresh_rotate', 'rotation', 'Web3', 'ENS', 'UD', 'HNS', 'IPFS',
    'Google Chrome / Brave / Edge', 'Mozilla Firefox', '%', ''
}
tokens = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"', re.S)
for path in (root / 'app/src/main/java/com/example/ui').rglob('*.kt'):
    source = path.read_text(encoding='utf8')
    for match in tokens.finditer(source):
        if not match.group().startswith('"'): continue
        value = match.group()[1:-1]
        prefix = source[max(0, match.start()-100):match.start()]
        supplied = re.search(r'(?:text|title|subtitle|contentDescription|placeholder|description|label|buttonText|copyLabel|successMessage)\s*=\s*$', prefix) or re.search(r'(?:Text|showMessage|append)\s*\(\s*$', prefix)
        if not supplied or value in allowed or not value.strip(): continue
        if value.startswith(('ID: ${', '.$', 'https://', '%${')): continue
        raise AssertionError(f'{path.relative_to(root)}: hardcoded label {value!r}')
    assert not re.search(r'ThemeMode\.\w+\s*->\s*"', source), f'{path}: hardcoded theme label'
    assert not re.search(r'if\s*\(checked\)\s*"', source), f'{path}: hardcoded enabled state'

print(f'Localization verified: {len(catalogs["en"])} resources in all five locales; format and UI-label checks passed.')
