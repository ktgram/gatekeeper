import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val jvmTargetVersion = JavaVersion.VERSION_17

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.telegram.bot)
}

group = "com.example.blank"
version = "0.0.1"
application {
    mainClass.set("com.example.blank.TgBotApplication")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.redisson)
    implementation(libs.jackson.kotlin)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jvmTargetVersion.majorVersion)
    }
    jvmToolchain(jvmTargetVersion.majorVersion.toInt())
}
