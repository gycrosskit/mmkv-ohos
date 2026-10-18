#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${VERSION:?Provide the RC version}"
[[ "$VERSION" =~ ^[0-9][0-9A-Za-z._-]*-rc\.[0-9]+$ ]] || { echo 'Expected an RC tag' >&2; exit 1; }
output="$PWD/build/release/$VERSION"
test ! -e "$output"
mkdir -p "$output/maven"
bash KMP/gradlew -p KMP --init-script "$PWD/Scripts/ci-repositories.gradle" \
  :mmkv:publishAllPublicationsToLocalTestRepository \
  "-PVERSION_NAME=$VERSION" -PGROUP=com.github.gycrosskit.mmkv-ohos \
  "-PMMKV_LOCAL_REPOSITORY=$output/maven" \
  --no-daemon --max-workers=1 --no-parallel --no-build-cache --rerun-tasks --stacktrace \
  -Dorg.gradle.internal.http.connectionTimeout=15000 -Dorg.gradle.internal.http.socketTimeout=30000
python3 prepare-jitpack-maven.py "$output/maven"
bash Scripts/check-release-maven.sh "$output/maven"
COPYFILE_DISABLE=1 tar --no-xattrs -czf "$output/mmkv-ohos-maven.tar.gz" -C "$output/maven" com/github/gycrosskit/mmkv-ohos
