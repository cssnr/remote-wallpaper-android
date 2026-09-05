import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// ACRA - Load credentials from secret.properties
val secretProperties = Properties().apply {
    val file = rootProject.file("secret.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
    //alias(libs.plugins.google.services)
    //alias(libs.plugins.firebase.crashlytics)
}

configure<ApplicationExtension> {
    namespace = "org.cssnr.remotewallpaper"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.cssnr.remotewallpaper"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ACRA - Acrarium backend setup: https://www.acra.ch/docs/Setup
        buildConfigField("String", "ACRA_URI", "\"${secretProperties.getProperty("acra.uri") ?: ""}\"")
        buildConfigField("String", "ACRA_USER", "\"${secretProperties.getProperty("acra.user") ?: ""}\"")
        buildConfigField("String", "ACRA_PASS", "\"${secretProperties.getProperty("acra.pass") ?: ""}\"")

        manifestPlaceholders["firebaseAnalyticsDeactivated"] = false // enabled
        manifestPlaceholders["firebaseCrashlyticsEnabled"] = true // enabled
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["firebaseAnalyticsDeactivated"] = true // disabled
            manifestPlaceholders["firebaseCrashlyticsEnabled"] = false // disabled
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    //implementation(libs.androidx.viewpager2)
    //implementation(platform(libs.firebase.bom))
    //implementation(libs.firebase.analytics)
    //implementation(libs.firebase.crashlytics)
    //implementation(libs.firebase.messaging)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.photoview)
    implementation(libs.konfetti.xml)
    implementation(libs.acra.http)
    implementation(libs.acra.toast)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
