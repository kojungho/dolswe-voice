#!/usr/bin/env python3
"""Download pinned public dependencies. Python 3.12+, no credentials needed."""
import hashlib
import json
import shutil
import sys
import tarfile
import urllib.request
import zipfile
from pathlib import Path

source = Path(__file__).resolve().parent
root = Path(sys.argv[1]).expanduser().resolve() if len(sys.argv) == 2 else None
if root is None:
    raise SystemExit('Usage: python3 prepare-deps.py /path/to/work')
config = json.loads((source / 'dependencies.json').read_text())
deps, assets = root / 'deps', root / 'assets'
models = assets / 'models'
for folder in (deps, models, assets / 'licenses'):
    folder.mkdir(parents=True, exist_ok=True)

def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def download(url, path, expected=None):
    if path.exists() and expected and digest(path) == expected:
        return
    partial = path.with_name(path.name + '.download')
    print('Downloading', path.name, flush=True)
    request = urllib.request.Request(url, headers={'User-Agent': 'DolsweVoice-build/1.0'})
    with urllib.request.urlopen(request, timeout=120) as incoming, partial.open('wb') as outgoing:
        shutil.copyfileobj(incoming, outgoing)
    if expected and digest(partial) != expected:
        partial.unlink()
        raise SystemExit('Checksum mismatch: ' + path.name)
    partial.replace(path)

for name, spec in config['files'].items():
    download(spec['url'], deps / name, spec['sha256'])
with zipfile.ZipFile(deps / 'sherpa.aar') as archive:
    (deps / 'sherpa.jar').write_bytes(archive.read('classes.jar'))
    for name in archive.namelist():
        if name.startswith('jni/arm64-v8a/') and name.endswith('.so'):
            target = deps / 'native' / 'jni' / 'arm64-v8a' / Path(name).name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(name))
shutil.copyfile(deps / 'silero_vad.onnx', models / 'silero_vad.onnx')
model = config['model']
if not all((models / name).exists() and digest(models / name) == sha for name, sha in model['files'].items()):
    packed = deps / 'sensevoice-2024.tar.bz2'
    download(model['url'], packed)
    with tarfile.open(packed, 'r:bz2') as archive:
        for name, sha in model['files'].items():
            members = [m for m in archive.getmembers() if m.isfile() and Path(m.name).name == name]
            if len(members) != 1:
                raise SystemExit('Missing or ambiguous model file: ' + name)
            with archive.extractfile(members[0]) as incoming, (models / name).open('wb') as outgoing:
                shutil.copyfileobj(incoming, outgoing)
            if digest(models / name) != sha:
                raise SystemExit('Model checksum mismatch: ' + name)
(assets / 'map').mkdir(exist_ok=True)
for name, spec in config.get('map', {}).items():
    download(spec['url'], assets / 'map' / name, spec['sha256'])
for path in (source / 'licenses').glob('*'):
    shutil.copyfile(path, assets / 'licenses' / path.name)
print('Ready. VOICE_DEPS_DIR=' + str(deps))
print('VOICE_ASSETS_DIR=' + str(assets))
