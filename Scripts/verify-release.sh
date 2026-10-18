#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${VERSION:?Provide an immutable Maven tag}"
[[ "$VERSION" =~ ^[0-9][0-9A-Za-z._-]*$ ]] || { echo 'Invalid version' >&2; exit 1; }
refs="$(git ls-remote https://github.com/gycrosskit/mmkv-ohos.git "refs/tags/$VERSION" "refs/tags/$VERSION^{}")"
commit="$(printf '%s\n' "$refs" | awk '$2 ~ /\^\{\}$/ {peeled=$1} $2 !~ /\^\{\}$/ {direct=$1} END {print peeled ? peeled : direct}')"
[[ "$commit" =~ ^[a-f0-9]{40}$ ]] || { echo 'Tag has no exact commit' >&2; exit 1; }
test "$(git rev-parse HEAD)" = "$commit" || { echo 'Checkout does not match the exact remote tag commit' >&2; exit 1; }
checksum="$(awk -v version="$VERSION" '$1 == version {print $2}' release-checksums.txt)"
[[ "$checksum" =~ ^[a-f0-9]{64}$ ]] || { echo "Missing frozen checksum for $VERSION" >&2; exit 1; }
publications=mmkv-kmp,mmkv-android,mmkv-kmp-iosarm64,mmkv-kmp-iossimulatorarm64,mmkv-kmp-iosx64,mmkv-kmp-ohosarm64,mmkv-cinterop-iosarm64,mmkv-cinterop-iossimulatorarm64,mmkv-cinterop-iosx64,mmkv-cinterop-ohosarm64
output="$(mktemp -d)"
trap 'rm -rf "$output"' EXIT
archive="$output/mmkv-ohos-maven.tar.gz"
curl -fsSL --retry 3 --connect-timeout 30 --max-time 300 -o "$archive" "https://github.com/gycrosskit/mmkv-ohos/releases/download/$VERSION/mmkv-ohos-maven.tar.gz"
printf '%s  %s\n' "$checksum" "$archive" | shasum -a 256 -c -
python3 - "$archive" "$output/maven" <<'EXTRACT'
import sys, tarfile
from pathlib import Path
root = Path(sys.argv[2]).resolve()
with tarfile.open(sys.argv[1]) as bundle:
    for member in bundle.getmembers():
        assert member.isfile() or member.isdir(), member.name
        assert (root / member.name).resolve().is_relative_to(root), member.name
    bundle.extractall(root)
EXTRACT
bash Scripts/check-release-maven.sh "$output/maven"
python3 .ci-shared/templates/check-public-maven.py --repo mmkv-ohos --version "$VERSION" --commit "$commit" --expected-publications "$publications" --license BSD-3-Clause --output-dir "${CI_DIAGNOSTICS_DIR:-ci-diagnostics}/public"
python3 - "$output/maven" "${CI_DIAGNOSTICS_DIR:-ci-diagnostics}/public/proof.json" "$checksum" <<'FROZEN'
import hashlib, json, sys
from pathlib import Path
from urllib.parse import urlsplit
root, proof_path = Path(sys.argv[1]), Path(sys.argv[2])
proof = json.loads(proof_path.read_text())
for publication in proof['modules']:
    for url, digest in publication['files'].items():
        artifact = root / proof['publicGroup'].replace('.', '/') / publication['module'] / proof['version'] / Path(urlsplit(url).path).name
        assert artifact.is_file(), artifact
        assert hashlib.sha256(artifact.read_bytes()).hexdigest() == digest, f'Public bytes differ from frozen archive: {url}'
proof['frozenArchiveSha256'] = sys.argv[3]
proof['publicVariantBytesMatchFrozenArchive'] = True
proof_path.write_text(json.dumps(proof, indent=2) + '\n')
print('All public variant bytes match the frozen Release archive')
FROZEN
