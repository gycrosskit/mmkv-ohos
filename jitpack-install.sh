#!/usr/bin/env bash
set -euo pipefail

archive=mmkv-maven.tar.gz
curl -fL --retry 3 -o "$archive" "https://github.com/gycrosskit/mmkv-ohos/releases/download/${VERSION}/${archive}"
sha256sum -c mmkv-maven.sha256
mkdir -p "$HOME/.m2/repository" build/release-maven
tar -xzf "$archive" -C "$HOME/.m2/repository"
tar -xzf "$archive" -C build/release-maven

python3 - <<'PY'
import json
from pathlib import Path

# JitPack maps classified KMP source/metadata JAR URLs to missing plain JARs.
for root in (Path.home() / '.m2/repository/com/github/gycrosskit/mmkv-ohos', Path('build/release-maven')):
    for file in root.rglob('*.module'):
        if file.name.startswith('._'):
            continue
        data = json.loads(file.read_text())
        data['variants'] = [
            variant for variant in data['variants']
            if not variant['name'].endswith(('SourcesElements-published', 'MetadataElements-published'))
        ]
        file.write_text(json.dumps(data, indent=2))
PY
