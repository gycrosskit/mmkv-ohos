#!/usr/bin/env bash
set -euo pipefail
# https://github.com/openharmony/docs/blob/master/en/release-notes/OpenHarmony-v5.0.3-release.md
# SDK version and SHA256 are fixed to that official release, including host architecture.
case "$(uname -m)" in
  x86_64) sdk_archive=ohos-sdk-mac-public.tar.gz; sdk_sha=1fa1c67cc025994f7083d7e8bbe5ed7ad57959ceec56c91d609e30c30e2c00e8 ;;
  arm64) sdk_archive=L2-SDK-MAC-M1-PUBLIC.tar.gz; sdk_sha=d43e8c69e079076b8b14e3d4dc368c83d47a0cb3187c47aa3d83fa98d9a7928d ;;
  *) echo 'Unsupported SDK host architecture' >&2; exit 1 ;;
esac
sdk_file="$RUNNER_TEMP/$sdk_archive"
curl --fail --location --proto '=https' --proto-redir '=https' --retry 3 --connect-timeout 30 --max-time 900 --output "$sdk_file" "https://repo.huaweicloud.com/openharmony/os/5.0.3-Release/$sdk_archive"
printf '%s  %s\n' "$sdk_sha" "$sdk_file" | shasum -a 256 --check
python3 - "$sdk_file" "$RUNNER_TEMP/ohos-sdk" <<'SDK'
import json, os, shutil, subprocess, sys, tarfile
from pathlib import Path
archive, destination = Path(sys.argv[1]), Path(sys.argv[2])
destination.mkdir()
with tarfile.open(archive, 'r:gz') as source:
    natives = [entry for entry in source if entry.isfile() and Path(entry.name).name.startswith('native-') and entry.name.endswith('.zip')]
    assert len(natives) == 1, f'Expected one official Native SDK ZIP, found {len(natives)}'
    native_zip = destination / 'native.zip'
    with source.extractfile(natives[0]) as input_file, native_zip.open('wb') as output_file:
        shutil.copyfileobj(input_file, output_file)
# Native clang/ld are symlinks; system unzip preserves links and executable modes.
subprocess.run(['unzip', '-q', str(native_zip), '-d', str(destination)], check=True)
toolchains = list(destination.rglob('build/cmake/ohos.toolchain.cmake'))
assert len(toolchains) == 1, f'Expected one OHOS toolchain, found {len(toolchains)}'
native = toolchains[0].parents[2]
package = json.loads((native / 'oh-uni-package.json').read_text())
assert str(package['apiVersion']) == '15' and package['version'] == '5.0.3.135', package
clang = native / 'llvm/bin/clang'
assert clang.is_file() and os.access(clang, os.X_OK), f'Missing executable Native clang: {clang}'
with open(os.environ['GITHUB_ENV'], 'a') as output:
    output.write(f'OHOS_SDK_NATIVE={native}\n')
print(json.dumps({'native': str(native), 'package': package}, sort_keys=True))
SDK
