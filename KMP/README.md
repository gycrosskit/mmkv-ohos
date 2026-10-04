# MMKV Kotlin Multiplatform（OpenHarmony 适配）

本分支基于 [Tencent/MMKV](https://github.com/Tencent/MMKV) 2.4.2，保留 `com.tencent.mmkv.kmp` API，
增加 `ohosArm64`。源码及衍生代码继续遵循上游 BSD 3-Clause 许可证。

## Gradle dependency

在 `dependencyResolutionManagement.repositories` 中加入 JitPack：

```kotlin
maven { url = uri("https://jitpack.io") }
```

Add the MMKV KMP package to your shared module:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.gycrosskit.mmkv-ohos:mmkv-kmp:2.4.2-ohos-2.2.21-3")
        }
    }
}
```

Android consumers should have AndroidX enabled in `gradle.properties`:

```properties
android.useAndroidX=true
```

支持的目标：

* Android
* `iosArm64`
* `iosSimulatorArm64`
* `iosX64`
* `ohosArm64`

The iOS deployment target is 13.0. Apple Silicon simulator runtimes start at
iOS 14.

## Published artifacts

消费者只依赖根坐标：

```text
com.github.gycrosskit.mmkv-ohos:mmkv-kmp:2.4.2-ohos-2.2.21-3
```

Gradle 根据 Kotlin Multiplatform 元数据选择 Android、iOS 或鸿蒙产物，无需直接声明平台产物。
JitPack 从本仓库对应版本的 GitHub Release 下载在 macOS 上构建的 Maven 产物。
本地验证时，可将消费者的仓库地址改为 `KMP/mmkv/build/local-maven`。

## Initialize

Android:

```kotlin
import android.app.Application
import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.initialize

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
    }
}
```

iOS:

```kotlin
import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.initialize

MMKV.initialize()
```

OpenHarmony：由宿主传入应用私有、可写目录，在首次使用前初始化。

```kotlin
import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.initialize

MMKV.initialize(rootDir)
```

## Basic usage

```kotlin
val kv = MMKV.defaultMMKV()
kv.encodeString("name", "MMKV")
val name = kv.decodeString("name")
```

`close()` permanently destroys the native MMKV instance. All references backed
by the same native instance become invalid immediately. The caller must ensure
that no operation is running and no reference is used afterward. Discard every
reference before reopening the same ID.

Android 继续依赖官方 `com.tencent:mmkv:2.4.2` AAR。iOS 和 OpenHarmony 的 KLIB 通过
C bridge 携带 MMKV Core 静态库，鸿蒙 KMP 消费者无需额外引入 `@tencent/mmkv` ohpm 包。
鸿蒙最终 `.so` 仍需由宿主正常打包。原生 C bridge 已在鸿蒙模拟器完成读写验证；
KMP 产物已完成链接，ArkTS 宿主调用、真机运行及与 ArkTS MMKV 共存尚未验证。

Do not also link the native MMKV CocoaPod or SwiftPM product into the same iOS
binary that consumes `mmkv-kmp`; both contain MMKV Core and can produce
duplicate native symbols.

## Build from source

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

`MMKV_CMAKE` 可省略，此时要求 `cmake` 已在 `PATH`。`verification-consumer` 默认从 JitPack 解析已发布版本，
传入 `-PmmkvMavenRepo` 时可验证本地待发布产物；两种方式均不引用本仓库源码。
需要自定义产物目录时使用 `-PMMKV_LOCAL_REPOSITORY=/path/to/maven`，并在验证工程传入
`-PmmkvMavenRepo=/path/to/maven`。`verification-consumer/native` 使用本地编译的鸿蒙静态库
验证 C bridge 读写，可在鸿蒙模拟器执行。

示例默认依赖本仓库源码；增加 `-PMMKV_INCLUDE_SAMPLE=true -PMMKV_USE_PUBLISHED=true`
后改为消费 `VERSION_NAME` 指定的 JitPack 版本，仓库配置优先从 JitPack 解析本 fork 的坐标；显式指定的本地验证仓库仍优先。

上游 Maven Central 发布流程见 [PUBLISHING.md](./PUBLISHING.md)。本分支通过 JitPack 发布，
发布新版本时需要在 macOS 构建到空的 Maven 目录，执行
`python3 prepare-jitpack-maven.py /path/to/maven` 将 cinterop KLIB 拆成独立模块，再用
`COPYFILE_DISABLE=1 tar --no-xattrs` 打包该目录下的 `com/github/gycrosskit/mmkv-ohos`，
更新根目录的 `mmkv-maven.sha256`，并上传同版本的 `mmkv-maven.tar.gz` 到 GitHub Release。
