/*
 * Tencent is pleased to support the open source community by making
 * MMKV available.
 *
 * Copyright (C) 2026 THL A29 Limited, a Tencent company.
 * All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *       https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Jar

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.tencent.mmkv.kmp"
    compileSdk = 35
    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

val mmkvVersion = (findProperty("MMKV_VERSION") as? String) ?: "2.4.2"
val publishVersion = (findProperty("VERSION_NAME") as? String) ?: mmkvVersion
val baseArtifactId = (findProperty("POM_ARTIFACT_ID") as? String) ?: "mmkv-kmp"
val publishedGroup = (findProperty("GROUP") as? String) ?: "com.tencent"
val isSnapshot = publishVersion.endsWith("-SNAPSHOT")
val sonatypeUsername =
    (
        findProperty("SONATYPE_NEXUS_USERNAME")
            ?: findProperty("mavenCentralUsername")
            ?: findProperty("REPOSITORY_USERNAME")
        ) as String?
val sonatypePassword =
    (
        findProperty("SONATYPE_NEXUS_PASSWORD")
            ?: findProperty("mavenCentralPassword")
            ?: findProperty("REPOSITORY_PASSWORD")
        ) as String?
val signingKey =
    (findProperty("SIGNING_KEY") ?: findProperty("signingInMemoryKey")) as String?
val signingPassword =
    (findProperty("SIGNING_PASSWORD") ?: findProperty("signingInMemoryKeyPassword")) as String?
val hasFileBasedSigning =
    !findProperty("signing.keyId")?.toString().isNullOrBlank() &&
        !findProperty("signing.secretKeyRingFile")?.toString().isNullOrBlank()
val mmkvGitRepository = findProperty("MMKV_GIT_REPOSITORY") as? String
val mmkvGitTag = findProperty("MMKV_GIT_TAG") as? String
val mmkvGitBranch = findProperty("MMKV_GIT_BRANCH") as? String
val mmkvGitCommit = findProperty("MMKV_GIT_COMMIT") as? String
val mmkvForceFetch = (findProperty("MMKV_FORCE_FETCH") as? String)?.toBooleanStrictOrNull() == true
val mmkvGitRef = mmkvGitCommit?.takeIf { it.isNotBlank() }
    ?: mmkvGitBranch?.takeIf { it.isNotBlank() }
    ?: mmkvGitTag?.takeIf { it.isNotBlank() }

// Keep this project's internal coordinates distinct from the published Android
// dependency (`com.tencent:mmkv`) so same-build resolution never substitutes the
// native Android AAR with this KMP wrapper project.
group = "$publishedGroup.kmpbuild"
version = publishVersion

val nativeInteropDir = project.file("nativeInterop")
val nativeBuildRoot = nativeInteropDir.resolve("build")

fun taskSuffix(label: String): String =
    label.split('-', '_')
        .filter { it.isNotBlank() }
        .joinToString("") { token -> token.replaceFirstChar { it.uppercase() } }

fun cmakeBuildDirFor(label: String) = nativeBuildRoot.resolve(label)

fun registerCMakeBuildTask(
    label: String,
    cmakeTarget: String = "mmkv-kmp",
    extraConfigureArgs: List<String> = emptyList(),
): TaskProvider<Exec> {
    val buildDir = cmakeBuildDirFor(label)
    val configureTask = tasks.register<Exec>("cmakeConfigure${taskSuffix(label)}") {
        group = "build"
        description = "Configure CMake for $label"

        inputs.file(nativeInteropDir.resolve("CMakeLists.txt"))
        inputs.file(nativeInteropDir.resolve("cinterop/mmkv.def"))
        outputs.file(buildDir.resolve("CMakeCache.txt"))
        outputs.file(buildDir.resolve("include/MMKVBridge.h"))

        doFirst { buildDir.mkdirs() }

        workingDir = nativeInteropDir
        commandLine(
            buildList {
                add((findProperty("MMKV_CMAKE") as? String) ?: "cmake")
                add("-S")
                add(".")
                add("-B")
                add(buildDir.absolutePath)
                add("-DCMAKE_BUILD_TYPE=Release")
                add("-DMMKV_VERSION=v$mmkvVersion")
                if (!mmkvGitRepository.isNullOrBlank()) {
                    add("-DMMKV_GIT_REPOSITORY=$mmkvGitRepository")
                }
                if (!mmkvGitRef.isNullOrBlank()) {
                    add("-DMMKV_GIT_TAG=$mmkvGitRef")
                }
                if (mmkvForceFetch) {
                    add("-DMMKV_FORCE_FETCH=ON")
                }
                addAll(extraConfigureArgs)
            }
        )
    }

    return tasks.register<Exec>("cmakeBuild${taskSuffix(label)}") {
        group = "build"
        description = "Build native MMKV artifact for $label via CMake"
        dependsOn(configureTask)

        inputs.file(nativeInteropDir.resolve("CMakeLists.txt"))
        inputs.file(nativeInteropDir.resolve("cinterop/mmkv.def"))
        outputs.file(buildDir.resolve("libmmkv-kmp.a"))
        // Let CMake perform its own incremental source check. Gradle cannot
        // reliably model FetchContent/local Core source inputs here.
        outputs.upToDateWhen { false }

        workingDir = nativeInteropDir
        commandLine(
            buildList {
                add((findProperty("MMKV_CMAKE") as? String) ?: "cmake")
                add("--build")
                add(buildDir.absolutePath)
                add("--config")
                add("Release")
                add("--target")
                add(cmakeTarget)
            }
        )
    }
}

fun publicationArtifactId(publicationName: String): String = when (publicationName) {
    "kotlinMultiplatform" -> baseArtifactId
    "android", "androidRelease" -> "$baseArtifactId-android"
    else -> "$baseArtifactId-${publicationName.lowercase()}"
}

fun pomName(publicationName: String): String = when (publicationName) {
    "kotlinMultiplatform" -> "MMKV Kotlin Multiplatform"
    else -> "MMKV Kotlin Multiplatform ($publicationName)"
}

fun MavenPublication.configurePom(publicationName: String) {
    pom {
        name.set(pomName(publicationName))
        description.set((findProperty("POM_DESCRIPTION") as? String) ?: "Experimental Kotlin Multiplatform wrapper for MMKV")
        url.set((findProperty("POM_URL") as? String) ?: "https://github.com/Tencent/MMKV")
        licenses {
            license {
                name.set((findProperty("POM_LICENCE_NAME") as? String) ?: "BSD 3-Clause License")
                url.set((findProperty("POM_LICENCE_URL") as? String) ?: "https://opensource.org/licenses/BSD-3-Clause")
            }
        }
        developers {
            developer {
                id.set((findProperty("POM_DEVELOPER_ID") as? String) ?: "tencent")
                name.set((findProperty("POM_DEVELOPER_NAME") as? String) ?: "Tencent")
            }
        }
        scm {
            url.set((findProperty("POM_SCM_URL") as? String) ?: "https://github.com/Tencent/MMKV")
            connection.set((findProperty("POM_SCM_CONNECTION") as? String) ?: "scm:git:git://github.com/Tencent/MMKV.git")
            developerConnection.set((findProperty("POM_SCM_DEV_CONNECTION") as? String) ?: "scm:git:ssh://git@github.com/Tencent/MMKV.git")
        }
    }
}

val javadocJars = mutableMapOf<String, TaskProvider<Jar>>()

fun javadocJarFor(publicationName: String): TaskProvider<Jar> =
    javadocJars.getOrPut(publicationName) {
        tasks.register<Jar>("${taskSuffix(publicationName)}JavadocJar") {
            // Each publication needs a distinct physical file so Gradle's
            // signing tasks don't race on a shared *.asc output.
            archiveBaseName.set("$baseArtifactId-$publicationName")
            archiveClassifier.set("javadoc")
            from(rootProject.file("README.md"))
        }
    }

val verifySonatypePublication = tasks.register("verifySonatypePublication") {
    group = "publishing"
    description = "Fail early when Maven Central credentials or release signing are missing."
    doLast {
        check(!sonatypeUsername.isNullOrBlank()) {
            "Missing SONATYPE_NEXUS_USERNAME (or mavenCentralUsername)."
        }
        check(!sonatypePassword.isNullOrBlank()) {
            "Missing SONATYPE_NEXUS_PASSWORD (or mavenCentralPassword)."
        }
        if (!isSnapshot) {
            check(!signingKey.isNullOrBlank() || hasFileBasedSigning) {
                "Missing an in-memory signing key or Gradle signing.keyId/signing.secretKeyRingFile."
            }
        }
    }
}

kotlin {
    withSourcesJar()

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    // The OHOS target uses the same C bridge as Darwin, built with the OHOS NDK.
    val darwinTargets = listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    )

    applyDefaultHierarchyTemplate {
        common {
            // Keep Darwin source in one shared source set while publishing only iOS targets.
            group("darwin") {
                group("ios")
            }
        }
    }

    val darwinBuildSettings = mapOf(
        "iosArm64" to listOf("iOS", "iphoneos", "arm64", "13.0"),
        "iosSimulatorArm64" to listOf("iOS", "iphonesimulator", "arm64", "13.0"),
        "iosX64" to listOf("iOS", "iphonesimulator", "x86_64", "13.0"),
    )
    darwinTargets.forEach { target ->
        val label = target.targetName
        val settings = darwinBuildSettings.getValue(label)
        val cmakeTask = registerCMakeBuildTask(
            label = label,
            extraConfigureArgs = listOf(
                "-DCMAKE_SYSTEM_NAME=${settings[0]}",
                "-DCMAKE_OSX_SYSROOT=${settings[1]}",
                "-DCMAKE_OSX_ARCHITECTURES=${settings[2]}",
                "-DCMAKE_OSX_DEPLOYMENT_TARGET=${settings[3]}",
                "-DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY",
            ),
        )
        val buildDir = cmakeBuildDirFor(label)
        val generatedIncludeDir = buildDir.resolve("include")

        target.compilations.getByName("main") {
            cinterops {
                val mmkv by creating {
                    defFile("nativeInterop/cinterop/mmkv.def")
                    compilerOpts("-I${generatedIncludeDir.absolutePath}")
                    includeDirs(generatedIncludeDir)
                    extraOpts("-libraryPath", buildDir.absolutePath)
                }
            }
        }

        tasks.matching { task ->
            task.name.contains(target.targetName, ignoreCase = true) &&
                task.name.startsWith("cinterop")
        }.configureEach {
            dependsOn(cmakeTask)
        }
    }

    val ohosTarget = ohosArm64()
    val ohosNativeSdk = (findProperty("OHOS_NATIVE_SDK") as? String)
        ?: System.getenv("OHOS_SDK_NATIVE")
        ?: System.getenv("DEVECO_SDK_HOME")?.let { "$it/openharmony/native" }
    val ohosBuildDir = cmakeBuildDirFor("ohosArm64")
    val ohosBuild = registerCMakeBuildTask(
        label = "ohosArm64",
        extraConfigureArgs = listOfNotNull(
            ohosNativeSdk?.let { "-DCMAKE_TOOLCHAIN_FILE=$it/build/cmake/ohos.toolchain.cmake" },
            "-DOHOS_ARCH=arm64-v8a",
            "-DOHOS_STL=c++_static",
        ),
    )
    tasks.named("cmakeConfigureOhosArm64").configure {
        doFirst {
            check(ohosNativeSdk != null && file("$ohosNativeSdk/build/cmake/ohos.toolchain.cmake").isFile) {
                "Set OHOS_NATIVE_SDK to the OpenHarmony native SDK directory"
            }
        }
    }
    ohosTarget.compilations.getByName("main") {
        cinterops {
            val mmkv by creating {
                defFile("nativeInterop/cinterop/mmkv.def")
                compilerOpts("-I${ohosBuildDir.resolve("include").absolutePath}")
                includeDirs(ohosBuildDir.resolve("include"))
                extraOpts("-libraryPath", ohosBuildDir.absolutePath)
            }
        }
    }
    tasks.matching { it.name.startsWith("cinterop") && it.name.contains("OhosArm64") }
        .configureEach { dependsOn(ohosBuild) }

    sourceSets {
        val nativeMain = maybeCreate("nativeMain")
        getByName("darwinMain").dependsOn(nativeMain)
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        androidMain {
            dependencies {
                implementation("com.tencent:mmkv:$mmkvVersion")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.test:runner:1.7.0")
            }
        }
    }
}


publishing {
    publications.withType<MavenPublication>().configureEach {
        groupId = publishedGroup
        artifactId = publicationArtifactId(name)
        artifact(javadocJarFor(name))
        configurePom(name)
    }

    repositories {
        maven {
            name = "localTest"
            url = (findProperty("MMKV_LOCAL_REPOSITORY") as? String)
                ?.takeIf { it.isNotBlank() }
                ?.let(::uri)
                ?: uri(layout.buildDirectory.dir("local-maven"))
        }

        val releaseRepo = findProperty("RELEASE_REPOSITORY_URL") as? String
        val snapshotRepo = findProperty("SNAPSHOT_REPOSITORY_URL") as? String
        val repoUrl = if (isSnapshot) snapshotRepo else releaseRepo
        if (!repoUrl.isNullOrBlank()) {
            maven {
                name = "sonatype"
                url = uri(repoUrl)
                credentials {
                    username = sonatypeUsername
                    password = sonatypePassword
                }
            }
        }
    }
}

tasks.matching {
    it.name.startsWith("publish") && it.name.endsWith("ToSonatypeRepository")
}.configureEach {
    dependsOn(verifySonatypePublication)
}

signing {
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    if (!signingKey.isNullOrBlank() || hasFileBasedSigning) {
        sign(publishing.publications)
    }
}

apply(from = rootProject.file("gradle/central-portal.gradle.kts"))
