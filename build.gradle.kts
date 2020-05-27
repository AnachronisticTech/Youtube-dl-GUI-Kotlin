plugins {
    kotlin("multiplatform") version "1.3.71"
    kotlin("plugin.serialization") version "1.3.71"
}

repositories {
    jcenter()
}

val os = org.gradle.internal.os.OperatingSystem.current()!!

kotlin {
    when {
        os.isWindows -> mingwX86("libui")
        os.isMacOsX -> macosX64("libui")
        os.isLinux -> linuxX64("libui")
        else -> throw Error("Unknown host")
    }.binaries.executable {
        if (os.isWindows) {
            windowsResources("hello.rc")
            linkerOpts("-mwindows")
        }
    }
    sourceSets {
        val libuiMain by getting {
            dependencies {
                implementation("com.github.msink:libui:0.1.7")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:0.20.0")
            }
            kotlin.srcDir("src/libuiMain/Common")
            when {
                os.isWindows -> kotlin.srcDir("src/libuiMain/Windows")
                os.isUnix -> kotlin.srcDir("src/libuiMain/Unix")
                else -> throw Error("Unknown host")
            }
        }
    }
}

fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val taskName = linkTaskName.replaceFirst("link", "windres")
    val inFile = compilation.defaultSourceSet.resources.sourceDirectories.singleFile.resolve(rcFileName)
    val outFile = buildDir.resolve("processedResources/$taskName.res")

    val windresTask = tasks.create<Exec>(taskName) {
        val konanUserDir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}/.konan"
        val konanLlvmDir = "$konanUserDir/dependencies/msys2-mingw-w64-i686-clang-llvm-lld-compiler_rt-8.0.1/bin"

        inputs.file(inFile)
        outputs.file(outFile)
        commandLine("$konanLlvmDir/windres", inFile, "-D_${buildType.name}", "-O", "coff", "-o", outFile) //, "-Xmulti-platform"
        environment("PATH", "$konanLlvmDir;${System.getenv("PATH")}")

        dependsOn(compilation.compileKotlinTask)
    }

    linkTask.dependsOn(windresTask)
    linkerOpts(outFile.toString())
}

tasks.register("buildMacApp") {
    doLast {
        val appName = "YDLGUI"
        val appPath = "$buildDir/bin/libui/releaseExecutable/$appName.app"
        mkdir("$appPath")
        mkdir("$appPath/Contents")
        mkdir("$appPath/Contents/Resources")
        mkdir("$appPath/Contents/MacOS")
        copy {
            from("src/libuiMain/resources/Info.plist")
            into("$appPath/Contents/")
        }
        copy {
            from("$buildDir/bin/libui/releaseExecutable/${rootProject.name}.kexe")
            into("$appPath/Contents/MacOS/")
            rename("(.*)", "$appName.kexe")
        }
    }
}
