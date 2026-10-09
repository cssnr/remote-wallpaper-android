// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    // NOTE: Not sure if this is required or not...
    //repositories {
    //    google()
    //    mavenCentral()
    //}
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    //alias(libs.plugins.kotlin.android) apply false
}
