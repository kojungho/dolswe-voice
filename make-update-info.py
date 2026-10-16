#!/usr/bin/env python3
"""Emit update.json for a signed APK; upload it with each GitHub release."""
from pathlib import Path
import hashlib,json,sys,xml.etree.ElementTree as ET
if len(sys.argv)!=3:raise SystemExit('Usage: python3 make-update-info.py signed.apk output/update.json')
apk=Path(sys.argv[1]);dest=Path(sys.argv[2]);ns='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(Path(__file__).resolve().parent/'android/AndroidManifest.xml').getroot()
version=manifest.get(ns+'versionName');code=int(manifest.get(ns+'versionCode'))
with apk.open('rb') as stream:sha=hashlib.file_digest(stream,'sha256').hexdigest()
metadata={'versionCode':code,'versionName':version,'apkUrl':'https://github.com/kojungho/dolswe-voice/releases/download/v'+version+'/dolswe-voice.apk','size':apk.stat().st_size,'sha256':sha}
dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
print('Created',dest)
