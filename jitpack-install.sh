#!/usr/bin/env bash
set -euo pipefail

archive=mmkv-maven.tar.gz
curl -fL --retry 3 -o "$archive" "https://github.com/gycrosskit/mmkv-ohos/releases/download/${VERSION}/${archive}"
sha256sum -c mmkv-maven.sha256
mkdir -p "$HOME/.m2/repository" build/release-maven
tar -xzf "$archive" -C "$HOME/.m2/repository"
tar -xzf "$archive" -C build/release-maven
