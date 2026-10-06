import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

// пустой секрет GitHub приходит пустой строкой, считаем его отсутствующим
fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

val signingProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "app.farvater"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.farvater"
        minSdk = 26
        targetSdk = 35
        // версию задаёт тег релиза: v1.2.3 даёт versionName 1.2.3 и versionCode 10203
        val tagVersion = (findProperty("farvaterVersion") as String?)?.removePrefix("v")
        versionName = tagVersion ?: "1.0.0"
        versionCode = tagVersion?.split('.')?.map { it.toInt() }?.let { (a, b, c) -> a * 10000 + b * 100 + c } ?: 1
        // публичный репозиторий с релизами, откуда приложение берёт обновления
        buildConfigField("String", "UPDATE_REPO", "\"${findProperty("farvaterUpdateRepo") ?: "lutzashl290788-cell/farvater"}\"")
    }

    signingConfigs {
        create("release") {
            val keystore = env("FARVATER_KEYSTORE") ?: signingProps.getProperty("storeFile")
            if (keystore != null) {
                storeFile = file(keystore)
                val password = env("FARVATER_KEYSTORE_PASSWORD") ?: signingProps.getProperty("storePassword")
                storePassword = password
                // псевдоним и пароль ключа необязательны: по умолчанию farvater и пароль хранилища
                keyAlias = env("FARVATER_KEY_ALIAS") ?: signingProps.getProperty("keyAlias") ?: "farvater"
                keyPassword = env("FARVATER_KEY_PASSWORD") ?: signingProps.getProperty("keyPassword") ?: password
            }
            // все три схемы подписи: установщики некоторых прошивок отклоняют APK только с v2
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // без ключа релиз остаётся неподписанным
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let { signingConfig = it }
        }
    }

    // отдельный APK под каждую архитектуру и универсальный
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        getByName("main") { assets.srcDir("../legal") }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // ресурсы нужны для скриншотов экранов
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        // служебный файл одной из библиотек, в приложении он не нужен
        resources.excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    // ядро Xray, libv2ray.aar кладёт scripts/fetch-natives.sh
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime)
    debugImplementation(libs.compose.ui.tooling)

    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}
