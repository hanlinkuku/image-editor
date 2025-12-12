plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://mirrors.huaweicloud.com/repository/maven/")
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
}