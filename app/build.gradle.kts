plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.jarvis.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jarvis.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-milestone1"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:voice"))
    implementation(project(":core:llm"))
    implementation(project(":core:actions"))
    implementation(project(":platform:voice-android"))
    implementation(project(":platform:android-control"))
    implementation(project(":providers:local-llm"))
    implementation(project(":providers:remote-llm"))
    implementation(project(":data:settings"))
    implementation(project(":data:security"))
    implementation(project(":data:logging"))

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
}
