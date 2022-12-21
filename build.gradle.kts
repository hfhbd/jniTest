plugins {
    kotlin("multiplatform") version "1.8.0-RC2"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.12.1"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.config(os: String) {
        binaries.executable()

        compilations.named("main") {
            cinterops {
                register("jni") {
                    val javaHome = File(System.getProperty("java.home"))
                    includeDirs(
                        javaHome.resolve("include"),
                        javaHome.resolve("include/$os"),
                    )
                    defFile(project.file("src/nativeInterop/cinterop/jni.def"))
                }
            }
        }
    }

    linuxX64 {
         config("linux")
    }
    macosArm64 { config("darwin") }
    macosX64 { config("darwin") }
    
    sourceSets {
        val nativeMain by creating
        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }
        val macosX64Main by getting {
            dependsOn(nativeMain)
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
    }
}
