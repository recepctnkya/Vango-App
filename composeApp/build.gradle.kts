import com.android.build.gradle.BaseExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.1.0" // Ensure this matches your Kotlin version
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "com.hexnet.vango.ComposeApp")
        }
    }
    
    sourceSets {
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.activity)
            implementation(libs.androidx.fragment)
            implementation("androidx.camera:camera-camera2:1.3.4")
            implementation("androidx.camera:camera-lifecycle:1.3.4")
            implementation("androidx.camera:camera-view:1.3.4")
            implementation("com.google.mlkit:barcode-scanning:17.3.0")
            implementation("com.google.android.gms:play-services-location:21.0.1")
            // Lifecycle dependencies for ProcessLifecycleOwner
            implementation("androidx.lifecycle:lifecycle-process:2.7.0")
            implementation("androidx.lifecycle:lifecycle-common:2.7.0")
            implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
            // OkHttp respects Android NetworkSecurityConfig (hostname-aware TLS).
            implementation("io.ktor:ktor-client-okhttp:2.3.7")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.7")
        }
        iosSimulatorArm64Main.dependencies {
            implementation(
                files("build/kmqtt-native/openssl-ios-simulator-arm64.klib")
            )
        }
        iosArm64Main.dependencies {
            implementation(
                files("build/kmqtt-native/openssl-ios-arm64.klib")
            )
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // Check for latest version
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0") // Check latest version
            implementation("io.ktor:ktor-client-core:2.3.7")
            implementation("io.github.davidepianca98:kmqtt-common:1.0.0")
            implementation("io.github.davidepianca98:kmqtt-client:1.0.0")
            implementation("com.juul.kable:core:0.31.1")
//            implementation("dev.icerock.moko:resources:0.23.0") // Check latest version
//            implementation("dev.icerock.moko:resources-compose:0.23.0") // For Compose integration
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.matching {
    it.name.startsWith("sync") && it.name.endsWith("ComposeResourcesForIos")
}.configureEach {
    doLast {
        val nativeArchives = outputs.files.asFileTree.matching {
            include("default/targets/**/included/*.a")
        }
        nativeArchives.forEach { archive ->
            check(archive.delete()) { "Could not remove native archive from iOS resources: $archive" }
            logger.lifecycle("Removed native archive from iOS resources: ${archive.name}")
        }
    }
}

android {
    namespace = "com.hextech.vango"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Keep existing app on phone; install next version as parallel package.
        applicationId = "com.hextech.vango.v090"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 91003
        versionName = "0.91-001"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.animation.core.android)
    debugImplementation(compose.uiTooling)
}

/**
 * After a successful installDebug, start MainActivity on an attached emulator (emulator-*)
 * or the sole connected device. Requires adb (Android SDK platform-tools).
 */
fun launchMainActivityOnDevice(logger: org.gradle.api.logging.Logger) {
    val sdkDir = project.extensions.getByType(BaseExtension::class.java).sdkDirectory
    val isWin = System.getProperty("os.name").lowercase().contains("windows")
    val adb = File(sdkDir, "platform-tools/${if (isWin) "adb.exe" else "adb"}")
    if (!adb.isFile) {
        logger.warn("launchDebug: adb not found at ${adb.absolutePath}")
        return
    }
    val listProc = ProcessBuilder(adb.absolutePath, "devices")
        .redirectErrorStream(true)
        .start()
    val devicesOut = listProc.inputStream.bufferedReader().readText()
    listProc.waitFor()
    val emulatorSerial = devicesOut.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("List of") }
        .mapNotNull { line ->
            val parts = line.split(Regex("\\s+"))
            if (parts.size >= 2 && parts[1] == "device" && parts[0].startsWith("emulator-")) parts[0] else null
        }
        .firstOrNull()
    val cmd = mutableListOf(adb.absolutePath)
    if (emulatorSerial != null) {
        cmd.add("-s")
        cmd.add(emulatorSerial)
        logger.lifecycle("Starting app on $emulatorSerial â€¦")
    } else {
        logger.lifecycle("Starting app on default device â€¦")
    }
    val appId = project.extensions.getByType(BaseExtension::class.java).defaultConfig.applicationId ?: "com.hextech.vango.v090"
    cmd.addAll(listOf("shell", "am", "start", "-n", "$appId/.MainActivity"))
    val launchProc = ProcessBuilder(cmd)
        .inheritIO()
        .start()
    val code = launchProc.waitFor()
    if (code != 0) {
        logger.warn("am start exited with code $code (emulator running and authorized?)")
    }
}

/** VERSION.json → versionName/versionCode / HexnetAppVersion.kt (makro-mikro Sync) */
tasks.register<Exec>("syncHexnetVersion") {
    onlyIf { System.getProperty("os.name").lowercase().contains("windows") }
    workingDir = rootProject.projectDir
    commandLine(
        "powershell",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        rootProject.file("scripts/Sync-VersionFiles.ps1").absolutePath
    )
}

tasks.named("preBuild").configure {
    dependsOn("syncHexnetVersion")
}

afterEvaluate {
    tasks.named("installDebug").configure {
        doLast {
            launchMainActivityOnDevice(logger)
        }
    }
}
