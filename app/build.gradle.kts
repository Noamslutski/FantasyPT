plugins {
    alias(libs.plugins.android.application)
}

// google-services.json is registered per-app in the Firebase console and downloaded manually —
// apply the plugin only once it's actually present so the project builds cleanly before that
// manual step happens. Once the file is dropped into app/, this line makes Firebase active with
// no further build-file changes needed.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.noamsl.fantasypt"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.noamsl.fantasypt"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        resValues = true
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    // Firebase — Auth (anonymous sign-in) + Realtime Database only, same as RacingManager's setup.
    implementation(platform("com.google.firebase:firebase-bom:34.14.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
