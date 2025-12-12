pluginManagement {
    repositories {
        maven("https://mirrors.huaweicloud.com/repository/maven/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://mirrors.huaweicloud.com/repository/maven/")
        maven("https://jitpack.io")
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")