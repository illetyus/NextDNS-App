"""Render or check an offline license bundle against the exact resolved inputs."""
import argparse
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--inventory', type=Path, default=root/'build/compliance/release-inventory.json')
parser.add_argument('--check', action='store_true')
args = parser.parse_args()
inventory = json.loads(args.inventory.read_text(encoding='utf8'))
assets = root/'app/src/main/assets/licenses'
upstream = json.loads((assets/'upstream-sources.json').read_text(encoding='utf8'))
parts = ["Open Source Client for NextDNS — source and dependency notices\n",
         (root/'NOTICE').read_text(encoding='utf8'),
         "\n=== Original project source: Apache-2.0 ===\n", (root/'LICENSE').read_text(encoding='utf8'),
         "\n=== Resolved build inputs ===\n",
         "This inventory includes platform/BOM metadata and desugaring inputs. It is not a count of libraries in the APK. Upstream components retain their licenses.\n"]
index = []
for component in inventory['components']:
    coordinate = component['coordinate']
    licenses = component['licenses']
    if not licenses:
        assert coordinate == 'com.google.guava:listenablefuture:1.0', coordinate
        licenses = [{'name':'Apache-2.0 (inherited from guava-parent:26.0-android)',
                     'url':'https://repo.maven.apache.org/maven2/com/google/guava/guava-parent/26.0-android/guava-parent-26.0-android.pom'}]
    parts.append(f"\n{coordinate}\n")
    for license in licenses:
        parts.append(f"  {license['name']}\n  {license.get('url','')}\n")
    artifacts = []
    for artifact in component['artifacts']:
        artifacts.append({key:artifact[key] for key in ('configuration','file','sha256')})
        for notice in artifact['embeddedNotices']:
            parts.append(f"\n=== {coordinate} / {artifact['file']} / {notice['path']} ===\n{notice['text']}\n")
    index.append({'coordinate':coordinate,'licenses':licenses,'artifacts':artifacts})
for source in upstream:
    data = (assets/source['file']).read_bytes()
    assert hashlib.sha256(data).hexdigest() == source['sha256'], source['file']
    parts.append(f"\n=== Upstream license text: {source['file']} ===\nSource: {source['url']}\n{data.decode('utf8')}\n")
parts.append("\n=== Desugared library source ===\nThe upstream source revision preparing version 2.1.5 is 73170c345e6a762fc6a1f0301bb15218850023ef.\n"
             "https://github.com/google/desugar_jdk_libs/tree/73170c345e6a762fc6a1f0301bb15218850023ef\n"
             "An unchanged upstream source archive accompanies the local review distribution. This is an upstream release-preparation revision, not a reproducibility attestation for Google's Maven binary.\n")
outputs = {'THIRD_PARTY_NOTICES.txt':''.join(parts).replace('\r\n','\n').encode('utf8'),
           'components.json':(json.dumps(index,ensure_ascii=False,indent=2)+'\n').encode('utf8')}
for name,data in outputs.items():
    path = assets/name
    if args.check:
        assert path.read_bytes().replace(b'\r\n',b'\n') == data, f'Bundled notices are stale: {name}'
    else:
        path.write_bytes(data)
print(f"PASS: {'checked' if args.check else 'prepared'} full notices for {len(index)} resolved modules, including {sum(len(c['artifacts']) for c in index)} artifact hashes")
