import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("multiplatform") version "1.8.10"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.12.1"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        withJava()
    }

    fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.config(os: String) {
        binaries.executable()

        compilations.named("main") {
            cinterops {
                register("jni") {
                    val javaHome = File(System.getProperty("java.home"))
                    println(javaHome.resolve("include"))
                    includeDirs(
                        javaHome.resolve("include"),
                        javaHome.resolve("include/$os"),
                    )
                    defFile(project.file("src/nativeInterop/cinterop/jni.def"))
                }
            }
        }
    }

    when (HostManager.host) {
        KonanTarget.LINUX_X64 -> linuxX64("native") { config("linux") }
        KonanTarget.LINUX_ARM64 -> linuxArm64("native") { config("linux") }
        KonanTarget.MACOS_X64 -> macosX64("native") { config("darwing", ) }
        KonanTarget.MACOS_ARM64 -> macosArm64("native") { config("darwin") }
    }
}


tasks.register<Exec>("runJni") {
    dependsOn(tasks.assemble)
    val javaHome: Provider<String> = providers.environmentVariable("JAVA_HOME")
    val classPath: Provider<String> = tasks.run.map {
        it.classpath.joinToString(":")
    }

    environment("LD_LIBRARY_PATH", javaHome.map { "${"$"}LD_LIBRARY_PATH:$it/lib/server" }.get())
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
