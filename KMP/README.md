# MMKV Kotlin Multiplatform（OpenHarmony 适配）

本分支基于 [Tencent/MMKV](https://github.com/Tencent/MMKV) 2.4.2，保留 `com.tencent.mmkv.kmp` API，
增加 `ohosArm64`。源码及衍生代码继续遵循上游 BSD 3-Clause 许可证。

## Gradle 依赖

在 `dependencyResolutionManagement.repositories` 中加入 JitPack：

```kotlin
maven { url = uri("https://jitpack.io") }
```

在共享模块中引入 MMKV KMP：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.gycrosskit.mmkv-ohos:mmkv-kmp:2.4.2-ohos-2.2.21-4")
        }
    }
}
```

Android 接入方应在 `gradle.properties` 中启用 AndroidX：

```properties
android.useAndroidX=true
```

支持的目标：

* Android
* `iosArm64`
* `iosSimulatorArm64`
* `iosX64`
* `ohosArm64`

iOS 最低部署版本为 13.0；Apple Silicon 模拟器运行时从 iOS 14 开始。

## 发布产物

消费者只依赖根坐标：

```text
com.github.gycrosskit.mmkv-ohos:mmkv-kmp:2.4.2-ohos-2.2.21-4
```

Gradle 根据 Kotlin Multiplatform 元数据选择 Android、iOS 或鸿蒙产物，无需直接声明平台产物。
JitPack 从本仓库对应版本的 GitHub Release 下载在 macOS 上构建的 Maven 产物。
本地验证时，可将消费者的仓库地址改为 `KMP/mmkv/build/local-maven`。

## 初始化

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

## 基本用法

```kotlin
val kv = MMKV.defaultMMKV()
kv.encodeString("name", "MMKV")
val name = kv.decodeString("name")
```

`close()` 会永久销毁对应的原生 MMKV 实例，引用同一实例的所有对象会立即失效。调用方需先确保没有正在执行的操作，之后不再使用旧引用；重新打开同一 ID 前应丢弃全部旧引用。

Android 继续依赖官方 `com.tencent:mmkv:2.4.2` AAR。iOS 和 OpenHarmony 的 KLIB 通过
C bridge 携带 MMKV Core 静态库，鸿蒙 KMP 消费者无需额外引入 `@tencent/mmkv` ohpm 包。
鸿蒙最终 `.so` 仍需由宿主正常打包。原生 C bridge 已在鸿蒙模拟器完成读写验证；
KMP 产物已完成链接，ArkTS 宿主调用、真机运行及与 ArkTS MMKV 共存尚未验证。

使用 `mmkv-kmp` 的同一个 iOS 二进制文件中，不要再链接原生 MMKV CocoaPod 或 SwiftPM 产品；两者都包含 MMKV Core，可能产生重复的原生符号。

## 文档

- [返回项目 README](../README.md)：版本、平台要求、发布与反馈入口。
- [开发与验证](DEVELOPMENT.md)：源码构建、独立消费者和发布归档。
- [上游发布流程](PUBLISHING.md)：Maven Central 流程；本 fork 当前使用 JitPack。
