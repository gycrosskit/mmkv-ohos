"""Release routing and binary checks without Gradle, SDK installation or network."""
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = "2.4.2-ohos-2.2.21-6-rc.1"


class ReleaseContractTest(unittest.TestCase):
    def test_remote_consumers_reject_local_overrides(self):
        settings = (ROOT / "verification-consumer/settings.gradle.kts").read_text()
        self.assertIn('require(mmkvRepository == "https://jitpack.io")', settings)
        self.assertIn('require(!providers.gradleProperty(property).isPresent)', settings)
        for property in ["MMKV_LOCAL_REPOSITORY", "MMKV_USE_MAVEN_LOCAL", "verifyLocalSource"]:
            self.assertIn('"' + property + '"', settings)
        self.assertIn('require(gradle.startParameter.includedBuilds.isEmpty())', settings)
        self.assertIn('repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)', settings)
        self.assertIn('exclusiveContent', settings)
        for forbidden in ['mavenLocal(', 'includeBuild(', 'dependencySubstitution']:
            self.assertNotIn(forbidden, settings)
        workflow = (ROOT / ".github/workflows/release-validation.yml").read_text()
        consumers = [line for line in workflow.splitlines() if '-p verification-consumer' in line]
        self.assertEqual(2, len(consumers))
        self.assertTrue(all('-PremoteOnly "-PmmkvVersion=$VERSION"' in line for line in consumers))
        self.assertIn('needs: verify-public', workflow)
        self.assertIn('GRADLE_USER_HOME=$RUNNER_TEMP/mmkv-release-gradle', workflow)
        self.assertIn('publicVariantBytesMatchFrozenArchive', (ROOT / 'Scripts/verify-release.sh').read_text())
        self.assertNotIn('install-ohos-sdk', workflow)
        for task in ['compileDebugKotlinAndroid', 'linkDebugFrameworkIosArm64', 'linkDebugSharedOhosArm64', 'linkDebugExecutableOhosArm64']:
            self.assertIn(task, workflow)

    def test_exact_inventory_versions_and_shared_sdk(self):
        script = (ROOT / 'Scripts/check-release-maven.sh').read_text()
        inventory = re.search(r'^publications=(.+)$', script, re.M).group(1).split(',')
        self.assertEqual(10, len(set(inventory)))
        self.assertEqual(4, sum(name.startswith('mmkv-cinterop-') for name in inventory))
        self.assertIn('mmkv-android', inventory)
        self.assertNotIn('mmkv-kmp-android', inventory)
        public_script = (ROOT / 'Scripts/verify-release.sh').read_text()
        public_inventory = re.search(r'^publications=(.+)$', public_script, re.M).group(1).split(',')
        self.assertEqual(inventory, public_inventory)
        self.assertIn('--license BSD-3-Clause --max-jvm-major 55', script)
        for name in ['fork-source-regression.yml', 'package-release.yml']:
            workflow = (ROOT / '.github/workflows' / name).read_text()
            self.assertIn('bash Scripts/install-ohos-sdk.sh', workflow)
            self.assertIn('607ff44db144c68fc6523de38c04aac29e427b68', workflow)
        self.assertIn('VERSION_NAME=' + VERSION, (ROOT / 'KMP/gradle.properties').read_text())
        self.assertIn(VERSION, (ROOT / 'verification-consumer/build.gradle.kts').read_text())
        self.assertIn(VERSION, (ROOT / '.github/workflows/package-release.yml').read_text())

    def test_binary_contract_rejects_corruption_and_missing_kotlin_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            # This isolates the extra binary contract; the pinned checker validates Maven separately.
            shared = directory / '.ci-shared/templates'
            shared.mkdir(parents=True)
            (shared / 'check-maven.py').write_text('')
            root = directory / 'maven/com/github/gycrosskit/mmkv-ohos/mmkv-kmp' / VERSION
            root.mkdir(parents=True)
            (root / 'root.module').write_text(json.dumps({'variants': [{
                'name': 'metadataApiElements', 'attributes': {'org.jetbrains.kotlin.platform.type': 'common'},
                'files': [{'url': 'metadata.jar'}]}]}))
            jar = root / 'metadata.jar'
            def check():
                return subprocess.run(['bash', str(ROOT / 'Scripts/check-release-maven.sh'), str(directory / 'maven')],
                                      cwd=directory, env=os.environ | {'VERSION': VERSION}, capture_output=True)
            with zipfile.ZipFile(jar, 'w') as output:
                output.writestr('commonMain/package_mmkv/0_mmkv.knm', b'fixture')
            self.assertEqual(0, check().returncode)
            (root / 'broken.klib').write_bytes(b'broken')
            self.assertNotEqual(0, check().returncode)
            (root / 'broken.klib').unlink()
            with zipfile.ZipFile(jar, 'w') as output:
                output.writestr('META-INF/MANIFEST.MF', b'fixture')
            self.assertNotEqual(0, check().returncode)

    def test_wrong_checkout_fails_before_download(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            git = directory / 'git'
            git.write_text('#!/bin/sh\ncase "$1" in\nls-remote) echo "' + 'a' * 40 + ' refs/tags/fixture" ;;\nrev-parse) echo "' + 'b' * 40 + '" ;;\n*) exit 99 ;;\nesac\n')
            marker = directory / 'downloaded'
            curl = directory / 'curl'
            curl.write_text('#!/bin/sh\ntouch "' + str(marker) + '"\nexit 99\n')
            git.chmod(0o755)
            curl.chmod(0o755)
            result = subprocess.run(['bash', str(ROOT / 'Scripts/verify-release.sh')], cwd=ROOT,
                                    env=os.environ | {'PATH': str(directory) + os.pathsep + os.environ['PATH'], 'VERSION': VERSION},
                                    capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode)
            self.assertIn('Checkout does not match the exact remote tag commit', result.stderr)
            self.assertFalse(marker.exists())


if __name__ == '__main__':
    unittest.main()
