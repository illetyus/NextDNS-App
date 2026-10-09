"""Compare every ZIP payload entry; ignore only recognized signing records."""
import hashlib
import json
import re
import zipfile

def entries(path):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        assert len(names) == len(set(names)), 'Duplicate ZIP names'
        return {name:hashlib.sha256(archive.read(name)).hexdigest() for name in sorted(names)
                if not name.endswith('/') and not re.fullmatch(r'META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|DSA|EC))', name, re.I)}

def digest(path):
    return hashlib.sha256(json.dumps(entries(path),sort_keys=True,separators=(',',':')).encode()).hexdigest()
