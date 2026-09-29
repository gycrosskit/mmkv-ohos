# GY CrossKit MMKV OpenHarmony 适配

本仓库基于 [Tencent/MMKV](https://github.com/Tencent/MMKV) 2.4.2，为实验性的 MMKV Kotlin Multiplatform 模块增加 `ohosArm64`，保留原有 `com.tencent.mmkv.kmp` API。Android、iOS 和 OpenHarmony 的接入细节见 [KMP 指南](KMP/README.md)。

## 引入 KMP 依赖

在 `settings.gradle.kts` 的 `dependencyResolutionManagement.repositories` 中加入：

```kotlin
maven { url = uri("https://jitpack.io") }
```

在共享模块中引入本 fork 的坐标：

```kotlin
commonMain.dependencies {
    implementation("com.github.gycrosskit.mmkv-ohos:mmkv-kmp:2.4.2-ohos-2.2.21-3")
}
```

Gradle 根据 KMP 元数据选择 Android、`iosArm64`、`iosSimulatorArm64`、`iosX64` 或 `ohosArm64` 产物。JitPack 从相同版本的 GitHub Release 获取在 macOS 上构建、经过摘要校验的产物。鸿蒙原生库仍需由宿主正常打包；ArkTS 宿主调用和真机运行尚未验证。

## 文档与上游

- [KMP 指南](KMP/README.md)：本 fork 的初始化、产物与构建说明。
- [上游中文说明](README_CN.md)、[上游英文说明](https://github.com/Tencent/MMKV/blob/master/README.md)：MMKV 的通用用法。上游文档中的 `com.tencent:mmkv-kmp` 坐标不代表本 fork 的鸿蒙版本；接入鸿蒙请使用上面的 JitPack 坐标。
- [Tencent/MMKV](https://github.com/Tencent/MMKV)：原项目及更新。

源码和衍生代码遵循上游 BSD 3-Clause 许可证，见 [LICENSE.TXT](LICENSE.TXT)。
