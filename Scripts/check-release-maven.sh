#!/usr/bin/env bash
set -euo pipefail
: "${VERSION:?Provide an immutable Maven tag}"
staging="${1:?Provide the extracted Maven directory}"
publications=mmkv-kmp,mmkv-kmp-android,mmkv-kmp-iosarm64,mmkv-kmp-iossimulatorarm64,mmkv-kmp-iosx64,mmkv-kmp-ohosarm64,mmkv-cinterop-iosarm64,mmkv-cinterop-iossimulatorarm64,mmkv-cinterop-iosx64,mmkv-cinterop-ohosarm64
python3 .ci-shared/templates/check-maven.py "$staging" com.github.gycrosskit.mmkv-ohos "$VERSION" mmkv-kmp ios_arm64,ios_x64,ios_simulator_arm64,ohos_arm64 "$publications" --license BSD-3-Clause --max-jvm-major 55
python3 - "$staging" "$VERSION" <<'PYTHON'
import json, sys, zipfile
from pathlib import Path
root = Path(sys.argv[1]) / 'com/github/gycrosskit/mmkv-ohos/mmkv-kmp' / sys.argv[2]
module = next(root.glob('*.module'))
metadata = next(v for v in json.loads(module.read_text())['variants'] if v['name'] == 'metadataApiElements')
assert metadata['attributes']['org.jetbrains.kotlin.platform.type'] == 'common', metadata
files = metadata['files']
assert len(files) == 1 and files[0]['url'].endswith('.jar'), files
with zipfile.ZipFile(root / files[0]['url']) as jar:
    assert any(name.endswith('.knm') for name in jar.namelist()), 'Missing Kotlin metadata'
for artifact in Path(sys.argv[1]).rglob('*'):
    if artifact.suffix in ('.jar', '.aar', '.klib'):
        with zipfile.ZipFile(artifact) as binary:
            assert binary.testzip() is None, artifact
            assert not any(Path(name).name.startswith('._') for name in binary.namelist()), artifact
print('Kotlin metadata and every JAR/AAR/KLIB ZIP CRC passed')
PYTHON
