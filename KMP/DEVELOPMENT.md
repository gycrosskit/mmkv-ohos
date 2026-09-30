# MMKV KMP 开发与验证

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

上游 Maven Central 发布流程见 [PUBLISHING.md](./PUBLISHING.md)。本分支通过 JitPack 发布，
发布新版本时需要在 macOS 构建到空的 Maven 目录，执行
`python3 prepare-jitpack-maven.py /path/to/maven` 将 cinterop KLIB 拆成独立模块，再用
`COPYFILE_DISABLE=1 tar --no-xattrs` 打包该目录下的 `com/github/gycrosskit/mmkv-ohos`，
更新根目录的 `mmkv-maven.sha256`，并上传同版本的 `mmkv-maven.tar.gz` 到 GitHub Release。
