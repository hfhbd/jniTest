import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform") version "2.0.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.14.0"
    id("application")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)

    jvm {
        withJava()
    }

    fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.config(os: String) {
        val javaHome = File(System.getProperty("java.home"))

        binaries.executable { 
            linkerOpts("-L${javaHome.resolve("lib/server")}", "-ljvm")
        }

        compilations.named("main") {
            cinterops {
                register("jni") {
                    includeDirs(
                        javaHome.resolve("include"),
                        javaHome.resolve("include/$os"),
                    )
                }
            }
        }
    }

    when (HostManager.host) {
        KonanTarget.LINUX_X64 -> linuxX64 { config("linux") }
        KonanTarget.LINUX_ARM64 -> linuxArm64 { config("linux") }
        KonanTarget.MACOS_X64 -> macosX64 { config("darwing", ) }
        KonanTarget.MACOS_ARM64 -> macosArm64 { config("darwin") }
        else -> error("Not supported target ${HostManager.host}")
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val copyToNative by tasks.registering(Copy::class) {
    val target = when (HostManager.host) {
        KonanTarget.LINUX_X64 -> "linuxX64"
        KonanTarget.LINUX_ARM64 -> "linuxArm64"
        KonanTarget.MACOS_X64 -> "macosX64"
        KonanTarget.MACOS_ARM64 -> "macosArm64"
        else -> error("Not supported target ${HostManager.host}")
    }.replaceFirstChar { it.uppercaseChar() }
    from(tasks.named("linkDebugExecutable$target"))
    into("build/bin/native/debugExecutable")
}

tasks.register<Exec>("runJni") {
    dependsOn(tasks.assemble)
    dependsOn(copyToNative)
    val javaHome: Provider<String> = providers.environmentVariable("JAVA_HOME")
    val classPath: Provider<String> = tasks.run.map {
        it.classpath.joinToString(":")
    }

    environment("LD_LIBRARY_PATH", javaHome.map { "${"$"}LD_LIBRARY_PATH:$it/lib/server" }.get().also { 
        println(it)
    })
    commandLine("./build/bin/native/debugExecutable/jniTest.kexe", classPath.get(), "Hello", 42)
}

tasks.register("getClassPath") {
    val classPath = tasks.run.map {
        it.classpath.joinToString(":")
    }
    doFirst {
        println("Classpath: ${classPath.get()}")
        File("class.path").writeText(classPath.get())
    }
}
