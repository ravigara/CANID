plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.dogregistration"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dogregistration"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // packaging block (commented — uncomment if you need it)
    /*
    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE")
        }
    }
    */
}

dependencies {
    // Compose BOM + UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons)

    // Lifecycle & Coroutines
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.playservices)
    implementation(libs.kotlinx.coroutines.android)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML & TFLite
    implementation(libs.tensorflow.lite.select.tf.ops)
    implementation(libs.google.mlkit.obj.detection)
    implementation(libs.tensorflow.lite.support)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Utilities
    implementation(libs.google.accompanist.permissions)
    implementation(libs.coil.compose)
    implementation(libs.google.android.material)
    implementation(libs.google.gson)

    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

// TensorFlow Lite runtime + support
    implementation("org.tensorflow:tensorflow-lite:2.15.0")                // core runtime
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")        // tflite support helpers
// Optional: if you converted with SELECT_TF_OPS, add this too:
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.15.0")

}
