# MMKV KMP 开发与验证

**最终核对入口（2026-10-08）**：[测试源码、执行平台、前轮复用与本轮重跑](../docs/功能与平台差异.md#验证结果与测试入口)。下列构建/发布命令与已有版本记录保留原范围，不能套用为候选新执行结果。

当前可消费版本为 `2.4.2-ohos-2.2.21-5`；准确发布状态见[对应 Release](https://github.com/gycrosskit/mmkv-ohos/releases/tag/2.4.2-ohos-2.2.21-5)。Core 三端消费映射与设备验收边界见[功能与平台差异](../docs/功能与平台差异.md)。此次源码维护删除 CMP 示例，继续沿用既有制品，不发布新版本。

消费者接入见 [KMP 指南](README.md)。以下命令在仓库根目录执行；本地源码构建与远程坐标消费是不同的验证。

## 从源码构建

构建三端产物需要 macOS、Xcode、Android SDK、OpenHarmony Native SDK、CMake 和 JDK：

```bash
ANDROID_HOME=/path/to/android-sdk bash KMP/gradlew -p KMP \
  :mmkv:publishAllPublicationsToLocalTestRepository \
  -POHOS_NATIVE_SDK=/path/to/openharmony/native \
  -PMMKV_CMAKE=/path/to/cmake

ANDROID_HOME=/path/to/android-sdk bash KMP/gradlew -p verification-consumer \
  compileDebugKotlinAndroid linkDebugFrameworkIosArm64 \
  linkDebugSharedOhosArm64 linkDebugExecutableOhosArm64
```

`MMKV_CMAKE` 可省略，此时要求 `cmake` 已在 `PATH`。KMP 验证工程默认从 JitPack 解析远程 Maven 坐标，不引用本仓库源码。
需要自定义产物目录时使用 `-PMMKV_LOCAL_REPOSITORY=/path/to/maven`，并在验证工程传入
`-PmmkvMavenRepo=/path/to/maven`。`verification-consumer/native` 使用本地编译的鸿蒙静态库
验证 C bridge 读写，可在鸿蒙模拟器执行。

当前源码已移除上游 CMP 示例及其 Android/desktop/iOS 宿主，当前消费验证使用上述 `verification-consumer`；已验证的 `2.4.2-ohos-2.2.21-5` Maven 制品本来不包含这些示例，消费坐标保持不变。旧示例接线与当时验证仅保留在 [2.4.2-ohos-2.2.21-5 的 DEVELOPMENT](https://github.com/gycrosskit/mmkv-ohos/blob/2.4.2-ohos-2.2.21-5/KMP/DEVELOPMENT.md)；历史 tag 与上游 BSD-3-Clause 许可不变。

上游 Maven Central 发布流程见 [PUBLISHING.md](./PUBLISHING.md)。本分支通过 JitPack 发布，
发布新版本时需要在 macOS 构建到空的 Maven 目录，执行
`python3 prepare-jitpack-maven.py /path/to/maven` 将 cinterop KLIB 拆成独立模块，再用
`COPYFILE_DISABLE=1 tar --no-xattrs` 打包该目录下的 `com/github/gycrosskit/mmkv-ohos`，
校验全部 10 个 publication 的产物、POM 与四种 sidecar 后，将实际归档 SHA-256 写入根目录的
`release-checksums.txt`（`Tag SHA256`），并上传同版本的 `mmkv-ohos-maven.tar.gz` 到 GitHub Release。

`2.4.2-ohos-2.2.21-4` 已通过真实 JitPack 消费者的 Android 编译、iOS arm64 Framework 链接、
OHOS 动态库与可执行文件链接，iOS/OHOS 禁用 partial linkage。10 个真实远程 publication 的
BSD-3-Clause POM、完整变体引用、文件大小、四种声明哈希、ZIP CRC 和内部依赖均通过，
Release 重下载 SHA-256 一致。JitPack 额外生成的 root identity redirect 与高阶 sidecar 的
404 单列为渠道边界，公开 MD5/SHA-1 和必要变体引用正常；设备运行仍由宿主验收。
