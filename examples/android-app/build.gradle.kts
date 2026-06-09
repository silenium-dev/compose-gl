plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

group = "dev.silenium.compose.gl.examples"

dependencies {
    implementation(project(":lib"))
    implementation(libs.slf4j.android)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.foundation:foundation:1.11.2")
    implementation("androidx.compose.runtime:runtime:1.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")
}

android {
    namespace = "dev.silenium.compose.gl.examples"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 26
        targetSdk = 37
    }
    packaging.resources.pickFirsts += "META-INF/*"
}
