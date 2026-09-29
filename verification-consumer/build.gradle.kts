plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.2.21-1.0.0"
    id("com.android.library") version "8.10.1"
}

kotlin {
    androidTarget()
    iosArm64 {
        binaries.framework()
        compilerOptions.freeCompilerArgs.add("-Xpartial-linkage=disable")
    }
    ohosArm64 {
        binaries.sharedLib()
        binaries.executable()
        compilerOptions.freeCompilerArgs.add("-Xpartial-linkage=disable")
    }
    sourceSets.commonMain.dependencies {
        implementation("com.github.gycrosskit.mmkv-ohos:mmkv-kmp:2.4.2-ohos-2.2.21-3")
    }
}

android {
    namespace = "io.github.gycrosskit.mmkv.verification"
    compileSdk = 35
    defaultConfig.minSdk = 23
}
