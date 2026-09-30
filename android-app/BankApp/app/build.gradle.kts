plugins {
    alias(libs.plugins.android.application)  // com.android.application
    alias(libs.plugins.kotlin.android)       // org.jetbrains.kotlin.android
    alias(libs.plugins.kotlin.compose)       // org.jetbrains.kotlin.plugin.compose
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val apiBaseUrl = providers.gradleProperty("API_BASE_URL")
    .orElse("http://10.0.2.2:8080/")
    .get()
val supportEmail = providers.gradleProperty("SUPPORT_EMAIL")
    .orElse("support@example.test")
    .get()
val enableLogging = providers.gradleProperty("ENABLE_LOGGING")
    .orElse("false")
    .get()

android {
    namespace = "com.bankingsystem.mobile"
    compileSdk = 36
    compileSdkExtension = 20
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.bankingsystem.mobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", apiBaseUrl.asBuildConfigString())
        buildConfigField("String", "SUPPORT_EMAIL", supportEmail.asBuildConfigString())
        buildConfigField("boolean", "ENABLE_LOGGING", enableLogging)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
dependencies {
    // Android core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.core.splashscreen)

    // Compose UI dependencies
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.browser)
    implementation(libs.play.services.mlkit.barcode.scanning)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Material design dependencies
    implementation(libs.material)
    implementation(libs.material.icons.extended)

    // Network and data storage dependencies
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Navigation dependencies
    implementation(libs.androidx.navigation.compose)

    // Image loading dependencies
    implementation(libs.coil.compose)

    // Face detection dependencies
    implementation(libs.face.detection)

    // --- Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.converter.moshi)
    implementation(libs.moshi.kotlin)

}

kapt { correctErrorTypes = true }
