// Top‑level Gradle (ROOT project)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.kotlin.kapt)          apply false  // sekarang alias ada
    alias(libs.plugins.google.services)      apply false
}
