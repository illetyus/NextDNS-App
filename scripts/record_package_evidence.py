#!/usr/bin/env python3
"""Record real CI package hashes, APK manifests and signature status."""
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path

root=Path(__file__).resolve().parents[1]
directory=root/'build/compliance'
directory.mkdir(parents=True,exist_ok=True)
sdk=Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
tools=sorted((sdk/'build-tools').iterdir(),key=lambda p:tuple(int(x) for x in re.findall(r'\d+',p.name)))
tool=next(path for path in reversed(tools) if (path/'aapt2').is_file() and (path/'apksigner').is_file())
packages=[]
for pattern,kind in [('apk/debug/*.apk','debug'),('apk/androidTest/debug/*.apk','androidTest'),('apk/release/*.apk','release'),('bundle/release/*.aab','releaseBundle')]:
 paths=list((root/'app/build/outputs').glob(pattern))
 assert len(paths)==1,(kind,paths)
 package=paths[0]
 entry={'kind':kind,'file':str(package.relative_to(root)).replace('\\','/'),'sha256':hashlib.sha256(package.read_bytes()).hexdigest(),'bytes':package.stat().st_size}
 if package.suffix=='.apk':
  manifest=subprocess.run([str(tool/'aapt2'),'dump','xmltree',str(package),'--file','AndroidManifest.xml'],check=True,capture_output=True,text=True).stdout
  (directory/f'{kind}-manifest.txt').write_text(manifest,encoding='utf8')
  package_id=re.search(r'A: package="([^"]+)"',manifest)
  assert package_id and package_id.group(1)=='com.aistudio.nextdns.mgrqvt'+('.test' if kind=='androidTest' else ''),kind
  def boolean_attribute(name):
   value=re.search(r':'+re.escape(name)+r'\([^)]*\)=(true|false|0x[0-9a-f]+)\b',manifest,re.I)
   if not value: return None
   raw=value.group(1).lower()
   return raw=='true' or (raw.startswith('0x') and int(raw,16)!=0)
  entry['applicationId']=package_id.group(1)
  entry['debuggable']=boolean_attribute('debuggable') or False
  entry['usesCleartextTraffic']=boolean_attribute('usesCleartextTraffic')
  if kind=='release':
   assert not entry['debuggable'],'Release APK is debuggable'
   assert entry['usesCleartextTraffic'] is False,'Release permits cleartext traffic'
   for permission in ('MANAGE_EXTERNAL_STORAGE','READ_SMS','ACCESS_FINE_LOCATION','QUERY_ALL_PACKAGES'):
    assert permission not in manifest,permission
  signature=subprocess.run([str(tool/'apksigner'),'verify','--print-certs',str(package)],capture_output=True,text=True)
  entry['signatureVerified']=signature.returncode==0
  entry['signatureDetails']=signature.stdout.strip() if signature.returncode==0 else 'UNSIGNED_OR_INVALID'
  if kind in ('debug','androidTest'):
   assert entry['signatureVerified'] and entry['debuggable'],kind
 packages.append(entry)
report={'commit':os.environ.get('GITHUB_SHA','local'),'packages':packages,'productionSigningApproved':False}
(directory/'package-evidence.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
print('Recorded four package hashes and verified debug/test signatures; production signing approval remains false.')
