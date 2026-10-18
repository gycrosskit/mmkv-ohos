# MMKV KMP 开发与验证

当前维护 RC：`2.4.2-ohos-2.2.21-6-rc.1`。准确发布状态见 [Release](https://github.com/gycrosskit/mmkv-ohos/releases/tag/2.4.2-ohos-2.2.21-6-rc.1)；该版本远程产物验收见 [Fork release validation](https://github.com/gycrosskit/mmkv-ohos/actions/workflows/release-validation.yml)。只有对应版本成功运行才构成验收，下面命令不代表已经执行。历史测试和设备边界见[功能与平台差异](../docs/功能与平台差异.md)。

## 源码 CI

[Fork source regression](../.github/workflows/fork-source-regression.yml) 在 PR 固定 head 上执行 Android AAR、iOS Simulator 库测试（必须实际执行 `MMKVSmokeTest`、`NativeStorageRecoveryTest`、`NativeHandlerBoundaryTest`）、iOS arm64 编译、OHOS 编译，以及轻量 Python 合同测试。macOS 使用 JDK 17 与官方 OpenHarmony 5.0.3.135 / API 15 Native SDK；[安装脚本](../Scripts/install-ohos-sdk.sh)按宿主架构校验官方 SHA-256，仅安装到 runner 临时目录。源码 CI 与发布消费验收分开记录。

上游 CMP 示例与 Android/desktop/iOS 宿主已删除，非 UI 独立消费者为 `verification-consumer`。旧示例与当时结果保留在 [历史 -5 tag](https://github.com/gycrosskit/mmkv-ohos/blob/2.4.2-ohos-2.2.21-5/KMP/DEVELOPMENT.md)。Core/KMP 生产逻辑、11 项有效库测试和 BSD-3-Clause 许可保留。

## RC 归档与冻结

[Package release candidate](../.github/workflows/package-release.yml) 在指定维护分支的构建输入变更或手动触发时运行；单独修改 README/文档或 `release-checksums.txt` 不重建。macOS 15 / JDK 17 / CMake / 已校验 SDK15 执行：

```bash
VERSION=2.4.2-ohos-2.2.21-6-rc.1 bash Scripts/package-maven.sh
```

脚本发布到全新空 staging，调用现有 `prepare-jitpack-maven.py` 拆分 cinterop，再使用固定 shared CI checker 校验恰好 10 个 publication（root、Android、4 Native、4 cinterop）、四种 sidecar/声明哈希、BSD-3-Clause、Java 11 字节码，并检查 Kotlin metadata 与所有 JAR/AAR/KLIB 的 ZIP CRC。运行需要 workflow 检出的 `.ci-shared`、Android SDK、Xcode 和 CMake，不在本地安装全局工具链。

产物为 `mmkv-ohos-maven.tar.gz`、`SHA256SUMS`、`source-receipt.json`；收据记录实际 producer HEAD/tree/run。维护者核对归档后，将真实 SHA-256 写入 `release-checksums.txt` 的 `Tag SHA256` 行，经 PR CI/合并后创建不可变同版本 tag，再把同一归档和收据上传同版本 prerelease。不得重新打包改变字节。JitPack installer 与 checksum 格式保持原样。

## 公开远程消费

Release `published` 事件触发一次完整验收，也可手动指定精确版本。先确认当前 checkout 等于远程 tag commit，核验冻结归档 SHA-256、解包后的全部 inventory/metadata，再核验 JitPack state commit 与全部 10 个公开 publication 的 identity/POM/license/variant bytes/四种声明哈希/ZIP CRC/内部引用，并逐个比对公开 variant 文件 SHA-256 与冻结归档中的相同字节。公网 MD5/SHA-1 sidecar 必须存在；JitPack 若缺 SHA-256/SHA-512 sidecar，报告为缺失，不计作已验证；冻结归档四种 sidecar 均必须完整。

只有该阶段成功后，独立全新 `GRADLE_USER_HOME` 才执行：

```bash
bash KMP/gradlew -p verification-consumer -PremoteOnly -PmmkvVersion=2.4.2-ohos-2.2.21-6-rc.1 \
  compileDebugKotlinAndroid linkDebugFrameworkIosArm64 linkDebugFrameworkIosSimulatorArm64 \
  compileKotlinIosX64 linkDebugSharedOhosArm64 linkDebugExecutableOhosArm64 \
  --refresh-dependencies --no-daemon --max-workers=1 --no-parallel
```

CI 根据 macOS 实际架构选择对应 Simulator Framework 链接。`remoteOnly` 把本 fork group 独占路由到公开 JitPack，拒绝 `mmkvMavenRepo` 本地覆盖、`MMKV_LOCAL_REPOSITORY`、`MMKV_USE_MAVEN_LOCAL`、源码替换和 composite build；不使用 `mavenLocal`，不进行 MMKV 源码 SDK/CMake 二次构建。iOS/OHOS 禁用 partial linkage；OHOS 同时链接 shared library 与 executable，以暴露最终 Native 未解析符号。编译/链接通过不等于 Android/OHOS 设备、ArkTS 宿主、跨进程或持久化重启验收。

## 本地定向核对

本地只运行轻量检查：

```bash
python3 Scripts/test-jitpack-metadata.py
python3 Scripts/test-release-contract.py
```

需要明确授权的源码构建时，沿用 `KMP/gradlew -p KMP`；`MMKV_CMAKE` 可指定 CMake，`OHOS_NATIVE_SDK` 指定现有 SDK，`MMKV_LOCAL_REPOSITORY` 指向独立空 Maven 目录。本地消费者可显式传 `mmkvMavenRepo`，但不能与 `remoteOnly` 同用，也不构成公开远程消费证明。`verification-consumer/native` 的 C bridge 模拟器读写保留为独立原生验证入口。上游 Maven Central 流程见 [PUBLISHING.md](PUBLISHING.md)。

## 历史验收

`2.4.2-ohos-2.2.21-4` 已通过真实 JitPack 消费者的 Android 编译、iOS arm64 Framework 链接、
OHOS 动态库与可执行文件链接，iOS/OHOS 禁用 partial linkage。10 个真实远程 publication 的
BSD-3-Clause POM、完整变体引用、文件大小、四种声明哈希、ZIP CRC 和内部依赖均通过，
Release 重下载 SHA-256 一致。JitPack 额外生成的 root identity redirect 与高阶 sidecar 的
404 单列为渠道边界，公开 MD5/SHA-1 和必要变体引用正常；设备运行仍由宿主验收。
