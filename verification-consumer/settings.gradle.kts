pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

val remoteOnly = providers.gradleProperty("remoteOnly").isPresent
val mmkvRepository = providers.gradleProperty("mmkvMavenRepo").orElse("https://jitpack.io").get()
if (remoteOnly) {
    require(gradle.startParameter.includedBuilds.isEmpty()) { "remoteOnly rejects composite builds" }
    require(mmkvRepository == "https://jitpack.io") { "remoteOnly requires public JitPack" }
    for (property in listOf("MMKV_LOCAL_REPOSITORY", "MMKV_USE_MAVEN_LOCAL", "verifyLocalSource")) {
        require(!providers.gradleProperty(property).isPresent) { "remoteOnly rejects $property" }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository { maven(mmkvRepository) }
            filter { includeGroup("com.github.gycrosskit.mmkv-ohos") }
        }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        google()
        mavenCentral()
    }
}

rootProject.name = "mmkv-ohos-consumer"
