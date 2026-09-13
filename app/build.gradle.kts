import org.gradle.internal.os.OperatingSystem
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val abis = (findProperty("eloqium.abis") as String? ?: "arm64-v8a")
    .split(",").map { it.trim() }.filter { it.isNotEmpty() }
val rulesForm = findProperty("eloqium.rules") as String? ?: "bytecode"
val languages = findProperty("eloqium.langs") as String? ?: "lang/enus lang/engb lang/eses lang/esus lang/frfr lang/frca lang/dede lang/itit"
val nativeOut = layout.buildDirectory.dir("native/jniLibs")

val buildNative by tasks.registering(Exec::class) {
    group = "build"
    description = "Compiles OpenEVV engine and JNI bridge for configured ABIs."
    val engine = rootProject.layout.projectDirectory.dir("native/openevv")
    val script = rootProject.layout.projectDirectory.file("native/build-native.sh")
    
    inputs.dir(rootProject.layout.projectDirectory.dir("native/patches"))
    inputs.file(rootProject.layout.projectDirectory.file("native/android.mk"))
    inputs.file(script)
    inputs.file(layout.projectDirectory.file("src/main/cpp/eloqium_jni.c"))
    inputs.property("abis", abis)
    inputs.property("rules", rulesForm)
    inputs.property("langs", languages)
    outputs.dir(nativeOut)
    
    val shell = if (OperatingSystem.current().isWindows) {
        val git = System.getenv("PROGRAMFILES")?.let { file("$it/Git/bin/bash.exe") }
        if (git != null && git.exists()) git.absolutePath else "bash"
    } else {
        "bash"
    }
    commandLine(shell, script.asFile.absolutePath)
    environment("ABIS", abis.joinToString(" "))
    environment("RULES", rulesForm)
    environment("LANGS", languages)
    environment("OUT", nativeOut.get().asFile.absolutePath)
    
    doFirst {
        if (!engine.file("Makefile").asFile.exists()) {
            throw GradleException("native/openevv directory is missing Makefile.")
        }
    }
}

android {
    namespace = "org.eloqium.tts"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.eloqium.tts"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += abis
        }
    }

    sourceSets.getByName("main") {
        kotlin.srcDirs("src/main/kotlin")
        jniLibs.srcDirs(nativeOut)
    }

    signingConfigs {
        create("release") {
            val localProps = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localPropsFile.inputStream().use { localProps.load(it) }
            }

            fun prop(key: String): String? {
                val p = localProps.getProperty(key)
                if (!p.isNullOrEmpty()) return p
                val gradleProp = findProperty(key) as? String
                if (!gradleProp.isNullOrEmpty()) return gradleProp
                return null
            }

            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
                ?: prop("release.keystore")
                ?: "/data/data/com.termux/files/home/.release_key/release_key.jks"

            val keystoreFile = rootProject.file(keystorePath)
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                val storePass = System.getenv("RELEASE_STORE_PASSWORD") ?: prop("release.storePassword")
                val alias = System.getenv("RELEASE_KEY_ALIAS") ?: prop("release.keyAlias") ?: "eloqium"
                val keyPass = System.getenv("RELEASE_KEY_PASSWORD") ?: prop("release.keyPassword") ?: storePass

                if (!storePass.isNullOrEmpty()) {
                    storePassword = storePass
                    keyAlias = alias
                    keyPassword = keyPass
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile?.exists() == true && !releaseConfig.storePassword.isNullOrEmpty()) {
                signingConfig = releaseConfig
            }
        }
        debug {
            isJniDebuggable = true
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.named("preBuild") {
    dependsOn(buildNative)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
}
