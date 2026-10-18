"""Check the Core-only fork boundary and real Maven staging metadata."""
import hashlib
import json
import re
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


class SourceBoundaryTest(unittest.TestCase):
    def test_kmp_has_no_retired_cmp_sample_or_ui_dependencies(self):
        root = Path(__file__).resolve().parents[1]
        self.assertFalse((root / "KMP/sample").exists(), "Retired CMP sample returned")
        # Kuikly may need Compose runtime/compiler; only the separate UI stack is forbidden.
        ui = re.compile(
            r"(?:androidx|org\.jetbrains)\.compose\.(?:ui|foundation|material)"
            r"|org\.jetbrains\.compose[\"\':]"
            r"|compose\.(?:ui|foundation|material|desktop|components|preview)"
            r"|activity-compose|ComposeUIViewController"
        )
        for directory in (root / "KMP", root / "verification-consumer"):
            for path in directory.rglob("*"):
                if path.suffix not in (".kt", ".kts") or any(
                    part in ("build", ".gradle", ".kotlin") for part in path.parts
                ):
                    continue
                with self.subTest(path=str(path.relative_to(root))):
                    self.assertIsNone(ui.search(path.read_text()), "Independent CMP UI stack returned")


class MavenPreparationTest(unittest.TestCase):
    def test_native_artifacts_and_dependencies_survive_source_cleanup(self):
        group = "com.github.gycrosskit.mmkv-ohos"
        version = "test-version"
        platforms = ("iosarm64", "iossimulatorarm64", "iosx64", "ohosarm64")
        with tempfile.TemporaryDirectory() as directory:
            staging = Path(directory)
            root = staging / "com/github/gycrosskit/mmkv-ohos"
            original_native = {}
            for platform in platforms:
                artifact = "mmkv-kmp-" + platform
                folder = root / artifact / version
                folder.mkdir(parents=True)
                binary = (platform + "-cinterop-bytes").encode()
                filename = "mmkv-cinterop-test.klib"
                (folder / filename).write_bytes(binary)
                cinterop = {"name": filename, "url": filename, "size": len(binary), "sha256": hashlib.sha256(binary).hexdigest()}
                native = {"name": platform + "ApiElements-published", "attributes": {"org.jetbrains.kotlin.native.target": platform}, "files": [{"url": "runtime.klib"}, cinterop], "dependencies": [{"group": "org.jetbrains.kotlin", "module": "stdlib"}]}
                original_native[platform] = native
                metadata = {"component": {"group": group, "module": artifact, "version": version}, "variants": [native, {"name": platform + "SourcesElements-published"}]}
                (folder / (artifact + ".module")).write_text(json.dumps(metadata))
            folder = root / "mmkv-kmp" / version
            folder.mkdir(parents=True)
            api = {"name": "metadataApiElements", "files": [{"url": "root.jar"}]}
            routing = {"name": "iosarm64ApiElements-published", "available-at": {"url": "../../mmkv-kmp-iosarm64/test-version/native.module", "group": group, "module": "mmkv-kmp-iosarm64", "version": version}}
            (folder / "root.module").write_text(json.dumps({"variants": [api, {"name": "metadataSourcesElements"}, routing]}))
            script = Path(__file__).resolve().parents[1] / "prepare-jitpack-maven.py"
            result = subprocess.run([sys.executable, str(script), directory], text=True, capture_output=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(json.loads((folder / "root.module").read_text())["variants"], [api, routing])
            for platform in platforms:
                native_path = next((root / ("mmkv-kmp-" + platform)).glob("*/*.module"))
                native = json.loads(native_path.read_text())["variants"]
                self.assertEqual(len(native), 1)
                self.assertEqual(native[0]["files"], [{"url": "runtime.klib"}])
                cinterop_name = "mmkv-cinterop-" + platform
                self.assertIn({"group": group, "module": cinterop_name, "version": {"requires": version}}, native[0]["dependencies"])
                interop_path = next((root / cinterop_name).glob("*/*.module"))
                interop = json.loads(interop_path.read_text())["variants"][0]
                self.assertEqual(interop["attributes"], original_native[platform]["attributes"])
                self.assertEqual(interop["dependencies"], [{"group": "org.jetbrains.kotlin", "module": "stdlib"}])
                artifact = interop_path.parent / interop["files"][0]["url"]
                self.assertEqual(artifact.read_bytes(), (platform + "-cinterop-bytes").encode())
                self.assertEqual(interop["files"][0]["sha256"], hashlib.sha256(artifact.read_bytes()).hexdigest())
                self.assertIn("published-with-gradle-metadata", next(interop_path.parent.glob("*.pom")).read_text())
            for module in root.rglob("*.module"):
                for algorithm in ("md5", "sha1", "sha256", "sha512"):
                    self.assertEqual(module.with_name(module.name + "." + algorithm).read_text(), hashlib.new(algorithm, module.read_bytes()).hexdigest())


if __name__ == "__main__":
    unittest.main()
