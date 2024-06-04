import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    idea
}

buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:_")
    }
}

repositories {
    mavenCentral()
    google()
}

apply(plugin = "java")
apply(plugin = "kotlin")



sourceSets {
    main {
        kotlin {
            srcDirs += File("build/generated/ksp/main/kotlin")
        }
    }
    test {
        kotlin {
            srcDirs += File("_code")
            srcDirs += File("_posts")
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }
}

dependencies {

    api(platform(Http4k.bom))
    api(platform("dev.forkhandles:forkhandles-bom:_"))

    api(Http4k.core)
    api("dev.forkhandles:values4k")
    api("dev.forkhandles:result4k")
}
