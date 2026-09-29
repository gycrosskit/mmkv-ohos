plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.2.21-1.0.0"
    id("com.android.library") version "8.10.1"
}

kotlin {
    androidTarget()
    iosArm64 { binaries.framework() }
    ohosArm64 {
        binaries.sharedLib()
        binaries.executable()
    }
    sourceSets.commonMain.dependencies {
        implementation("com.github.gycrosskit.mmkv-ohos:mmkv-kmp:2.4.2-ohos-2.2.21-2")
    }
}

android {
    namespace = "io.github.gycrosskit.mmkv.verification"
    compileSdk = 35
    defaultConfig.minSdk = 23
}
