rootProject.name = "MMKV-KMP"

pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
val useMavenLocal = providers.gradleProperty("MMKV_USE_MAVEN_LOCAL").orNull == "true"
val localRepository = providers.gradleProperty("MMKV_LOCAL_REPOSITORY").orNull

dependencyResolutionManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        if (!localRepository.isNullOrBlank()) {
            maven(localRepository)
        }
        if (useMavenLocal) {
            mavenLocal()
        }
        google()
        mavenCentral()
    }
}

include(":mmkv")
if (providers.gradleProperty("MMKV_INCLUDE_SAMPLE").orNull == "true") {
    include(":sample:composeApp")
}
