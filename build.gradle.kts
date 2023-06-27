import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform") version "1.8.22"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
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
        KonanTarget.LINUX_X64 -> linuxX64("native") { config("linux") }
        KonanTarget.LINUX_ARM64 -> linuxArm64("native") { config("linux") }
        KonanTarget.MACOS_X64 -> macosX64("native") { config("darwing", ) }
        KonanTarget.MACOS_ARM64 -> macosArm64("native") { config("darwin") }
        else -> error("Not supported target ${HostManager.host}")
    }
}


tasks.register<Exec>("runJni") {
    dependsOn(tasks.assemble)
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
