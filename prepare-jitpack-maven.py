"""Prepare a macOS-built MMKV Maven directory for JitPack's metadata rewriting."""

import hashlib
import json
import shutil
import sys
from pathlib import Path

root = Path(sys.argv[1]) / "com/github/gycrosskit/mmkv-ohos"
group = "com.github.gycrosskit.mmkv-ohos"
platforms = ("iosarm64", "iossimulatorarm64", "iosx64", "ohosarm64")


def write_module(path, data):
    content = (json.dumps(data, indent=2) + "\n").encode()
    path.write_bytes(content)
    for algorithm in ("md5", "sha1", "sha256", "sha512"):
        path.with_name(path.name + "." + algorithm).write_text(
            hashlib.new(algorithm, content).hexdigest()
        )


for platform in platforms:
    module = next((root / ("mmkv-kmp-" + platform)).glob("*/*.module"))
    metadata = json.loads(module.read_text())
    version = metadata["component"]["version"]
    variant = next(v for v in metadata["variants"] if v["name"].endswith("ApiElements-published"))
    cinterop = next(f for f in variant["files"] if "-cinterop-" in f["url"])
    variant["files"].remove(cinterop)

    artifact = "mmkv-cinterop-" + platform
    variant.setdefault("dependencies", []).append(
        {"group": group, "module": artifact, "version": {"requires": version}}
    )

    target_dir = root / artifact / version
    target_dir.mkdir(parents=True, exist_ok=True)
    filename = artifact + "-" + version + ".klib"
    shutil.copyfile(module.parent / cinterop["url"], target_dir / filename)
    cinterop["name"] = filename
    cinterop["url"] = filename

    cinterop_metadata = {
        "formatVersion": "1.1",
        "component": {"group": group, "module": artifact, "version": version},
        "variants": [{
            "name": variant["name"],
            "attributes": variant["attributes"],
            "dependencies": [d for d in variant["dependencies"] if d["group"] != group],
            "files": [cinterop],
        }],
    }
    write_module(target_dir / (artifact + "-" + version + ".module"), cinterop_metadata)
    (target_dir / (artifact + "-" + version + ".pom")).write_text(
        '<project xmlns="http://maven.apache.org/POM/4.0.0">\n'
        '  <!-- do_not_remove: published-with-gradle-metadata -->\n'
        '  <modelVersion>4.0.0</modelVersion>\n'
        '  <groupId>' + group + '</groupId>\n'
        '  <artifactId>' + artifact + '</artifactId>\n'
        '  <version>' + version + '</version>\n'
        '  <packaging>klib</packaging>\n'
        '</project>\n'
    )
    write_module(module, metadata)

for module in root.rglob("*.module"):
    metadata = json.loads(module.read_text())
    metadata["variants"] = [
        variant for variant in metadata["variants"]
        if not variant["name"].endswith(("SourcesElements-published", "MetadataElements-published"))
    ]
    write_module(module, metadata)
